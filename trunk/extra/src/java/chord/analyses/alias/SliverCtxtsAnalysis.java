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
import chord.analyses.snapshot.Execution;
import chord.analyses.snapshot.StatFig;
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
  @Override public boolean equals(Object _that) {
    CtxtVar that = (CtxtVar)_that;
    return v.equals(that.v) && e.equals(that.e);
  }
  @Override public int hashCode() {
    return v.hashCode() * 37 + e.hashCode();
  }
}

class Query {
  Quad e;
  boolean proven = false;
  //Abstraction abs = new Abstraction(); // Abstraction that works for this query

  Query(Quad e) { this.e = e; }
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
 * CI1 --[CICM]--+--> CM1 --+
 *               |          |
 * CI2 --[CICM]--+          +--[MH]--> CH1 --[AFA]--> CH2 <--[VEA]-- V1
 *
 * (INIT) Initialize C, call 0-CFA to get VEA,AFA,CICM and ItoI,ItoH and reachableH,reachableI.
 * Iterate:
 *   (PRUNE_CONTEXT) Use congruence tests to reduce the number of contexts.
 *   (PRUNE_SLIVER) Follow VEA pointers forward once and AFA pointers backwards to find the *influential slivers* [infSlivers].
 *   Choose a subset of these slivers which are within a fixed number of hops from something that a query variable points to;
 *   these are the ones we want to refine [chosenInfSlivers].
 *   Terminology: a hint is the *influencing slivers* of a particular query e.
 *   If the influence set is small enough, then we break out.
 *   (REFINE) Given the chosen influential slivers, follow MH, CICM, and MI
 *   pointers backwards, splitting these slivers to produce refined slivers, expanding contexts if necessary
 *   The refined slivers determines our abstraction [refinedSlivers,refinedContexts].
 *   (COMPUTE_C) Compute C,CC,CI,CH from our computed abstraction.
 *   (ANALYSIS) Call k-CFA to obtain VEA,AFA,CICM.
 * (FULL) Set the heap abstraction to be the influential slivers and run the
 * flow-sensitive, context-sensitive full program analysis.
 *
 * MH, MI: fixed in advance
 * C,CC,CI,CH --(INIT)--> ItoI,ItoH,VEA,AFA,CICM --(PRUNE_CONTEXT,PRUNE_SLIVER,REFINE)--> refinedContexts,refinedSlivers
 *                                        ^                                                              |
 *                                        |                                                              |
 *                                        +-------------(ANALYSIS)-- C,CC,CI,CH <--(COMPUTE_C)-----------+
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
  produces = { "C", "AH", "CfromIC", "AfromHC", "EfromVC" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis {
  private Execution X;

  static final String analysisTaskName = "cspa-sliver-dlog";

  // Immutable inputs
  private DomV domV;
  private DomM domM;
  private DomI domI;
  private DomH domH;
  private DomE domE;
  private ProgramRel relEV;

  // Produced by the analysis
  private ProgramRel relVEA;
  private ProgramRel relAFA;
  private ProgramRel relFA;

  // Produced by us for the analysis
  private DomC domC;
  private ProgramRel relAH;
  private ProgramRel relCfromIC;
  private ProgramRel relAfromHC;
  private ProgramRel relEfromVC;

  // Canonical
  Quad _H;
  Register _V;

  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);

  // Options
  int verbose;
  int maxIters;
  int maxHintSize;
  int initRefinementRadius;
  boolean useObjectSensitivity;

  IndexMap<Ctxt> globalC = new IndexMap<Ctxt>(); // dom C will change over time, but keep a consistent indexing for comparing across iterations

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  Set<Register> vSet = new HashSet<Register>();
  HashMap<Quad,List<Quad>> i2h = new HashMap<Quad,List<Quad>>(); // i -> list of allocation sites that can be prepended to i
  HashMap<Quad,List<Quad>> i2i = new HashMap<Quad,List<Quad>>(); // i -> list of call sites that can be prepended to i
  HashMap<Quad,List<Quad>> h2h = new HashMap<Quad,List<Quad>>(); // i -> list of call sites that can be prepended to i
  HashMap<Quad,List<Quad>> rev_i2i = new HashMap<Quad,List<Quad>>();
  HashMap<Quad,List<Quad>> rev_h2h = new HashMap<Quad,List<Quad>>();
  HashMap<jq_Method,List<Quad>> callees = new HashMap<jq_Method,List<Quad>>(); // m -> list of call sites (reverse of IM)

  // Current abstraction
  Abstraction abs = new Abstraction();
  int refinementRadius; // Maximum number of hops that any candidate is away from a pivot

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
  String estr(Quad e) { return String.format("%s[%s]", domE.indexOf(e), qstr(e)); }
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
    buf.append(domC.indexOf(c));
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
    return ve.v+":"+cstrBrief(ve.e);
  }

  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }

  ////////////////////////////////////////////////////////////

  class Abstraction {
    void clear() {
      Sa.clear();
      Sv.clear();
      distHist.clear();
      lengthHist.clear();
    }

    @Override public String toString() { return String.format("|Sa|=%s,|Sv|=%s", Sa.size(), Sv.size()); }

    int size() { return Sa.size() + Sv.size(); }
    void add(Abstraction that) {
      Sa.addAll(that.Sa);
      Sv.addAll(that.Sv);
      distHist.add(that.distHist);
      lengthHist.add(that.lengthHist);
    }

    boolean proven = true; // True until proven not true via counterexample
    HashSet<Ctxt> Sa = new HashSet<Ctxt>(); // Abstract objects
    HashSet<CtxtVar> Sv = new HashSet<CtxtVar>(); // Contextual variables
    Histogram distHist = new Histogram(); // Distribution over distances of abstract objects
    Histogram lengthHist = new Histogram(); // Distribution over distances of abstract objects
  }

  private void init() {
    X = Execution.v("hints");  
    X.addSaveFiles("hints.txt", "hints-str.txt");

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
    relCfromIC = (ProgramRel) ClassicProject.g().getTrgt("CfromIC");
    relAfromHC = (ProgramRel) ClassicProject.g().getTrgt("AfromHC");
    relEfromVC = (ProgramRel) ClassicProject.g().getTrgt("EfromVC");

    _H = (Quad)domH.get(1);
    _V = domV.get(1);

    // Compute which queries we should answer in the whole program
    String focus = X.getStringArg("eFocus", null);
    if (focus != null) {
      for (String e : focus.split(","))
        queries.add(new Query(domE.get(Integer.parseInt(e))));
    }
    else {
      computedExcludedClasses();
      for (Quad e : domE)
        if (!computeStatementIsExcluded(e) && e2v(e) != null) queries.add(new Query(e));
    }
  }

  void finish() {
    relEV.close();
    X.finish(null);
  }

  public void run() {
    init();

    this.verbose = X.getIntArg("verbose", 0);
    this.maxIters = X.getIntArg("maxIters", 1);
    this.maxHintSize = X.getIntArg("maxHintSize", 10);
    this.initRefinementRadius = X.getIntArg("initRefinementRadius", 2);
    this.useObjectSensitivity = X.getBooleanArg("useObjectSensitivity", false);

    java.util.HashMap<Object,Object> options = new java.util.LinkedHashMap<Object,Object>();
    options.put("version", 1);
    options.put("program", System.getProperty("chord.work.dir"));
    options.put("maxIters", maxIters);
    options.put("maxHintSize", maxHintSize);
    options.put("initRefinementRadius", initRefinementRadius);
    options.put("useObjectSensitivity", useObjectSensitivity);
    X.writeMap("options.map", options);

    init0CFA(); // STEP (INIT)

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      backupRelations(iter);
      refinementRadius = initRefinementRadius + (iter-1);

      Abstraction hotAbs = computeInfSlivers(); // STEP (PRUNE)
      //outputHint(iter);

      if (hotAbs == null) {
        X.logs("Analysis goal reached, exiting...");
        break;
      }
      if (iter == maxIters) {
        X.logs("Reached maximum number of iterations, exiting...");
        break;
      }

      refineSlivers(hotAbs); // Step (REFINE)
      computeC(); // Step (COMPUTE_C)

      ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
      ClassicProject.g().setTaskDone(this); // We are generating all this stuff, so mark it as done...
      ClassicProject.g().setTrgtDone(domC);
      ClassicProject.g().setTrgtDone(relAH);
      ClassicProject.g().setTrgtDone(relCfromIC);
      ClassicProject.g().setTrgtDone(relAfromHC);
      ClassicProject.g().setTrgtDone(relEfromVC);
      ClassicProject.g().runTask(analysisTaskName); // Step (ANALYSIS)
    }

    finish();
  }

  void backupRelations(int iter) {
    X.logs("backupRelations");
    try {
      String path = X.path(""+iter);
      new File(path).mkdir();
      domC.save(path, true);
      /*for (ProgramRel rel : new ProgramRel[] { relCfromIC, relAH, relVEA, relAFA }) {
        rel.load();
        rel.print(path);
        //rel.close();
      }*/
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    PrintWriter out = OutDirUtils.newPrintWriter("globalC.txt");
    for (int i = 0; i < globalC.size(); i++)
      out.println(cstrVerbose(globalC.get(i)));
    out.close();
  }

  // Step (INIT)
  void init0CFA() {
    // Construct C
    domC.clear();
    domC.getOrAdd(emptyCtxt); // Empty context
    for (int hi = 1; hi < domH.size(); hi++) {
      // Create a separate context for each reachable allocation site
      Quad h = (Quad)domH.get(hi);
      domC.getOrAdd(emptyCtxt.append(h));
    }
    domC.save();
    for (Ctxt c : domC) globalC.getOrAdd(c);

    // Construct AH
    relAH.zero();
    for (int hi = 1; hi < domH.size(); hi++) {
      Quad h = (Quad)domH.get(hi);
      relAH.add(emptyCtxt.append(h), h); // Create a separate context for each reachable allocation site
    }
    relAH.save();

    // Construct CfromIC (no context)
    if (!useObjectSensitivity) {
      relCfromIC.zero();
      for (int i = 0; i < domI.size(); i++)
        relCfromIC.add(emptyCtxt, domI.get(i), emptyCtxt);
      relCfromIC.save();
    }

    // Construct AfromHC (build up abstract object)
    relAfromHC.zero();
    for (int hi = 1; hi < domH.size(); hi++) {
      Quad h = (Quad)domH.get(hi);
      relAfromHC.add(emptyCtxt.append(h), h, emptyCtxt);
    }
    relAfromHC.save();

    // Construct EfromVC (no context)
    relEfromVC.zero();
    for (int v = 0; v < domV.size(); v++)
      relEfromVC.add(emptyCtxt, domV.get(v), emptyCtxt);
    relEfromVC.save();

    ClassicProject.g().runTask(analysisTaskName); 

    // Only consider reachable stuff
    {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("reachableV"); rel.load();
      Iterable<Register> result = rel.getAry1ValTuples();
      for (Register v : result) vSet.add(v);
      rel.close();
    }
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

    for (Quad i : iSet) {
      i2h.put(i, new ArrayList<Quad>());
      i2i.put(i, new ArrayList<Quad>());
      rev_i2i.put(i, new ArrayList<Quad>());
    }

    for (Quad h : hSet) {
      h2h.put(h, new ArrayList<Quad>());
      rev_h2h.put(h, new ArrayList<Quad>());
    }

    // Build callees
    {
      ProgramRel relIM = (ProgramRel)ClassicProject.g().getTrgt("IM"); relIM.load();
      PairIterable<Quad,jq_Method> result = relIM.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad i = pair.val0;
        jq_Method m = pair.val1;
        assert iSet.contains(i) : istr(i);
        List<Quad> l = callees.get(m);
        if (l == null) callees.put(m, l = new ArrayList<Quad>());
        l.add(i);
      }
      relIM.close();
    }

    // Used fixed call graph to determine:
    //  - set of reachable H and I.
    //  - information for building slivers (overapproximation suffices)
    {
      ProgramRel relItoH = (ProgramRel)ClassicProject.g().getTrgt("ItoH"); relItoH.load();
      PairIterable<Quad,Quad> result = relItoH.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad i = pair.val0;
        Quad h = pair.val1;
        assert iSet.contains(i) : istr(i);
        assert hSet.contains(h) : hstr(h);
        i2h.get(i).add(h);
      }
      relItoH.close();
    }
    {
      ProgramRel relItoI = (ProgramRel)ClassicProject.g().getTrgt("ItoI"); relItoI.load();
      PairIterable<Quad,Quad> result = relItoI.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad i = pair.val0;
        Quad j = pair.val1;
        assert iSet.contains(i) : istr(i);
        assert iSet.contains(j) : istr(j);
        i2i.get(i).add(j);
        rev_i2i.get(j).add(i);
      }
      relItoI.close();
    }
    {
      ProgramRel relHtoH = (ProgramRel)ClassicProject.g().getTrgt("HtoH"); relHtoH.load();
      PairIterable<Quad,Quad> result = relHtoH.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad g = pair.val0;
        Quad h = pair.val1;
        assert hSet.contains(g) : hstr(g);
        assert hSet.contains(h) : hstr(h);
        h2h.get(g).add(h);
        rev_h2h.get(h).add(g);
      }
      relHtoH.close();
    }

    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s", hSet.size(), iSet.size());
  }

  // Step (PRUNE): compute influential slivers
  // Return subset to refine or null if nothing to refine.
  int numGlobEncounters;
  Abstraction computeInfSlivers() {
    X.logs("computeInfSlivers()");
    relVEA.load();
    relAFA.load();
    relFA.load();

    abs.clear();
    Abstraction hotAbs = new Abstraction();
    hotAbs.clear();

    // When tracing back from an abstract object a, cache the abstraction that we need
    Map<Ctxt,Abstraction> a2abs = new HashMap<Ctxt,Abstraction>();
    Map<Ctxt,Abstraction> a2hotAbs = new HashMap<Ctxt,Abstraction>();

    int numDone = 0;
    numGlobEncounters = 0;
    for (Query q : queries) { // For each query...
      if (q.proven) continue;

      Register v = e2v(q.e); // ...which accesses a field of v

      // Find all slivers that v could point to... [pointsTo]
      RelView view = relVEA.getView();
      view.selectAndDelete(0, v); // Variable
      view.delete(1); // in any context
      Iterable<Ctxt> result = view.getAry1ValTuples();
      List<Ctxt> pointsTo = new ArrayList<Ctxt>();
      Ctxt minc = null;
      for (Ctxt c : result) { // For each such sliver c...
        if (c.length() == 0) { // Ignore things that point to glob
          //X.errors("Invariant broken: Query %s (variable %s) points to glob!", estr(e), vstr(v));
          numGlobEncounters++;
          continue;
        }
        pointsTo.add(c);
        if (minc == null || domH.indexOf(c.head()) < domH.indexOf(minc.head())) minc = c;
      }
      view.free();
      // Just look at one element of the points-to set
      if (X.getBooleanArg("takeOnePointsTo", false)) {
        pointsTo.clear();
        pointsTo.add(minc);
      }

      // Compute the abstraction for this query q
      Abstraction qAbs = new Abstraction();
      for (Ctxt a : pointsTo) {
        // Trace back from each pivot c (use cache if possible)
        Abstraction aAbs = a2abs.get(a);
        if (aAbs == null) {
          a2abs.put(a, aAbs = new Abstraction());
          traceBack(aAbs, a, Integer.MAX_VALUE);
        }
        qAbs.add(aAbs);
      }
      q.proven = qAbs.proven;
      abs.add(qAbs);

      if (q.proven) {
        numDone++;
        X.logs("QUERY DONE (PROVEN): %s |abs| = %s (|pts|=%s): %s",
            qAbs.distHist, qAbs.size(), pointsTo.size(), estr(q.e));
      }
      else if (qAbs.size() <= maxHintSize) {
        // Done - don't do any more refinement (DON'T USE THIS)
        numDone++;
        X.logs("QUERY DONE (SMALL): %s |abs| = %s <= %s (|pts|=%s): %s",
            qAbs.distHist, qAbs.size(), maxHintSize, pointsTo.size(), estr(q.e));
      }
      else {
        Abstraction qHotAbs = new Abstraction();
        // Choose some influential slivers to refine
        for (Ctxt a : pointsTo) {
          Abstraction aHotAbs = a2hotAbs.get(a);
          if (aHotAbs == null) {
            a2hotAbs.put(a, aHotAbs = new Abstraction());
            traceBack(aHotAbs, a, refinementRadius);
          }
          qHotAbs.add(aHotAbs);
        }
        hotAbs.add(qHotAbs);

        X.logs("QUERY REFINE: %s chose %s/%s as hint [radius<=%s] (|pts|=%s): %s",
            qHotAbs.distHist,
            qHotAbs.size(), qAbs.size(), refinementRadius,
            pointsTo.size(), estr(q.e));
      }
    }

    X.logs("numGlobEncounters = %s", numGlobEncounters);
    X.logs("TOTAL: %s/%s queries are done", numDone, queries.size());
    X.logs("TOTAL: chose %s/%s influential slivers to refine across %s/%s queries",
        hotAbs.size(), abs.size(), queries.size()-numDone, queries.size());

    // Print histogram of slivers
    X.logs("LENGTH(hotAbs): %s", hotAbs.lengthHist);
    X.logs("LENGTH(abs): %s", abs.lengthHist);

    relVEA.close();
    relAFA.close();
    relFA.close();

    return numDone < queries.size() ? hotAbs : null;
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
      if (o instanceof Ctxt)
        return cstrBrief((Ctxt)o);
      else
        return vestrBrief((CtxtVar)o);
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
  void traceBack(Abstraction abs, Ctxt a0, int maxDist) {
    boolean buildGraph = verbose >= 3 && maxDist == Integer.MAX_VALUE;
    BFSGraph graph = null;
    if (buildGraph) graph = new BFSGraph();

    TObjectIntHashMap<Ctxt> dists = new TObjectIntHashMap<Ctxt>();
    Set<Ctxt> visited = abs.Sa;

    // Initialize
    LinkedList<Ctxt> queue = new LinkedList<Ctxt>();
    dists.put(a0, 0);
    queue.addLast(a0);
    visited.add(a0);

    // Perform BFS
    while (queue.size() > 0) {
      Ctxt a = queue.removeFirst();
      int adist = dists.get(a);

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
            if (buildGraph) graph.add(graph.auxEdges, a, b);
            continue;
          }
          if (visited.contains(b)) {
            if (buildGraph) graph.add(graph.auxEdges, a, b);
            continue;
          }
          if (buildGraph) graph.add(graph.treeEdges, a, b);
          visited.add(b);
          int bdist = adist+1;
          abs.distHist.add(bdist);
          abs.lengthHist.add(b.length());
          dists.put(b, bdist);
          if (bdist < maxDist)
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
          if (buildGraph) graph.add(graph.treeEdges, a, ve);
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
  }

  // Step (REFINE): Refine the hotAbs
  void refineSlivers(Abstraction hotAbs) {
    X.logs("refineSlivers()");
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

      if (verbose >= 1) X.logs("Refining %s:", cstr(a));

      List<Quad> extensions;
      if (useObjectSensitivity)
        extensions = rev_h2h.get(a.last()); // Allocation sites
      else {
        extensions = a.length() == 1 ?
          callees.get(a.head().getMethod()) : // Call sites that call the containing method of this allocation site (c.head())
          rev_i2i.get(a.last()); // Call sites that lead to the last call site in the current chain
      }

      // If didn't extend, then assume that the last call site (c.last()) exists in an initial method,
      // which is not called by anything.
      // ASSUMPTION: initial methods are never called by some intermediate method.
      if (extensions == null || extensions.size() == 0) {
        if (verbose >= 2) X.logs("  Keep same sliver (no extensions): %s", cstr(a));
        newAbs.Sa.add(a);
      }
      else {
        for (Quad q : extensions) {
          newAbs.Sa.add(a.append(q));
          if (verbose >= 2) X.logs("  New sliver: %s", cstr(a.append(q)));
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
      if (useObjectSensitivity)
        extensions = rev_h2h.get(e.last()); // Allocation sites
      else {
        extensions = e.length() == 0 ?
          callees.get(domV.getMethod(v)) : // Call sites that call the containing method of this variable
          rev_i2i.get(e.last()); // Call sites that lead to the last call site in the current chain
      }

      // If didn't extend, then assume that the last call site (c.last()) exists in an initial method,
      // which is not called by anything.
      // ASSUMPTION: initial methods are never called by some intermediate method.
      if (extensions == null || extensions.size() == 0) {
        if (verbose >= 2) X.logs("  Keep same sliver (no extensions): %s", vestrBrief(ve));
        newAbs.Sv.add(ve);
      }
      else {
        for (Quad q : extensions) {
          CtxtVar newve = new CtxtVar(ve.v, ve.e.append(q));
          newAbs.Sv.add(newve);
          if (verbose >= 2) X.logs("  New sliver: %s", vestrBrief(newve));
        }
      }
    }

    X.logs("Refinement: |Sa| = %s (old: %s), |Sv| = %s (old: %s)",
        newAbs.Sa.size(), abs.Sa.size(),
        newAbs.Sv.size(), abs.Sv.size());
    X.logs("LENGTH(newAbs): %s", lengthHistogram(newAbs.Sa));

    abs = newAbs;
  }

  void addSuffixesToDomC(Ctxt c) {
    for (int i = 0; i <= c.length(); i++)
      domC.getOrAdd(c.suffix(i));
  }

  // Step (COMPUTE_C): Use current abstraction (Sa,Sv) to populate domains and relations
  void computeC() {
    X.logs("computeC()");

    // Put all suffixes into domC
    {
      domC.clear();
      for (Ctxt a : abs.Sa) addSuffixesToDomC(a);
      for (CtxtVar cv : abs.Sv) addSuffixesToDomC(cv.e);
      domC.save();
      X.logs("Converted %s into %s domain C elements", abs, domC.size());
    }

    // AH
    {
      relAH.zero();
      Set<Quad> inSomeContext = new HashSet<Quad>(); // set of heads of some context
      for (Ctxt a : abs.Sa) {
        relAH.add(a, a.head());
        inSomeContext.add(a.head());
      }
      for (Quad h : hSet) {
        if (!inSomeContext.contains(h))
          relAH.add(emptyCtxt, h);
      }
      relAH.save();
    }

    // CfromIC
    if (!useObjectSensitivity) {
      relCfromIC.zero();
      for (Ctxt c : domC) {
        if (c.length() == 0) {
          for (Quad i : iSet) // Try all possible call sites
            relCfromIC.add(extendC(i, c), i, c);
        }
        else if (!isAlloc(c.head())) {
          for (Quad i : i2i.get(c.head())) // Only try call sites that can follow
            relCfromIC.add(extendC(i, c), i, c);
        }
      }
      relCfromIC.save();
    }

    // AfromHC
    {
      relAfromHC.zero();
      for (Ctxt c : domC) { // For each context...
        if (c.length() == 0) {
          for (Quad h : hSet) // Try all possible allocation sites next
            relAfromHC.add(extendA(h, c), h, c);
        }
        else if (useObjectSensitivity) {
          for (Quad h : h2h.get(c.head())) // Only try allocation sites that can follow
            relAfromHC.add(extendA(h, c), h, c);
        }
        else if (!useObjectSensitivity && !isAlloc(c.head())) { // (consider only contexts)
          for (Quad h : i2h.get(c.head())) // Only try allocation sites that can follow
            relAfromHC.add(extendA(h, c), h, c);
        }
      }
      relAfromHC.save();
    }

    // EfromVC
    {
      relEfromVC.zero();
      for (Ctxt c : domC) { // For each context...
        if (c.length() == 0) {
          for (Register v : vSet) // Try all variables
            relEfromVC.add(extendV(v, c), v, c);
        }
        else if (useObjectSensitivity) {
          for (Register v : vSet) // Try all variables (FUTURE: prune)
            relEfromVC.add(extendV(v, c), v, c);
        }
        else if (!useObjectSensitivity && !isAlloc(c.head())) { // (consider only contexts)
          for (Register v : vSet) // Try all variables (FUTURE: prune)
            relEfromVC.add(extendV(v, c), v, c);
        }
      }
      relEfromVC.save();
    }
  }

  Ctxt extendC(Quad q, Ctxt c) {
    // Create d = [q, c] but truncate to length k, where k is the maximum such that d is a valid context
    for (int k = c.length()+1; k >= 0; k--) { // Try to extend
      Ctxt d = c.prepend(q, k);
      if (domC.indexOf(d) != -1)
        return d;
    }
    throw new RuntimeException("At least emptyCtxt should be in domC");
  }
  Ctxt extendA(Quad h, Ctxt c) {
    for (int k = c.length()+1; k >= 0; k--) {
      Ctxt a = c.prepend(h, k);
      if (abs.Sa.contains(a))
        return a;
    }
    return emptyCtxt;
  }
  CtxtVar extendV(Register v, Ctxt c) {
    for (int k = c.length(); k >= 0; k--) {
      Ctxt e = c.prefix(k);
      CtxtVar ve = new CtxtVar(v, e);
      if (abs.Sv.contains(ve))
        return ve;
    }
    throw new RuntimeException("Variable (without context) not in Sv");
  }

  Histogram lengthHistogram(Set<Ctxt> slivers) {
    Histogram hist = new Histogram();
    for (Ctxt c : slivers)
      hist.counts[c.length()]++;
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
