/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntProcedure;

import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import chord.util.Execution;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.TrioIterable;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomE;
import chord.doms.DomV;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IndexMap;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ChordRuntimeException;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

class CtxtVar {
  public CtxtVar(Register v, Ctxt e) { this.v = v; this.e = e; }
  public final Register v;
  public final Ctxt e;
  public int length() { return 1 + e.length(); }
  @Override public boolean equals(Object _that) {
    CtxtVar that = (CtxtVar)_that;
    return v.equals(that.v) && e.equals(that.e);
  }
  @Override public int hashCode() {
    return v.hashCode() * 37 + e.hashCode();
  }
}

/**
 * Performs a refinement-based pointer analysis.
 *
 * A context c [Ctxt] is a chain of allocation/call sites.
 *
 * An abstraction is specified by:
 *  - Sa: set of abstract objects [Ctxt] (allocation site + context)
 *  - Sv: set of contextual variables (variable + context)
 *
 * A sliver is a contextual variable or abstract object.
 *
 * Example:
 *
 * CI1 --[IM]--+--> CM1 --+
 *             |          |
 * CI2 --[IM]--+          +--[MH]--> CH1 --[AFA]--> CH2 <--[VEA]-- V1
 *
 * (INIT) Initialize abstraction to 0-CFA.
 * Iterate:
 *   (COMPUTE_C) Compute C and related relations from current abstraction.
 *   (ANALYSIS) Call Datalog analysis to obtain VEA,AFA.
 *   (PRUNE) Follow VEA pointers forward once and AFA pointers backwards to find relevant abstract objects (abs).
 *   Choose a subset of these slivers which are within a fixed number of hops from something that a query variable points to;
 *   these are the ones we want to refine (hotAbs).
 *   (REFINE) Given the chosen influential slivers, follow IM or HtoM backwards, splitting slivers.
 * (FULL) Set the heap abstraction to be the influential slivers and run the
 * flow-sensitive, context-sensitive full program analysis.
 *
 * For thread-escape, input variables are those whose field is accessed in
 * client code (get these by looking at EV).
 *
 * Example: suppose the input is V1.
 *   1) Abstract object that V1 can reach is CH2; influential slivers: {CH1, CH2}.
 *   2) split CH1 into CH1,I1 and CH1,I2.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(
  name = "sliver-ctxts-java",
  produces = { "C", "AH", "CfromMIC", "CfromMA", "AfromHC", "EfromVC", "objI", "relevantQueryE" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis {
  private Execution X;

  static final String initTaskName = "cspa-init-sliver-dlog";
  static final String analysisTaskName = "cspa-sliver-dlog";

  // Immutable inputs
  private DomV domV;
  private DomM domM;
  private DomI domI;
  private DomH domH;
  private DomE domE;
  private ProgramRel relEV;

  // Datalog -> Java
  private ProgramRel relVEA;
  private ProgramRel relAFA;
  private ProgramRel relFA;

  // Java -> Datalog
  private DomC domC;
  private ProgramRel relAH;
  private ProgramRel relCfromMIC; // call-site-based
  private ProgramRel relCfromMA; // object-based
  private ProgramRel relAfromHC;
  private ProgramRel relEfromVC;
  private ProgramRel relobjI;
  private ProgramRel relRelevantQueryE;

  // Canonical
  Quad _H;
  Register _V;

  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);

  // Options
  int verbose;
  int maxIters;
  //int maxHintSize;
  boolean refineA;
  boolean refineV;
  boolean useDatalogRelevance;
  int initRefinementRadiusA;
  int initRefinementRadiusV;
  boolean useObjectSensitivity;
  boolean departWhenRefine;

  IndexMap<Ctxt> globalC = new IndexMap<Ctxt>(); // dom C will change over time, but keep a consistent indexing for comparing across iterations

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  Set<Register> vSet = new HashSet<Register>();
  Set<jq_Method> mSet = new HashSet<jq_Method>();
  HashMap<Quad,List<Quad>> i_this_h = new HashMap<Quad,List<Quad>>(); // i -> this can point to something allocated at h
  HashMap<Quad,List<jq_Method>> im = new HashMap<Quad,List<jq_Method>>();
  HashMap<jq_Method,List<Quad>> rev_h2m = new HashMap<jq_Method,List<Quad>>();
  HashMap<jq_Method,List<Quad>> rev_im = new HashMap<jq_Method,List<Quad>>();

  // Current abstraction
  Abstraction abs = new Abstraction();
  int refinementRadiusA; // Maximum number of hops that any candidate is away from a pivot (for abstract objects)
  int refinementRadiusV; // Maximum number of hops that any candidate is away from a pivot (for contextual variables)

  class Result {
    int numProven;
    int absHashCode;
    String absSummary;
  }
  List<Result> results = new ArrayList<Result>();

  // Determine the queries in client code (for thread-escape)
  public void computedExcludedClasses() {
    String[] checkExcludedPrefixes = Config.toArray(Config.checkExcludeStr);
    Program program = Program.g();
    for (jq_Reference r : program.getClasses()) {
      String rName = r.getName();
      for (String prefix : checkExcludedPrefixes) {
        if (rName.startsWith(prefix))
          excludedClasses.add(r);
      }
    }
  }
  private boolean computeStatementIsExcluded(Quad e) {
    jq_Class c = e.getMethod().getDeclaringClass();
    return excludedClasses.contains(c);
  }
  Set<jq_Reference> excludedClasses = new HashSet<jq_Reference>();
  List<Query> queries = new ArrayList<Query>();;

  // Helper methods
  private <T> ArrayList<T> sel0(T x, ProgramRel rel, Object o0) {
    RelView view = rel.getView();
    view.selectAndDelete(0, o0);
    ArrayList<T> list = new ArrayList();
    for(Object o1 : view.getAry1ValTuples())
      list.add((T)o1);
    view.free();
    return list;
  }
  private <T> ArrayList<T> sel1(T x, ProgramRel rel, Object o1) {
    RelView view = rel.getView();
    view.selectAndDelete(1, o1);
    ArrayList<T> list = new ArrayList();
    for(Object o0 : view.getAry1ValTuples())
      list.add((T)o0);
    view.free();
    return list;
  }

  private jq_Type h2t(Quad h) {
    Operator op = h.getOperator();
    if (op instanceof New) 
      return New.getType(h).getType();
    else if (op instanceof NewArray)
      return NewArray.getType(h).getType();
    else {
      assert (op instanceof MultiNewArray);
      return MultiNewArray.getType(h).getType();
    }
  }

  Register e2v(Quad e) {
    for (Register v : sel0(_V, relEV, e)) return v;
    return null;
  }
  String hstrBrief(Quad h) {
    String path = new File(h.toJavaLocStr()).getName();
    return path+"("+h2t(h).shortName()+")";
  }
  String istrBrief(Quad i) {
    String path = new File(i.toJavaLocStr()).getName();
    Operator op = i.getOperator();
    jq_Method m = InvokeStatic.getMethod(i).getMethod();
    return path+"("+m.getName()+")";
  }

  String qstr(Quad q) { return q.toJavaLocStr()+"("+q.toString()+")"; }
  String hstr(Quad h) { return String.format("%s[%s]", domH.indexOf(h), qstr(h)); }
  String istr(Quad i) { return String.format("%s[%s]", domI.indexOf(i), qstr(i)); }
  String estr(Quad e) { return String.format("%s[%s]", domE.indexOf(e), e.toVerboseStr()); }
  String vstr(Register v) { return String.format("%s[%s]", domV.indexOf(v), v); }
  String cstr(Ctxt c) { return cstr(c, true); }
  String cstr(Ctxt c, boolean showIndex) {
    StringBuilder buf = new StringBuilder();
    if (showIndex) buf.append(domC.indexOf(c));
    buf.append('{');
    for (int i = 0; i < c.length(); i++) {
      if (i > 0) buf.append(" | ");
      Quad q = c.get(i);
      buf.append(isAlloc(q) ? hstr(q) : istr(q));
    }
    buf.append('}');
    return buf.toString();
  }
  String cstrVerbose(Ctxt c) { return cstr(c, false)+(c.length() > 0 ? "_"+c.head().toByteLocStr() : ""); }
  String cstrBrief(Ctxt c) {
    StringBuilder buf = new StringBuilder();
    //buf.append(domC.indexOf(c));
    buf.append('{');
    for (int i = 0; i < c.length(); i++) {
      if (i > 0) buf.append(" | ");
      Quad q = c.get(i);
      buf.append(isAlloc(q) ? hstrBrief(q) : istrBrief(q));
    }
    buf.append('}');
    return buf.toString();
  }
  String vestrBrief(CtxtVar ve) {
    return ve.v+"@"+mstrBrief(domV.getMethod(ve.v))+":"+cstrBrief(ve.e);
  }
  String mstrBrief(jq_Method m) {
    return m.getDeclaringClass().shortName()+"."+m.getName();
  }

  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }

  ////////////////////////////////////////////////////////////

  class Query {
    Quad e;
    boolean proven = false;
    Abstraction abs = new Abstraction(); // Current abstraction for this query

    Query(Quad e) { this.e = e; }
  }

  class Abstraction {
    void clear() {
      proven = true;
      Sa.clear();
      Sv.clear();
    }

    @Override public int hashCode() {
      return Sa.hashCode() * 37 + Sv.hashCode();
    }
    @Override public boolean equals(Object _that) {
      Abstraction that = (Abstraction)_that;
      return Sa.equals(that.Sa) && Sv.equals(that.Sv);
    }

    @Override public String toString() {
      return String.format("A_%s%s|V_%s%s", Sa.size(), lengthHistogram(Sa), Sv.size(), lengthHistogram(Sv));
    }
    String size() { return Sa.size()+","+Sv.size(); }

    // Note: we will double count if this is disjoint from that
    void add(Abstraction that) {
      proven &= that.proven;
      Sa.addAll(that.Sa);
      Sv.addAll(that.Sv);
    }

    // Number of contexts in which variables were analyzed
    int sizeC() {
      Set<Ctxt> set = new HashSet<Ctxt>();
      for (CtxtVar ve : Sv) set.add(ve.e);
      return set.size();
    }

    boolean proven = true; // True until proven not true via counterexample
    HashSet<Ctxt> Sa = new HashSet<Ctxt>(); // Abstract objects
    HashSet<CtxtVar> Sv = new HashSet<CtxtVar>(); // Contextual variables
  }

  private void init() {
    X = Execution.v("adaptive");  

    // Immutable inputs
    domV = (DomV) ClassicProject.g().getTrgt("V"); ClassicProject.g().runTask(domV);
    domM = (DomM) ClassicProject.g().getTrgt("M"); ClassicProject.g().runTask(domM);
    domI = (DomI) ClassicProject.g().getTrgt("I"); ClassicProject.g().runTask(domI);
    domH = (DomH) ClassicProject.g().getTrgt("H"); ClassicProject.g().runTask(domH);
    domE = (DomE) ClassicProject.g().getTrgt("E"); ClassicProject.g().runTask(domE);
    relEV = (ProgramRel) ClassicProject.g().getTrgt("EV"); ClassicProject.g().runTask(relEV); relEV.load();

    // Mutable inputs
    relVEA = (ProgramRel) ClassicProject.g().getTrgt("VEA");
    relAFA = (ProgramRel) ClassicProject.g().getTrgt("AFA");
    relFA = (ProgramRel) ClassicProject.g().getTrgt("FA");

    // Output
    domC = (DomC) ClassicProject.g().getTrgt("C");
    relAH = (ProgramRel) ClassicProject.g().getTrgt("AH");
    relCfromMIC = (ProgramRel) ClassicProject.g().getTrgt("CfromMIC");
    relCfromMA = (ProgramRel) ClassicProject.g().getTrgt("CfromMA");
    relAfromHC = (ProgramRel) ClassicProject.g().getTrgt("AfromHC");
    relEfromVC = (ProgramRel) ClassicProject.g().getTrgt("EfromVC");
    relobjI = (ProgramRel) ClassicProject.g().getTrgt("objI");
    relRelevantQueryE = (ProgramRel) ClassicProject.g().getTrgt("relevantQueryE");

    _H = (Quad)domH.get(1);
    _V = domV.get(1);
  }

  void finish() {
    relEV.close();
    X.finish(null);
  }

  public void run() {
    init();

    this.verbose = X.getIntArg("verbose", 0);
    this.maxIters = X.getIntArg("maxIters", 1);
    //this.maxHintSize = X.getIntArg("maxHintSize", 10);
    this.refineA = X.getBooleanArg("refineA", false);
    this.refineV = X.getBooleanArg("refineV", false);
    this.useDatalogRelevance = X.getBooleanArg("useDatalogRelevance", false);
    this.initRefinementRadiusA = X.getIntArg("initRefinementRadiusA", 0);
    this.initRefinementRadiusV = X.getIntArg("initRefinementRadiusV", 0);
    this.useObjectSensitivity = X.getBooleanArg("useObjectSensitivity", false);
    this.departWhenRefine = X.getBooleanArg("departWhenRefine", false);

    X.putOption("version", 1);
    X.putOption("program", System.getProperty("chord.work.dir"));
    /*X.putOption("maxIters", maxIters);
    //X.putOption("maxHintSize", maxHintSize);
    X.putOption("refineA", refineA);
    X.putOption("refineV", refineV);
    X.putOption("initRefinementRadiusA", initRefinementRadiusA);
    X.putOption("initRefinementRadiusV", initRefinementRadiusV);
    X.putOption("useObjectSensitivity", useObjectSensitivity);
    X.putOption("departWhenRefine", departWhenRefine);*/
    X.flushOptions();

    init0CFA(); // STEP (INIT)

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      computeC(); // Step (COMPUTE_C)
      runAnalysis(); // Step (ANALYSIS)

      backupRelations(iter);
      refinementRadiusA = initRefinementRadiusA + (iter-1);
      refinementRadiusV = initRefinementRadiusV + (iter-1);

      Abstraction hotAbs = useDatalogRelevance ? pruneAbstractionDatalog() : pruneAbstraction(); // STEP (PRUNE)
      outputStatus(iter);
      //outputHint(iter);

      if (hotAbs == null) {
        X.logs("Proven all queries, exiting...");
        X.putOutput("conclusion", "prove");
        break;
      }
      if (iter == maxIters) {
        X.logs("Reached maximum number of iterations, exiting...");
        X.putOutput("conclusion", "max");
        break;
      }

      if (converged()) {
        X.logs("Refinement converged, exiting...");
        X.putOutput("conclusion", "conv");
        break;
      }

      refineAbstraction(hotAbs); // Step (REFINE)
    }
    finish();
  }

  void backupRelations(int iter) {
    X.logs("backupRelations");
    try {
      if (X.getBooleanArg("saveRelations", false)) {
        String path = X.path(""+iter);
        new File(path).mkdir();
        domC.save(path, true);

        for (String name : new String[] { "CfromMIC", "CfromMA", "AfromHC", "EfromVC", "AH", "VEA", "AFA", "FA", "escA" }) {
          X.logs("  Saving relation "+name);
          ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt(name);
          rel.load();
          rel.print(path);
          //rel.close(); // Crashes
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    /*PrintWriter out = OutDirUtils.newPrintWriter("globalC.txt");
    for (int i = 0; i < globalC.size(); i++)
      out.println(cstrVerbose(globalC.get(i)));
    out.close();*/
  }

  // Step (INIT)
  void init0CFA() {
    ClassicProject.g().runTask(initTaskName); 

    // Reachable things
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableH"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad h : result) hSet.add(h);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableI"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad i : result) iSet.add(i);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableV"); rel.load();
      Iterable<Register> result = rel.getAry1ValTuples();
      for (Register v : result) vSet.add(v);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableM"); rel.load();
      Iterable<jq_Method> result = rel.getAry1ValTuples();
      for (jq_Method m : result) mSet.add(m);
      rel.close();
    }

    // h2m, im
    for (Quad i : iSet) {
      i_this_h.put(i, new ArrayList<Quad>());
      im.put(i, new ArrayList<jq_Method>());
    }
    for (jq_Method m : mSet) {
      rev_im.put(m, new ArrayList<Quad>());
      rev_h2m.put(m, new ArrayList<Quad>());
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("IthisH"); rel.load();
      PairIterable<Quad,Quad> result = rel.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad i = pair.val0;
        Quad h = pair.val1;
        assert iSet.contains(i) : istr(i);
        assert hSet.contains(h) : hstr(h);
        i_this_h.get(i).add(h);
      }
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("HtoM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad h = pair.val0;
        jq_Method m = pair.val1;
        assert hSet.contains(h) : hstr(h);
        assert mSet.contains(m) : m;
        rev_h2m.get(m).add(h);
      }
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("IM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad i = pair.val0;
        jq_Method m = pair.val1;
        assert iSet.contains(i) : istr(i);
        im.get(i).add(m);
        rev_im.get(m).add(i);
      }
      rel.close();
    }
    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s, |vSet| = %s, |mSet| = %s", hSet.size(), iSet.size(), vSet.size(), mSet.size());

    // Compute which queries we should answer in the whole program
    String focus = X.getStringArg("eFocus", null);
    if (focus != null) {
      for (String e : focus.split(","))
        queries.add(new Query(domE.get(Integer.parseInt(e))));
    }
    else {
      /*computedExcludedClasses();
      for (Quad e : domE)
        if (!computeStatementIsExcluded(e) && e2v(e) != null) queries.add(new Query(e));*/
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("queryE"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad e : result) queries.add(new Query(e));
      rel.close();
    }
    X.logs("%s queries", queries.size());

    //// Now run 0-CFA (again) using the slivers framework.

    // Use object sensitivity?
    relobjI.zero();
    if (useObjectSensitivity) {
      for (Quad i : iSet) relobjI.add(i);
    }
    relobjI.save();

    // Run with initial abstraction: separate all allocation sites
    for (Quad h : hSet)
      abs.Sa.add(emptyCtxt.append(h));
    for (Register v : vSet)
      abs.Sv.add(new CtxtVar(v, emptyCtxt));
  }

  int numProven() {
    int n = 0;
    for (Query q : queries)
      if (q.proven) n++;
    return n;
  }

  // Don't get size of abstraction
  Abstraction pruneAbstractionDatalog() {
    abs.clear();
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("relevantA"); rel.load();
      Iterable<Ctxt> result = rel.getAry1ValTuples();
      for (Ctxt a : result) abs.Sa.add(a);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("relevantVE"); rel.load();
      PairIterable<Register,Ctxt> result = rel.getAry2ValTuples();
      for (Pair<Register,Ctxt> pair : result) {
        Register v = pair.val0;
        Ctxt e = pair.val1;
        abs.Sv.add(new CtxtVar(v, e));
      }
      rel.close();
    }
    Set<Quad> esc = new HashSet();
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("relevantEscE"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad e : result) esc.add(e);
      rel.close();
    }
    X.logs("%s of relevant queries unproven", esc.size());

    for (Query q : queries) {
      if (q.proven) continue;
      q.proven = !esc.contains(q.e);
    }
    X.logs("SUMMARY(pruned): %s", abs);
    X.logs("STATUS: proved %s/%s queries", numProven(), queries.size());
    return abs; // Refine everything
  }

  // Step (PRUNE): prune the abstraction.
  // Also return subset of abstraction to refine or null if nothing to refine.
  int numGlobEncounters;
  Abstraction pruneAbstraction() {
    // Queries already proven
    int numProven = numProven();

    X.logs("pruneAbstraction(): focus on %d unproven queries (out of %d total)", queries.size()-numProven, queries.size());
    relVEA.load();
    relAFA.load();
    relFA.load();

    abs.clear(); // We're going to start afresh (this will contain the pruned abstraction)
    TObjectIntHashMap<Ctxt> dists = new TObjectIntHashMap<Ctxt>();
    Abstraction hotAbs = new Abstraction();

    // When tracing back from an abstract object a, cache the abstraction that we need
    Map<Ctxt,Abstraction> a2abs = new HashMap<Ctxt,Abstraction>();
    Map<Ctxt,Abstraction> a2hotAbs = new HashMap<Ctxt,Abstraction>();

    numGlobEncounters = 0;
    for (Query q : queries) { // For each query...
      if (q.proven) continue; // ...that has not been proven

      Register v = e2v(q.e); // ...which accesses a field of v

      // Find all slivers that v could point to... [pointsTo]
      RelView view = relVEA.getView();
      view.selectAndDelete(0, v); // Variable
      view.delete(1); // in any context
      Iterable<Ctxt> result = view.getAry1ValTuples();
      List<Ctxt> pointsTo = new ArrayList<Ctxt>();
      Ctxt mina = null;
      for (Ctxt a : result) { // For each such sliver c...
        if (a.length() == 0) { // Ignore things that point to glob
          //X.errors("Invariant broken: Query %s (variable %s) points to glob!", estr(e), vstr(v));
          numGlobEncounters++;
          continue;
        }
        pointsTo.add(a);
        if (mina == null || domH.indexOf(a.head()) < domH.indexOf(mina.head())) mina = a;
      }
      view.free();
      // Just look at one element of the points-to set
      if (X.getBooleanArg("takeOnePointsTo", false)) {
        pointsTo.clear();
        if (mina != null) pointsTo.add(mina);
      }

      // Compute the (pruned) abstraction for this query q
      q.abs.clear();
      TObjectIntHashMap<Ctxt> qDists = new TObjectIntHashMap<Ctxt>();
      for (Ctxt a : pointsTo) {
        // Trace back from each pivot c (use cache if possible)
        Abstraction aAbs = a2abs.get(a);
        if (aAbs == null) a2abs.put(a, aAbs = new Abstraction());
        minMerge(qDists, traceBack(aAbs, a, false));
        q.abs.add(aAbs);
      }
      q.proven = q.abs.proven; // Did we prove it?

      if (q.proven) {
        numProven++;
        X.logs("QUERY DONE (PROVEN): %s ; dist%s (|pts|=%s): %s",
            q.abs, distHistogram(qDists), pointsTo.size(), estr(q.e));
      }
      /*else if (qAbs.size() <= maxHintSize) {
        // Done - don't do any more refinement (DON'T USE THIS)
        numDone++;
        X.logs("QUERY DONE (SMALL): %s |abs| = %s <= %s (|pts|=%s): %s",
            qAbs.distHist, qAbs.size(), maxHintSize, pointsTo.size(), estr(q.e));
      }*/
      else {
        abs.add(q.abs); // Add to the main abstraction
        minMerge(dists, qDists);

        Abstraction qHotAbs = new Abstraction();
        // Choose some influential slivers to refine
        for (Ctxt a : pointsTo) {
          Abstraction aHotAbs = a2hotAbs.get(a);
          if (aHotAbs == null) {
            a2hotAbs.put(a, aHotAbs = new Abstraction());
            traceBack(aHotAbs, a, true);
          }
          qHotAbs.add(aHotAbs);
        }
        hotAbs.add(qHotAbs);

        X.logs("QUERY REFINE: %s ; dist %s ; chose %s/%s (|pts|=%s): %s",
          q.abs, distHistogram(qDists), qHotAbs.size(), q.abs.size(), pointsTo.size(), estr(q.e));
      }
    }

    X.logs("numGlobEncounters = %s", numGlobEncounters);
    X.logs("STATUS: proved %s/%s queries", numProven, queries.size());
    X.logs("STATUS: chose %s/%s slivers to refine across remaining %s/%s queries",
        hotAbs.size(), abs.size(), queries.size()-numProven, queries.size());

    // Print histogram of slivers
    X.logs("DIST(pruned): %s", distHistogram(dists));
    X.logs("SUMMARY(pruned): %s", abs);
    X.logs("SUMMARY(selected): %s", hotAbs);

    relVEA.close();
    relAFA.close();
    relFA.close();

    return numProven < queries.size() ? hotAbs : null;
  }

  boolean converged() {
    if (results.size() < 2) return false;
    Result a = results.get(results.size()-2);
    Result b = results.get(results.size()-1);
    return a.absHashCode == b.absHashCode;
  }

  void outputStatus(int iter) {
    // abs is only for unproven queries; for others, look at q.abs
    Abstraction totalAbs = new Abstraction();
    for (Query q : queries) totalAbs.add(q.abs);
   
    X.addSaveFiles("abstraction.Sa."+iter, "abstraction.Sv."+iter);
    {
      PrintWriter out = OutDirUtils.newPrintWriter("abstraction.Sa."+iter);
      for (Ctxt a : totalAbs.Sa)
        out.println(cstrBrief(a));
      out.close();
    }
    {
      PrintWriter out = OutDirUtils.newPrintWriter("abstraction.Sv."+iter);
      for (CtxtVar ve : totalAbs.Sv)
        out.println(vestrBrief(ve));
      out.close();
    }

    int numProven = numProven();
    Result result = new Result();
    result.numProven = numProven;
    result.absHashCode = abs.hashCode();
    result.absSummary = abs.toString();
    results.add(result);

    X.putOutput("currIter", iter);
    X.putOutput("numEscaping", queries.size()-numProven);
    X.putOutput("numLocal", numProven);
    X.putOutput("totalSizeA", totalAbs.Sa.size());
    X.putOutput("totalSizeC", totalAbs.sizeC());
    X.putOutput("totalSizeV", totalAbs.Sv.size());
    X.putOutput("numProvenHistory", numProvenHistory());
    X.flushOutput();
  }

  String numProvenHistory() {
    StringBuilder buf = new StringBuilder();
    for (Result r : results) {
      if (buf.length() > 0) buf.append(',');
      buf.append(r.numProven);
    }
    return buf.toString();
  }

  /*void outputHint(int iter) {
    // Output hints
    PrintWriter out = OutDirUtils.newPrintWriter("hints.txt");
    PrintWriter sout = OutDirUtils.newPrintWriter("hints-str.txt");
    for (Quad e : e2hints.keySet()) {
      sout.println(estr(e));
      Set<Ctxt> hint = e2hints.get(e);
      int ei = domE.indexOf(e);
      if (hint.size() == 0) { // Null hint
        sout.println("  NULL (query points to nothing)");
        out.println(ei + " -2"); // Query trivially true
      }
      else if (hint.size() <= maxHintSize) {
        for (Ctxt c : hint) {
          sout.println("  "+cstr(c));
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < c.length(); i++) {
            Quad q = c.get(i);
            sb.append(isAlloc(q) ? "H"+domH.indexOf(q) : "I"+domI.indexOf(q));
          }
          out.println(ei + " " + sb);
        }
      }
      else {
        sout.println("  GIVE_UP");
        out.println(ei + " -1"); // Just give up
      }
    }
    out.close();
    sout.close();

    // Print hint statistics
    int sumHintSize = 0;
    int maxHintSize = 0;
    for (Set<Ctxt> hint : e2hints.values()) {
      int size = hint.size();
      sumHintSize += size;
      maxHintSize = Math.max(maxHintSize, size);
    }
    X.putOutput("currIter", iter);
    X.putOutput("avgHintSize", 1.0*sumHintSize/e2hints.size());
    X.putOutput("maxHintSize", maxHintSize);
    X.putOutput("numQueries", e2hints.size());
    X.putOutput("numChosenInfSlivers", chosenInfSlivers.size());
    X.putOutput("numInfSlivers", infSlivers.size());
  }*/

  // For displaying
  class BFSGraph {
    HashMap<Object,List<Object>> treeEdges = new HashMap<Object,List<Object>>();
    HashMap<Object,List<Object>> auxEdges = new HashMap<Object,List<Object>>();

    void add(HashMap<Object,List<Object>> edges, Object c, Object b) {
      List<Object> bs = edges.get(c);
      if (bs == null) edges.put(c, bs = new ArrayList<Object>());
      bs.add(b);
    }

    String str(Object o) {
      if (o == null) return "THREAD_START";  // This is a jq_Field
      if (o instanceof Ctxt)
        return cstrBrief((Ctxt)o);
      else if (o instanceof CtxtVar)
        return vestrBrief((CtxtVar)o);
      else if (o instanceof jq_Field)
        return o.toString();
      else
        throw new RuntimeException("Unknown object (not abstract object, contextual variable or field: "+o);
    }

    void display(Object c) {
      X.logs("===== GRAPH =====");
      display(c, "");
    }
    void display(Object c, String prefix) {
      X.logs(prefix+str(c));
      String newPrefix = prefix+"  ";
      if (auxEdges.get(c) != null) {
        for (Object b : auxEdges.get(c))
          X.logs(newPrefix+str(b) + " ...");
      }
      if (treeEdges.get(c) != null) {
        for (Object b : treeEdges.get(c))
          display(b, newPrefix);
      }
    }
  }

  // Traceback in the heap from a0
  TObjectIntHashMap<Ctxt> traceBack(Abstraction abs, Ctxt a0, boolean limitDist) {
    // For debugging
    boolean buildGraph = verbose >= 3 && !limitDist;
    BFSGraph graph = null;
    if (buildGraph) graph = new BFSGraph();

    TObjectIntHashMap<Ctxt> dists = new TObjectIntHashMap<Ctxt>();

    // Initialize
    LinkedList<Ctxt> queue = new LinkedList<Ctxt>();
    dists.put(a0, 0);
    queue.addLast(a0);

    // Perform BFS
    while (queue.size() > 0) {
      Ctxt a = queue.removeFirst();
      int adist = dists.get(a);
      if (!limitDist || (refineA && adist <= refinementRadiusA)) // Add to abstraction
        abs.Sa.add(a);

      // Follow the heap backwards
      {
        RelView view = relAFA.getView();
        view.delete(1); // Any field
        view.selectAndDelete(2, a); // Going to a
        Iterable<Ctxt> result = view.getAry1ValTuples();
        for (Ctxt b : result) {
          if (b.length() == 0) { // Ignore going to glob
            //X.errors("Invariant broken: glob can reach %s", cstr(c));
            numGlobEncounters++;
            if (refineA && buildGraph) graph.add(graph.auxEdges, a, b);
            continue;
          }
          if (dists.contains(b)) {
            if (refineA && buildGraph) graph.add(graph.auxEdges, a, b);
            continue;
          }
          if (refineA && buildGraph) graph.add(graph.treeEdges, a, b);
          int bdist = adist+1;
          dists.put(b, bdist);
          queue.addLast(b);
        }
      }

      // Look at what variables point to a
      {
        RelView view = relVEA.getView();
        view.selectAndDelete(2, a);
        PairIterable<Register,Ctxt> result = view.getAry2ValTuples();
        for (Pair<Register,Ctxt> pair : result) {
          Register v = pair.val0;
          Ctxt e = pair.val1;
          CtxtVar ve = new CtxtVar(v, e);
          if (refineV && buildGraph) graph.add(graph.treeEdges, a, ve);
          int vdist = adist+1;
          if (!limitDist || (refineV && vdist <= refinementRadiusV))
            abs.Sv.add(ve);
        }
      }

      // Look at if global fields points to a
      {
        RelView view = relFA.getView();
        view.selectAndDelete(1, a);
        Iterable<jq_Field> result = view.getAry1ValTuples();
        for (jq_Field f : result) {
          if (buildGraph) graph.add(graph.treeEdges, a, f);
          abs.proven = false; // Thread-escaping!
        }
      }
    }

    // Record the graph
    if (buildGraph) graph.display(a0);
    return dists;
  }

  // Step (REFINE): Refine the hotAbs
  void refineAbstraction(Abstraction hotAbs) {
    X.logs("refineAbstraction()");
    Abstraction newAbs = new Abstraction();

    // Refine abstract objects (Sa)
    for (Ctxt a : abs.Sa) {
      // Keep as is
      if (!hotAbs.Sa.contains(a)) {
        newAbs.Sa.add(a);
        continue;
      }

      if (a.length() == 0) // Should not be trying to refine the glob
        throw new RuntimeException("We are trying to refine the glob - something went wrong!");

      List<Quad> extensions;
      if (useObjectSensitivity)
        extensions = rev_h2m.get(a.last().getMethod()); // Allocation sites
      else {
        extensions = rev_im.get(a.last().getMethod()); // Call sites
      }

      if (verbose >= 1) X.logs("Sa-REFINE %s (%s extensions)", cstrBrief(a), extensions.size());

      // If didn't extend, then assume that the last call site (c.last()) exists in an initial method,
      // which is not called by anything.
      // ASSUMPTION: initial methods are never called by some intermediate method.
      // Need to check this - might not be a valid assumption for k-object-sensitivity.
      if (extensions == null || extensions.size() == 0) {
        //if (verbose >= 2) X.logs("  Keep same sliver (%s extensions): %s", extensions.size(), cstrBrief(a));
        newAbs.Sa.add(a);
      }
      else {
        for (Quad q : extensions) {
          newAbs.Sa.add(a.append(q));
          if (verbose >= 2) X.logs("  New: %s", cstrBrief(a.append(q)));
        }
      }
    }

    // Refine contextual variables (Sv)
    for (CtxtVar ve : abs.Sv) {
      if (!hotAbs.Sv.contains(ve)) {
        newAbs.Sv.add(ve);
        continue;
      }
      Register v = ve.v;
      Ctxt e = ve.e;
      
      List<Quad> extensions;
      if (useObjectSensitivity) {
        extensions = e.length() == 0 ?
          rev_h2m.get(domV.getMethod(v)) : // Allocation sites which can lead to method containing that variable
          rev_h2m.get(e.last().getMethod()); // Allocation sites which can actually lead to the current allocation site
      }
      else {
        extensions = e.length() == 0 ?
          rev_im.get(domV.getMethod(v)) : // Call sites that call the containing method of this variable
          rev_im.get(e.last().getMethod()); // Call sites that lead to the last call site in the current chain
      }

      if (verbose >= 1) X.logs("Sv-REFINE %s (%s extensions)", vestrBrief(ve), extensions.size());

      // If didn't extend, then assume that the last call site (c.last()) exists in an initial method,
      // which is not called by anything.
      // ASSUMPTION: initial methods are never called by some intermediate method.
      if (extensions == null || extensions.size() == 0) {
        if (verbose >= 2) X.logs("  Keep: %s", vestrBrief(ve));
        newAbs.Sv.add(ve);
      }
      else {
        if (!departWhenRefine) {
          if (verbose >= 2) X.logs("  Keep: %s", vestrBrief(ve));
          newAbs.Sv.add(ve);
        }
        for (Quad q : extensions) {
          CtxtVar newve = new CtxtVar(ve.v, ve.e.append(q));
          newAbs.Sv.add(newve);
          if (verbose >= 2) X.logs("  New: %s", vestrBrief(newve));
        }
      }
    }

    X.logs("REFINE |Sa| : %s -> %s, |Sv| : %s -> %s",
        abs.Sa.size(), newAbs.Sa.size(),
        abs.Sv.size(), newAbs.Sv.size());
    X.logs("SUMMARY(refined): %s", newAbs);

    abs = newAbs;
  }

  void addSuffixesToDomC(Ctxt c) {
    for (int i = 0; i <= c.length(); i++)
      domC.getOrAdd(c.suffix(i));
  }

  // Step (COMPUTE_C): Use current abstraction (Sa,Sv) to populate domains and relations
  void computeC() {
    X.logs("computeC()");

    // The queries that we haven't proven yet.
    relRelevantQueryE.zero();
    for (Query q : queries)
      if (!q.proven) relRelevantQueryE.add(q.e);
    relRelevantQueryE.save();

    if (useObjectSensitivity) {
      // Make sure Sa has all of Sv's environments
      // Can mutate Sa because we're pruneAbstraction will nuke abs anyway
      int n = 0;
      for (CtxtVar ve : abs.Sv) {
        if (ve.e.length() > 0 && abs.Sa.add(ve.e)) n++;
      }
      X.logs("For object-sensitivity, added %s new contexts (abstract objects) of Sv into Sa", n);
    }

    //// Find minimal contexts for each method by iteration
    X.logs("Computing method contexts based on Sa and Sv");
    HashMap<jq_Method,Set<Ctxt>> contexts = new HashMap<jq_Method,Set<Ctxt>>(); // method m -> contexts I need to support
    // Initialize: have each method support the variables and abstract objects in that method.
    for (jq_Method m : mSet) {
      Set<Ctxt> set = new HashSet<Ctxt>();
      set.add(emptyCtxt);
      contexts.put(m, set);
    }
    for (CtxtVar ve : abs.Sv) {
      jq_Method m = domV.getMethod(ve.v);
      contexts.get(m).add(ve.e);
    }
    for (Ctxt a : abs.Sa) {
      jq_Method m = a.head().getMethod();
      contexts.get(m).add(a.tail()); 
    }
    // If m1 calls m2, then m1 better have the context that can support m2's relevant contexts.
    // Propagate these changes until convergence.
    while (true) {
      boolean changed = false;
      for (jq_Method m : contexts.keySet()) { // For each method m...
        List<Ctxt> cs = new ArrayList(contexts.get(m));
        for (Ctxt c : cs) { // it can be analyzed in context c...
          if (c.length() == 0) continue;
          jq_Method prevm = c.head().getMethod(); // Get method containing either the first allocation/call site of the context
          Ctxt prevc = useObjectSensitivity && m.isStatic() ? c : c.tail();
          changed |= contexts.get(prevm).add(prevc); // Add the context
        }
      }
      if (!changed) break;
    }

    // Put all suffixes into domC
    {
      domC.clear();
      for (Ctxt a : abs.Sa) addSuffixesToDomC(a);
      for (CtxtVar ve : abs.Sv) addSuffixesToDomC(ve.e);
      domC.save();
      X.logs("Converted %s into %s domain C elements", abs, domC.size());
    }

    // AH
    HashMap<Quad,List<Ctxt>> h2a = new HashMap<Quad,List<Ctxt>>();
    for (Quad h : hSet) h2a.put(h, new ArrayList<Ctxt>());
    relAH.zero();
    Set<Quad> inSomeContext = new HashSet<Quad>(); // set of heads of some context
    for (Ctxt a : abs.Sa) {
      relAH.add(a, a.head());
      h2a.get(a.head()).add(a);
      inSomeContext.add(a.head());
    }
    for (Quad h : hSet) { // [] stands for allocation sites which are not in any abstraction
      if (inSomeContext.contains(h)) continue;
      relAH.add(emptyCtxt, h);
      h2a.get(h).add(emptyCtxt);
    }
    relAH.save();

    // CfromMIC, CfromMA
    relCfromMIC.zero();
    relCfromMA.zero();
    if (!useObjectSensitivity) {
      for (Quad i : iSet) { // At call site i
        for (Ctxt c : contexts.get(i.getMethod())) { // ...in context c
          for (jq_Method m : im.get(i)) { // ...calling method m
            relCfromMIC.add(extend(i, c, contexts.get(m)), m, i, c); // Get the new context
          }
        }
      }
    }
    else {
      for (Quad i : iSet) { // At call site i
        for (Quad h : i_this_h.get(i)) { // ...this argument can point to something allocated at h
          for (Ctxt a : h2a.get(h)) { // Corresponding to some abstract object a
            for (jq_Method m : im.get(i)) { // ...calling method m
              relCfromMA.add(project(a, contexts.get(m)), m, a); // Get the new context
            }
          }
        }
      }
    }
    relCfromMIC.save();
    relCfromMA.save();

    // AfromHC
    relAfromHC.zero();
    for (Quad h : hSet) { // At allocation site h
      for (Ctxt c : contexts.get(h.getMethod())) // ...in context c
        relAfromHC.add(extend(h, c, abs.Sa), h, c); // Get the abstract object
    }
    relAfromHC.save();

    // EfromVC
    relEfromVC.zero();
    for (Register v : vSet) { // For variable v
      for (Ctxt c : contexts.get(domV.getMethod(v))) // ...in context c
        relEfromVC.add(extend(v, c, abs.Sv), v, c); // Get the context e for that variable
    }
    relEfromVC.save();
  }

  Ctxt project(Ctxt c, Set<Ctxt> ref) {
    // Return longest prefix of c in ref
    for (int k = c.length(); k >= 0; k--) { // Try to extend
      Ctxt d = c.prefix(k);
      if (ref.contains(d)) return d;
    }
    return emptyCtxt;
  }
  Ctxt extend(Quad q, Ctxt c, Set<Ctxt> ref) {
    // Create d = [q, c] but truncate to length k, where k is the maximum such that d is in ref
    for (int k = c.length()+1; k >= 0; k--) { // Try to extend
      Ctxt d = c.prepend(q, k);
      if (ref.contains(d)) return d;
    }
    return emptyCtxt;
  }
  Ctxt extend(Register v, Ctxt c, Set<CtxtVar> ref) {
    for (int k = c.length(); k >= 0; k--) {
      Ctxt e = c.prefix(k);
      CtxtVar ve = new CtxtVar(v, e);
      if (ref.contains(ve))
        return e;
    }
    return emptyCtxt;
  }

  // Step (ANALYSIS)
  void runAnalysis() {
    ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
    ClassicProject.g().setTaskDone(this); // We are generating all this stuff, so mark it as done...
    ClassicProject.g().setTrgtDone(domC);
    ClassicProject.g().setTrgtDone(relAH);
    ClassicProject.g().setTrgtDone(relCfromMIC);
    ClassicProject.g().setTrgtDone(relCfromMA);
    ClassicProject.g().setTrgtDone(relAfromHC);
    ClassicProject.g().setTrgtDone(relEfromVC);
    ClassicProject.g().setTrgtDone(relobjI);
    ClassicProject.g().setTrgtDone(relRelevantQueryE);
    ClassicProject.g().runTask(analysisTaskName);
  }

  Histogram lengthHistogram(Set slivers) {
    Histogram hist = new Histogram();
    for (Object c : slivers) {
      if (c instanceof Ctxt)
        hist.counts[((Ctxt)c).length()]++;
      else if (c instanceof CtxtVar)
        hist.counts[((CtxtVar)c).length()]++;
      else
        throw new RuntimeException("Can't get length of "+c);
    }
    return hist;
  }

  Histogram distHistogram(TObjectIntHashMap<Ctxt> dists) {
    final Histogram hist = new Histogram();
    dists.forEachEntry(new TObjectIntProcedure<Ctxt>() {
      public boolean execute(Ctxt c, int dist) {
        hist.counts[dist]++;
        return true;
      }
    });
    return hist;
  }

  // dest(key) <- min(dest(key), source(key)) for all keys
  <T> void minMerge(final TObjectIntHashMap<T> dest, final TObjectIntHashMap<T> source) {
    source.forEachEntry(new TObjectIntProcedure<T>() {
      public boolean execute(T key, int value) {
        if (dest.containsKey(key))
          dest.put(key, Math.min(dest.get(key), value));
        else
          dest.put(key, value);
        return true;
      }
    });
  }

  class Histogram {
    int[] counts = new int[1000];
    void clear() {
      for (int i = 0; i < counts.length; i++)
        counts[i] = 0;
    }
    public void add(int i) { counts[i]++; }
    public void add(Histogram h) {
      for (int i = 0; i < counts.length; i++)
        counts[i] += h.counts[i];
    }
    @Override public String toString() {
      StringBuilder buf = new StringBuilder();
      for (int n = 0; n < counts.length; n++) {
        if (counts[n] == 0) continue;
        if (buf.length() > 0) buf.append(" ");
        buf.append(n+":"+counts[n]);
      }
      return '['+buf.toString()+']';
    }
  }
}
