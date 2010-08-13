/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

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
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.snapshot.Execution;
import chord.analyses.snapshot.StatFig;
import chord.bddbddb.Rel.PairIterable;
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
import chord.project.Project;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ChordRuntimeException;
import chord.util.tuple.object.Pair;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

/**
 * Performs a refinement-based pointer analysis.
 * At the end, computes for a set of variables of interest,
 * the set of abstract objects.
 *
 * An abstract object is a sliver: [h, i_1, ... i_k],
 * where h is an allocation site and i_1, ... i_k is the context (a chain of call sites).
 *
 * An abstraction is specified by a set of slivers.
 * Note that importantly, we don't have to have uniform k values at all.
 *
 * Example:
 *
 * CI1 --[CICM]--+--> CM1 --+--[MI]--> CI3
 *               |          |
 * CI2 --[CICM]--+          +--[MH]--> CH1 --[CFC]--> CH2 <--[CVC]-- V1
 *
 * Input: given a set of variables we're interested in.
 * Output: for each variable v, a set of slivers that can influence the value of v.
 *
 * 0) Initialize C, call 0-CFA to get in CFC, CICM.
 * Iterate:
 *   1) Follow CVC pointers forward once and CFC pointers backwards to find the *influential slivers*.
 *   Choose a subset of these slivers which are within a fixed number of hops from something that a query variable points to;
 *   these are the ones we want to refine.
 *   Terminology: a hint is the *influencing slivers* of a particular query e.
 *   If the influence set is small enough, then we break out.
 *   2) Given the chosen influential slivers, follow MH, CICM, and MI
 *   pointers backwards, splitting these slivers to produce refined slivers.
 *   3) Compute C,CC,CI,CH from these refined slivers to reflect the new changes.
 *   4) Call k-CFA to obtain CFC and CICM.
 * 5) Set the heap abstraction to be the influential slivers and run the
 * flow-sensitive, context-sensitive full program analysis.
 *
 * MH, MI: fixed in advance
 * C --(0)--> CFC,CICM --(1,2,3)--> refinedSlivers --(4)--> C,CC,CI,CH --(5)--> CFC,CICM
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
  producedNames = { "C", "CC", "CH", "CI", "epsilonV", "epsilonM", "kcfaSenM", "kobjSenM" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis {
  private Execution X;

  static final String zcfaTaskName = "cspa-0cfa-sliver-dlog";
  static final String kcfaTaskName = "cspa-kcfa-sliver-dlog";

  public static final int CTXTINS = 0;  // abbr ci; must be 0
  public static final int KCFASEN = 2;  // abbr cs

  private jq_Method mainMeth;
  private boolean[] isCtxtSenV;   // indexed by domV
  private int[] methKind;         // indexed by domM

  // Immutable inputs
  private DomV domV;
  private DomM domM;
  private DomI domI;
  private DomH domH;
  private DomE domE;
	private ProgramRel relEV;

  // Mutable inputs
  private ProgramRel relCVC;
  private ProgramRel relCFC;
  private ProgramRel relCICM;

  // Outputs
  private DomC domC;
  private ProgramRel relCC;
  private ProgramRel relCH;
  private ProgramRel relCI;
  private ProgramRel relKcfaSenM;
  private ProgramRel relKobjSenM;
  private ProgramRel relEpsilonM;
  private ProgramRel relEpsilonV;

  // Canonical
  Quad _H;
  Register _V;

  //final Quad glob = null;
  //final Ctxt globCtxt = new Ctxt(new Quad[] {glob});
  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);

  // Options
  int verbose;
  int maxIters;
  int maxHintSize;
  int initRefinementRadius;

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  HashMap<Quad,List<Quad>> i2h = new HashMap<Quad,List<Quad>>(); // i -> list of allocation sites that can be prepended to i
  HashMap<Quad,List<Quad>> i2i = new HashMap<Quad,List<Quad>>(); // i -> list of call sites that can be prepended to i

  // Current state
  Set<Ctxt> chosenInfSlivers = new HashSet<Ctxt>(); // Slivers backward reachable (truncated at refinementRadius)
  Set<Ctxt> infSlivers = new HashSet<Ctxt>(); // Slivers backward reachable (no truncation)
  Set<Ctxt> refinedSlivers = new HashSet<Ctxt>(); // InfSlivers extended even further (used to populate domain C)
  Map<Quad,Set<Ctxt>> e2hints = new HashMap<Quad,Set<Ctxt>>();
  int refinementRadius; // Maximum number of hops that any candidate is away from a pivot

  // Determine the queries in client code (for thread-escape)
  public void computedExcludedClasses() {
    String[] checkExcludedPrefixes = Config.toArray(Config.checkExcludeStr);
    Program program = Program.getProgram();
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
  HashSet<Quad> queryE = new HashSet<Quad>(); // Set of all queries that we should answer

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

  Register e2v(Quad e) {
    for (Register v : sel0(_V, relEV, e)) return v;
    return null;
  }
  //String hstr(Quad h) { return h == glob ? "glob" : String.format("%s[%s]", domH.indexOf(h), h.toVerboseStr()); }
  String hstr(Quad h) { return String.format("%s[%s]", domH.indexOf(h), h.toVerboseStr()); }
  String istr(Quad i) { return String.format("%s[%s]", domI.indexOf(i), i.toVerboseStr()); }
  String estr(Quad e) { return String.format("%s[%s]", domE.indexOf(e), e.toVerboseStr()); }
  String vstr(Register v) { return String.format("%s[%s]", domV.indexOf(v), v); }
  String cstr(Ctxt c) {
    StringBuilder buf = new StringBuilder();
    buf.append(domC.indexOf(c));
    buf.append('{');
    for (int i = 0; i < c.length(); i++) {
      if (i > 0) buf.append(" | ");
      Quad q = c.get(i);
      buf.append(isAlloc(q) ? hstr(q) : istr(q));
    }
    buf.append('}');
    return buf.toString();
  }

  //boolean isAlloc(Quad q) { return q == glob || domH.indexOf(q) != -1; }
  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }

  ////////////////////////////////////////////////////////////

  private void init() {
    X = Execution.v("hints");  

    // Immutable inputs
    domV = (DomV) Project.getTrgt("V"); Project.runTask(domV);
    domM = (DomM) Project.getTrgt("M"); Project.runTask(domM);
    domI = (DomI) Project.getTrgt("I"); Project.runTask(domI);
    domH = (DomH) Project.getTrgt("H"); Project.runTask(domH);
    domE = (DomE) Project.getTrgt("E"); Project.runTask(domE);
		relEV = (ProgramRel) Project.getTrgt("EV"); Project.runTask(relEV); relEV.load();

    // Mutable inputs
		relCVC = (ProgramRel) Project.getTrgt("CVC");
		relCFC = (ProgramRel) Project.getTrgt("CFC");
		relCICM = (ProgramRel) Project.getTrgt("CICM");

    // Output
    domC = (DomC) Project.getTrgt("C");
    relCC = (ProgramRel) Project.getTrgt("CC");
    relCH = (ProgramRel) Project.getTrgt("CH");
    relCI = (ProgramRel) Project.getTrgt("CI");
    relKcfaSenM = (ProgramRel) Project.getTrgt("kcfaSenM");
    relKobjSenM = (ProgramRel) Project.getTrgt("kobjSenM");
    relEpsilonM = (ProgramRel) Project.getTrgt("epsilonM");
    relEpsilonV = (ProgramRel) Project.getTrgt("epsilonV");

    mainMeth = Program.getProgram().getMainMethod();

    int numV = domV.size();
    int numM = domM.size();
    int numH = domH.size();
    int numI = domI.size();

    // Set the context-sensitivity of various methods
    methKind = new int[numM];
    for (int mIdx = 0; mIdx < numM; mIdx++) {
      jq_Method mVal = domM.get(mIdx);
      methKind[mIdx] = getCtxtKind(mVal);
    }

    // Based on context-sensitivity of methods, set the context-sensitivity of variables inside the method
    isCtxtSenV = new boolean[numV];
    for (int mIdx = 0; mIdx < numM; mIdx++) {
      if (methKind[mIdx] != CTXTINS) {
        jq_Method m = domM.get(mIdx);
        ControlFlowGraph cfg = m.getCFG();
        RegisterFactory rf = cfg.getRegisterFactory();
        for (Object o : rf) {
          Register v = (Register) o;
          if (v.getType().isReferenceType()) {
            int vIdx = domV.indexOf(v);
            // locals unused by any quad in cfg are not in domain V
            if (vIdx != -1)
              isCtxtSenV[vIdx] = true;
          }
        }
      }
    }
    validate();

    // Mark which methods and variables to be context sensitive
    relEpsilonM.zero();
    relKcfaSenM.zero();
    relKobjSenM.zero();
    for (int mIdx = 0; mIdx < numM; mIdx++) {
      int kind = methKind[mIdx];
      switch (kind) {
        case CTXTINS: relEpsilonM.add(mIdx); break;
        case KCFASEN: relKcfaSenM.add(mIdx); break;
        default: assert false;
      }
    }
    relEpsilonM.save();
    relKcfaSenM.save();
    relKobjSenM.save();
		relEpsilonV.zero();
		for (int v = 0; v < numV; v++)
			if (!isCtxtSenV[v]) relEpsilonV.add(v);
		relEpsilonV.save();

    _H = (Quad)domH.get(1);
    _V = domV.get(1);

    // Compute which queries we should answer in the whole program
    computedExcludedClasses();
    for (Quad e : domE) if (!computeStatementIsExcluded(e) && e2v(e) != null) queryE.add(e);
    for (Quad e : queryE) e2hints.put(e, new HashSet<Ctxt>());
  }

  void finish() {
    relEV.close();
    X.finish(null);
  }
  
  private void validate() {
    // check that the main jq_Method and each class initializer method
    // and each method without a body is not asked to be analyzed
    // context sensitively.
    int numM = domM.size();
    for (int m = 0; m < numM; m++) {
      int kind = methKind[m];
      if (kind != CTXTINS) {
        jq_Method meth = domM.get(m);
        assert (meth != mainMeth);
        assert (!(meth instanceof jq_ClassInitializer));
      }
    }
    // check that each variable in a context insensitive method is
    // not asked to be treated context sensitively.
    int numV = domV.size();
    for (int v = 0; v < numV; v++) {
      if (isCtxtSenV[v]) {
        Register var = domV.get(v);
        jq_Method meth = domV.getMethod(var);
        int m = domM.indexOf(meth);
        int kind = methKind[m];
        assert (kind != CTXTINS);
      }
    }
  }

  private int getCtxtKind(jq_Method m) {
    return m == mainMeth || m instanceof jq_ClassInitializer || m.isAbstract() ? CTXTINS : KCFASEN;
  }

  public void run() {
    init();

    this.verbose = X.getIntArg("verbose", 0);
    this.maxIters = X.getIntArg("maxIters", 1);
    this.maxHintSize = X.getIntArg("maxHintSize", 10);
    this.initRefinementRadius = X.getIntArg("initRefinementRadius", 2);

    java.util.HashMap<Object,Object> options = new java.util.LinkedHashMap<Object,Object>();
    options.put("version", 1);
    options.put("program", System.getProperty("chord.work.dir"));
    options.put("maxIters", maxIters);
    options.put("maxHintSize", maxHintSize);
    options.put("initRefinementRadius", initRefinementRadius);
    X.writeMap("options.map", options);

    init0CFA();

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      refinementRadius = initRefinementRadius + iter;

      if (computeInfSlivers()) { // Step (1)
        X.logs("Found satisfactory chosenInfSlivers, exiting...");
        break;
      }
      if (iter == maxIters) {
        X.logs("Reached maximum number of iterations, exiting...");
        break;
      }

      relCICM.load();
      refineSlivers(); // Step (2)
      computeC(); // Step (3)
      relCICM.close();

			Project.resetTaskDone(zcfaTaskName);
			Project.resetTaskDone(kcfaTaskName);
      Project.runTask(kcfaTaskName); // Step (4)
    }

    finish();
  }

  // Step (0)
  void init0CFA() {
    // Construct C, CH (trivially)
    domC.clear();
    domC.getOrAdd(emptyCtxt); // Empty context
    Ctxt[] h2c = new Ctxt[domH.size()];
    for (int h = 1; h < domH.size(); h++) {
      // Create a separate context for each reachable allocation site
      Ctxt c = emptyCtxt.append((Quad)domH.get(h));
      domC.getOrAdd(c);
      h2c[h] = c;
    }
    domC.save();

    relCH.zero();
    for (int h = 1; h < domH.size(); h++) {
      // Create a separate context for each reachable allocation site
      relCH.add(h2c[h], domH.get(h));
    }
    relCH.save();

    Project.runTask(zcfaTaskName); 

    // Used fixed call graph to determine:
    //  - set of reachable H and I.
    //  - information for building slivers (overapproximation suffices)
    {
      ProgramRel relItoH = (ProgramRel)Project.getTrgt("ItoH"); relItoH.load();
      PairIterable<Quad,Quad> result = relItoH.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad i = pair.val0;
        Quad h = pair.val1;
        iSet.add(i);
        hSet.add(h);
        List<Quad> l = i2h.get(i);
        if (l == null) i2h.put(i, l = new ArrayList<Quad>());
        l.add(h);
      }
      relItoH.close();
    }
    {
      ProgramRel relItoI = (ProgramRel)Project.getTrgt("ItoI"); relItoI.load();
      PairIterable<Quad,Quad> result = relItoI.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) {
        Quad i = pair.val0;
        Quad j = pair.val1;
        iSet.add(i);
        iSet.add(j);
        List<Quad> l = i2i.get(i);
        if (l == null) i2i.put(i, l = new ArrayList<Quad>());
        l.add(j);
      }
      relItoI.close();
    }
    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s", hSet.size(), iSet.size());
  }

  // Step (1): compute influential slivers
  boolean computeInfSlivers() {
    X.logs("computeInfSlivers()");
    relCVC.load();
    relCFC.load();

    chosenInfSlivers.clear(); // Truncate at refinementRadius
    infSlivers.clear(); // No truncation

    // When tracing back...
    Map<Ctxt,Set<Ctxt>> c2chosenHint = new HashMap<Ctxt,Set<Ctxt>>();
    Map<Ctxt,Set<Ctxt>> c2hint = new HashMap<Ctxt,Set<Ctxt>>();

    int numDone = 0;
    for (Quad e : queryE) { // For each query...
      Set<Ctxt> chosenHint = new HashSet<Ctxt>(); // Truncate at refinementRadius
      Set<Ctxt> hint = e2hints.get(e); // No truncation
      hint.clear();

      Register v = e2v(e); // ...which accesses a field of v

      // Find all slivers that v could point to...
      RelView view = relCVC.getView();
      view.delete(0);
      view.selectAndDelete(1, v);
      Iterable<Ctxt> result = view.getAry1ValTuples();
      List<Ctxt> pointsTo = new ArrayList<Ctxt>();
      for (Ctxt c : result) { // For each such sliver c...
        pointsTo.add(c);
        if (c.length() == 0)
          X.errors("Invariant broken: Query %s (variable %s) points to glob!", estr(e), vstr(v));
      }
      view.free();

      for (Ctxt c : pointsTo) {  
        // Trace back from each pivot c (use cache if possible)
        Set<Ctxt> myHint = c2hint.get(c);
        if (myHint == null) {
          c2hint.put(c, myHint = new HashSet<Ctxt>());
          traceBack(myHint, c, Integer.MAX_VALUE);
        }
        hint.addAll(myHint);
      }
      String distHist = "-";
      //String distHist = traceBack(hint, pointsTo, Integer.MAX_VALUE);
      infSlivers.addAll(hint);

      if (hint.size() <= maxHintSize) {
        // Done - no more refinement
        numDone++;
        X.logs("QUERY DONE: [%s] |hint| = %s <= %s (|pts|=%s): %s",
            distHist, hint.size(), maxHintSize, pointsTo.size(), estr(e));
      }
      else {
        // Choose some influential slivers to refine
        for (Ctxt c : pointsTo) {
          Set<Ctxt> myChosenHint = c2chosenHint.get(c);
          if (myChosenHint == null) {
            c2chosenHint.put(c, myChosenHint = new HashSet<Ctxt>());
            traceBack(myChosenHint, c, refinementRadius);
          }
          chosenHint.addAll(myChosenHint);
        }
        //traceBack(chosenHint, pointsTo, refinementRadius);
        chosenInfSlivers.addAll(chosenHint);

        X.logs("QUERY REFINE: [%s] chose %s/%s as hint [radius<=%s] (|pts|=%s): %s",
            distHist,
            chosenHint.size(), hint.size(), refinementRadius,
            pointsTo.size(), estr(e));
      }
    }

    X.logs("TOTAL: %s/%s queries are done (with |hint| <= %s)", numDone, queryE.size(), maxHintSize);
    X.logs("TOTAL: chose %s/%s influential slivers to refine across %s queries", chosenInfSlivers.size(), infSlivers.size(), queryE.size());

    // Print histogram of slivers
    X.logs("LENGTH(chosenInfSlivers): %s", lengthHistogram(chosenInfSlivers));
    X.logs("LENGTH(infSlivers): %s", lengthHistogram(infSlivers));

    relCVC.close();
    relCFC.close();

    return numDone == queryE.size();
  }

  String traceBack(Set<Ctxt> visited, Ctxt c, int maxDist) {
    if (verbose >= 2) X.logs("traceBack from %s", cstr(c));
    return traceBack(visited, Collections.singletonList(c), maxDist);
  }
  String traceBack(Set<Ctxt> visited, List<Ctxt> cs, int maxDist) {
    TObjectIntHashMap<Ctxt> dists = new TObjectIntHashMap<Ctxt>();
    LinkedList<Ctxt> queue = new LinkedList<Ctxt>();
    for (Ctxt c : cs) {
      dists.put(c, 0);
      queue.addLast(c);
      visited.add(c);
    }
    // Perform BFS
    while (queue.size() > 0) {
      Ctxt c = queue.removeFirst();
      int cdist = dists.get(c);

      RelView view = relCFC.getView();
      view.delete(1);
      view.selectAndDelete(2, c);
      Iterable<Ctxt> result = view.getAry1ValTuples();
      for (Ctxt b : result) {
        if (verbose >= 3 && maxDist == Integer.MAX_VALUE)
          X.logs("EDGE %s %s", domC.indexOf(b), domC.indexOf(c));
        if (visited.contains(b)) continue;
        if (b.length() == 0)
          X.errors("Invariant broken: glob can reach %s", cstr(c));
        visited.add(b);
        int bdist = cdist+1;
        dists.put(b, bdist);
        if (bdist < maxDist)
          queue.addLast(b);
      }
    }

    return distHistogram(dists);
  }

  // Step (3): Refine the chosenInfSlivers
  void refineSlivers() {
    X.logs("refineSlivers()");
    // Refine only chosenInfSlivers, not infSlivers
    int numOldSlivers = refinedSlivers.size();
    refinedSlivers.clear();

    for (Ctxt c : infSlivers) { // For each candidate...
      assert c.length() == 0 || isAlloc(c.head());
      if (true || !chosenInfSlivers.contains(c)) { // Don't refine TMP
        refinedSlivers.add(c);
        continue;
      }

      jq_Method m = c.head().getMethod(); // Containing method

      // See which call sites can call this method
      RelView view = relCICM.getView();
      view.delete(0);
      view.selectAndDelete(2, c.tail());
      view.selectAndDelete(3, m);
      Iterable<Quad> result = view.getAry1ValTuples();
      if (verbose >= 1) X.logs("Refining sliver %s:", cstr(c));
      for(Quad i : result) { // i can call method m in context c
        refinedSlivers.add(c.append(i));
        if (verbose >= 2) X.logs("  New sliver: %s", cstr(c.append(i)));
      }
      view.free();
    }

    X.logs("Refinement yielded %s refinedSlivers (from %s) using %s/%s chosenInfSlivers", refinedSlivers.size(), numOldSlivers, chosenInfSlivers.size(), infSlivers.size());
    X.logs("LENGTH(refinedSlivers): %s", lengthHistogram(refinedSlivers));
  }

  void test0CFA() {
    X.logs("Installing 0-CFA into C, CH, CC, CI");
    // Construct C, CH (trivially)
    domC.clear();
    domC.getOrAdd(emptyCtxt); // Empty context
    Ctxt[] h2c = new Ctxt[domH.size()];
    for (int h = 1; h < domH.size(); h++) {
      // Create a separate context for each reachable allocation site
      Ctxt c = emptyCtxt.append((Quad)domH.get(h));
      domC.getOrAdd(c);
      h2c[h] = c;
    }
    domC.save();

    relCH.zero();
    for (int h = 1; h < domH.size(); h++) {
      // Create a separate context for each reachable allocation site
      relCH.add(h2c[h], domH.get(h));
    }
    relCH.save();

    // Use k-CFA code to do 0-CFA (sanity check)
    relCI.zero();
    for (int i = 0; i < domI.size(); i++)
      relCI.add(emptyCtxt, domI.get(i));
    relCI.save();
    relCC.zero();
    for (Ctxt c : domC) relCC.add(emptyCtxt, c);
    relCC.save();
  }

  // Step (4): Use refinedSlivers to populate C, CC, CI, CH
  void computeC() {
    //test0CFA();
    //if (true) return;

    X.logs("computeC()");

    // DomC will consist of all suffixes of all refinedSlivers
    {
      domC.clear();
      //domC.getOrAdd(globCtxt); // plus a special glob
      for (Ctxt sliver : refinedSlivers) {
        for (int k = 0; k <= sliver.length(); k++)
          domC.getOrAdd(sliver.suffix(k));
      }
      domC.save();
      X.logs("Converted %s refinedSlivers into %s domain C elements", refinedSlivers.size(), domC.size());
    }

    /*{
      X.logs("Filling C with 0-CFA");
      domC.clear();
      domC.getOrAdd(emptyCtxt); // Empty context
      for (Quad h : hSet)
        domC.getOrAdd(emptyCtxt.append(h));
      domC.save();
    }*/

    Set<Quad> inSomeContext = new HashSet<Quad>(); // set of heads of some context
    for (Ctxt c : domC)
      if (c.length() > 0) inSomeContext.add(c.head());

    // Extract head
    X.logs("Building CI and CH...");
		relCI.zero();
		relCH.zero();
    for (Ctxt c : domC) {
      if (c.length() == 0) { // Empty context corresponds to any allocation/call site that's not represented by a sliver.
        for (Quad h : hSet) if (!inSomeContext.contains(h)) relCH.add(c, h);
        for (Quad i : iSet) if (!inSomeContext.contains(i)) relCI.add(c, i);
      }
      else {
        Quad q = c.head();
        if (isAlloc(q)) relCH.add(c, q);
        else relCI.add(c, q);
      }
    }
		relCI.save();
		relCH.save();

    X.logs("Building CC...");
		relCC.zero();
    for (Ctxt c : domC) { // For each context (path of call sites), try extending
      // To be absolutely safe, we would consider prepending all possible sites.
      // But this would make the CC relation too large.
      // So we have to anticipate what kind of prepending we'll encounter.
      if (c.length() == 0) {
        // Can extend the empty context to any singleton or empty context
        for (Ctxt d : domC) {
          if (d.length() <= 1)
            relCC.add(c, d);
        }
        continue;
      }

      if (isAlloc(c.head())) continue;

      // Extend
      Quad i = c.head();
      for (Quad h : i2h.get(i)) extend(h, c);
      for (Quad j : i2i.get(i)) extend(j, c);

      // WARNING
      // We are using old information (CICM) to determine the posible extensions.
      // We must assume that the new CICM can only get smaller because we are making finer distinctions.

      // Extend c with call sites
      /*{
        RelView view = relCICM.getView();
        view.selectAndDelete(0, c.tail());
        view.selectAndDelete(1, c.head());
        view.delete(2);
        Iterable<jq_Method> result = view.getAry1ValTuples();
        for(jq_Method m : result) { // For each method that we can invoke from c...
          // For all call sites in this method...
          RelView viewMI = relMI.getView();
          viewMI.selectAndDelete(0, m);
          Iterable<Quad> resultMI = viewMI.getAry1ValTuples();
          for (Quad i : resultMI) extend(i, c); // Can prepend i to c
          viewMI.free();

        }
        view.free();
      }

      // Extend c with allocation sites
      {
        RelView view = relCICM.getView();
        view.delete(0);
        view.delete(1);
        view.selectAndDelete(2, c);
        Iterable<jq_Method> result = view.getAry1ValTuples();
        for(jq_Method m : result) { // For each method that we can be in (in context c)
          // For all allocation sites in this method...
          RelView viewMH = relMH.getView();
          viewMH.selectAndDelete(0, m);
          Iterable<Quad> resultMH = viewMH.getAry1ValTuples();
          for (Quad h : resultMH) extend(h, c);
          viewMH.free();
        }
        view.free();
      }*/
    }

		relCC.save();
  }

  void extend(Quad q, Ctxt c) {
    // Create d = [q, c] but truncate to length k, where k is the maximum such that d is a valid context
    for (int k = c.length()+1; k >= 0; k--) { // Try to extend
      Ctxt d = c.prepend(q, k);
      if (domC.indexOf(d) != -1) {
        relCC.add(c, d);
        break;
      }
      //if (k == 0 && isAlloc(q)) // This allocation site goes in the glob
        //relCC.add(c, globCtxt);
    }
  }

  String lengthHistogram(Set<Ctxt> slivers) {
    int[] counts = new int[1000];
    for (Ctxt c : slivers)
      counts[c.length()]++;
    return formatCounts(counts);
  }

  String distHistogram(TObjectIntHashMap<Ctxt> dists) {
    final int[] counts = new int[1000];
    dists.forEachEntry(new TObjectIntProcedure<Ctxt>() {
      public boolean execute(Ctxt c, int dist) {
        counts[dist]++;
        return true;
      }
    });
    return formatCounts(counts);
  }

  String formatCounts(int[] counts) {
    StringBuilder buf = new StringBuilder();
    for (int n = 0; n < counts.length; n++) {
      if (counts[n] == 0) continue;
      if (buf.length() > 0) buf.append(" ");
      buf.append(n+":"+counts[n]);
    }
    return '['+buf.toString()+']';
  }
}
