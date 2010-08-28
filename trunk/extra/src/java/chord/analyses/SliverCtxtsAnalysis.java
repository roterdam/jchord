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
import joeq.Class.jq_Field;
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
import chord.util.IndexMap;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ChordRuntimeException;
import chord.util.tuple.object.Pair;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

/**
 * Performs a refinement-based pointer analysis.
 *
 * A context is a sequence of call sites [i_1, ..., i_k].
 * An abstract object is a sliver: [h, i_1, ... i_k], where h is an allocation site.
 *
 * An abstraction is specified:
 *   - A set of contexts (closed under suffixes) [refinedContexts]
 *   - A set of slivers (supported by the contexts) [refinedSlivers]
 *
 * Input: given a set of variables we're interested in.
 * Output: for each variable v, a small set of slivers that can reach an object
 * that v points with respect to the given abstraction.
 *
 * Example:
 *
 * CI1 --[CICM]--+--> CM1 --+
 *               |          |
 * CI2 --[CICM]--+          +--[MH]--> CH1 --[CFC]--> CH2 <--[CVC]-- V1
 *
 * (INIT) Initialize C, call 0-CFA to get CVC,CFC,CICM and ItoI,ItoH and reachableH,reachableI.
 * Iterate:
 *   (PRUNE_CONTEXT) Use congruence tests to reduce the number of contexts.
 *   (PRUNE_SLIVER) Follow CVC pointers forward once and CFC pointers backwards to find the *influential slivers* [infSlivers].
 *   Choose a subset of these slivers which are within a fixed number of hops from something that a query variable points to;
 *   these are the ones we want to refine [chosenInfSlivers].
 *   Terminology: a hint is the *influencing slivers* of a particular query e.
 *   If the influence set is small enough, then we break out.
 *   (REFINE) Given the chosen influential slivers, follow MH, CICM, and MI
 *   pointers backwards, splitting these slivers to produce refined slivers, expanding contexts if necessary
 *   The refined slivers determines our abstraction [refinedSlivers,refinedContexts].
 *   (COMPUTE_C) Compute C,CC,CI,CH from our computed abstraction.
 *   (ANALYSIS) Call k-CFA to obtain CVC,CFC,CICM.
 * (FULL) Set the heap abstraction to be the influential slivers and run the
 * flow-sensitive, context-sensitive full program analysis.
 *
 * MH, MI: fixed in advance
 * C,CC,CI,CH --(INIT)--> ItoI,ItoH,CVC,CFC,CICM --(PRUNE_CONTEXT,PRUNE_SLIVER,REFINE)--> refinedContexts,refinedSlivers
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
  produces = { "C", "CC", "CH", "CI" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis {
  private Execution X;

  static final String kcfaTaskName = "cspa-kcfa-sliver-dlog";

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

  // Canonical
  Quad _H;
  Register _V;

  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);

  // Options
  int verbose;
  int maxIters;
  int maxHintSize;
  int initRefinementRadius;

  IndexMap<Ctxt> globalC = new IndexMap<Ctxt>(); // dom C will change over time, but keep a consistent indexing for comparison

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  HashMap<Quad,List<Quad>> i2h = new HashMap<Quad,List<Quad>>(); // i -> list of allocation sites that can be prepended to i
  HashMap<Quad,List<Quad>> i2i = new HashMap<Quad,List<Quad>>(); // i -> list of call sites that can be prepended to i
  HashMap<Quad,List<Quad>> rev_i2i = new HashMap<Quad,List<Quad>>();
  HashMap<jq_Method,List<Quad>> callees = new HashMap<jq_Method,List<Quad>>(); // m -> list of call sites (reverse of IM)

  // Current state
  Set<Ctxt> chosenInfSlivers = new HashSet<Ctxt>(); // Slivers backward reachable (truncated at refinementRadius)
  Set<Ctxt> infSlivers = new HashSet<Ctxt>(); // Slivers backward reachable (no truncation)
  Set<Ctxt> refinedContexts = new HashSet<Ctxt>(); // Specifies abstraction
  Set<Ctxt> refinedSlivers = new HashSet<Ctxt>(); // Specifies abstraction
  Map<Quad,Set<Ctxt>> e2hints = new HashMap<Quad,Set<Ctxt>>(); // infSlivers
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
  String cvstr(Ctxt c) { return cstr(c, false)+(c.length() > 0 ? "_"+c.head().toByteLocStr() : ""); }

  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }

  ////////////////////////////////////////////////////////////

  private void init() {
    X = Execution.v("hints");  
    X.addSaveFiles("hints.txt", "hints-str.txt");

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

    _H = (Quad)domH.get(1);
    _V = domV.get(1);

    // Compute which queries we should answer in the whole program
    String focus = X.getStringArg("eFocus", null);
    if (focus != null) {
      for (String e : focus.split(","))
        queryE.add(domE.get(Integer.parseInt(e)));
    }
    else {
      computedExcludedClasses();
      for (Quad e : domE)
        if (!computeStatementIsExcluded(e) && e2v(e) != null) queryE.add(e);
    }

    for (Quad e : queryE) e2hints.put(e, new HashSet<Ctxt>());
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

    java.util.HashMap<Object,Object> options = new java.util.LinkedHashMap<Object,Object>();
    options.put("version", 1);
    options.put("program", System.getProperty("chord.work.dir"));
    options.put("maxIters", maxIters);
    options.put("maxHintSize", maxHintSize);
    options.put("initRefinementRadius", initRefinementRadius);
    X.writeMap("options.map", options);

    init0CFA(); // STEP (INIT)

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      backupRelations(iter);
      refinementRadius = initRefinementRadius + (iter-1);

      pruneContexts(); // STEP (PRUNE_CONTEXT)
      boolean done = computeInfSlivers(); // STEP (PRUNE_SLIVER)
      outputHint(iter);

      if (done) {
        X.logs("Found satisfactory chosenInfSlivers, exiting...");
        break;
      }
      if (iter == maxIters) {
        X.logs("Reached maximum number of iterations, exiting...");
        break;
      }

      relCICM.load();
      refineSlivers(); // Step (REFINE)
      computeC(); // Step (COMPUTE_C)
      relCICM.close();

			Project.resetTrgtDone(domC); // Make everything that depends on domC undone
			Project.setTaskDone(this); // We are generating all this stuff, so mark it as done...
			Project.setTrgtDone(domC);
			Project.setTrgtDone(relCI);
			Project.setTrgtDone(relCH);
			Project.setTrgtDone(relCC);
      Project.runTask(kcfaTaskName); // Step (ANALYSIS)
    }

    finish();
  }

  void backupRelations(int iter) {
    X.logs("backupRelations");
    try {
      String path = X.path(""+iter);
      new File(path).mkdir();
      domC.save(path, true);
      for (ProgramRel rel : new ProgramRel[] { relCC, relCI, relCH, relCVC, relCFC }) {
        rel.load();
        rel.print(path);
        //rel.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    PrintWriter out = OutDirUtils.newPrintWriter("globalC.txt");
    for (int i = 0; i < globalC.size(); i++)
      out.println(cvstr(globalC.get(i)));
    out.close();
  }

  // Step (INIT)
  void init0CFA() {
    // Construct C
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

    for (Ctxt c : domC) globalC.getOrAdd(c);

    // Construct CH
    relCH.zero();
    for (int h = 1; h < domH.size(); h++)
      relCH.add(h2c[h], domH.get(h)); // Create a separate context for each reachable allocation site
    relCH.save();

    // Construct CI
    relCI.zero();
    for (int i = 0; i < domI.size(); i++)
      relCI.add(emptyCtxt, domI.get(i));
    relCI.save();

    // Construct CC
    relCC.zero();
    for (Ctxt c : domC) relCC.add(emptyCtxt, c);
    relCC.save();

    Project.runTask(kcfaTaskName); 

    // Only consider reachable stuff
    {
      ProgramRel rel = (ProgramRel)Project.getTrgt("reachableH"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad h : result) hSet.add(h);
      rel.close();
    }
    {
      ProgramRel rel = (ProgramRel)Project.getTrgt("reachableI"); rel.load();
      Iterable<Quad> result = rel.getAry1ValTuples();
      for (Quad i : result) iSet.add(i);
      rel.close();
    }

    for (Quad i : iSet) {
      i2h.put(i, new ArrayList<Quad>());
      i2i.put(i, new ArrayList<Quad>());
      rev_i2i.put(i, new ArrayList<Quad>());
    }

    // Build callees
    {
      ProgramRel relIM = (ProgramRel)Project.getTrgt("IM"); relIM.load();
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
      ProgramRel relItoH = (ProgramRel)Project.getTrgt("ItoH"); relItoH.load();
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
      ProgramRel relItoI = (ProgramRel)Project.getTrgt("ItoI"); relItoI.load();
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

    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s", hSet.size(), iSet.size());
  }

  // Step (PRUNE_SLIVER)
  void pruneContexts() {
    // FUTURE WORK
  }

  // Step (PRUNE_SLIVER): compute influential slivers (and also choose subset for refinement)
  boolean computeInfSlivers() {
    X.logs("computeInfSlivers()");
    relCVC.load();
    relCFC.load();

    chosenInfSlivers.clear(); // Truncate at refinementRadius
    infSlivers.clear(); // No truncation

    // When tracing back...
    Map<Ctxt,Set<Ctxt>> c2chosenHint = new HashMap<Ctxt,Set<Ctxt>>();
    Map<Ctxt,Set<Ctxt>> c2hint = new HashMap<Ctxt,Set<Ctxt>>();
    Map<Ctxt,Histogram> c2hist = new HashMap<Ctxt,Histogram>();

    int numDone = 0;
    for (Quad e : queryE) { // For each query...
      Set<Ctxt> chosenHint = new HashSet<Ctxt>(); // Truncate at refinementRadius
      Set<Ctxt> hint = e2hints.get(e); // No truncation
      hint.clear();

      Register v = e2v(e); // ...which accesses a field of v

      // Find all slivers that v could point to... [pointsTo]
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

      Histogram hist = new Histogram(); // Note: overcounts
      for (Ctxt c : pointsTo) {  
        // Trace back from each pivot c (use cache if possible)
        Set<Ctxt> myHint = c2hint.get(c);
        if (myHint == null) {
          c2hint.put(c, myHint = new HashSet<Ctxt>());
          c2hist.put(c, traceBack(myHint, c, Integer.MAX_VALUE));
        }
        hint.addAll(myHint);
        hist.add(c2hist.get(c));
      }
      infSlivers.addAll(hint);

      if (hint.size() <= maxHintSize) {
        // Done - no more refinement
        numDone++;
        X.logs("QUERY DONE: %s |hint| = %s <= %s (|pts|=%s): %s",
            hist, hint.size(), maxHintSize, pointsTo.size(), estr(e));
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
        chosenInfSlivers.addAll(chosenHint);

        X.logs("QUERY REFINE: %s chose %s/%s as hint [radius<=%s] (|pts|=%s): %s",
            hist,
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

  void outputHint(int iter) {
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
            if (i == 0) sb.append(domH.indexOf(c.get(i)));
            else sb.append(" "+domI.indexOf(c.get(i)));
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
  }

  Histogram traceBack(Set<Ctxt> visited, Ctxt c, int maxDist) {
    if (verbose >= 2) X.logs("traceBack from %s", cstr(c));
    return traceBack(visited, Collections.singletonList(c), maxDist);
  }
  Histogram traceBack(Set<Ctxt> visited, List<Ctxt> cs, int maxDist) {
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
      //view.delete(1);
      view.selectAndDelete(2, c);
      PairIterable<Ctxt,jq_Field> result = view.getAry2ValTuples();
      for (Pair<Ctxt,jq_Field> pair : result) {
        Ctxt b = pair.val0;
        jq_Field f = pair.val1;
        if (verbose >= 3 && maxDist == Integer.MAX_VALUE)
          //X.logs("EDGE %s %s", globalC.getOrAdd(b), globalC.getOrAdd(c));
          X.logs("EDGE %s %s_%s %s_%s", f, globalC.getOrAdd(b), cstr(b, false), globalC.getOrAdd(c), cstr(c, false));
        if (visited.contains(b)) continue;
        if (b.length() == 0)
          X.errors("Invariant broken: glob can reach %s", cstr(c));
        visited.add(b);
        int bdist = cdist+1;
        dists.put(b, bdist);
        if (bdist < maxDist)
          queue.addLast(b);
      }

      /*RelView view = relCFC.getView();
      view.delete(1);
      view.selectAndDelete(2, c);
      Iterable<Ctxt> result = view.getAry1ValTuples();
      for (Ctxt b : result) {
        if (verbose >= 3 && maxDist == Integer.MAX_VALUE)
          //X.logs("EDGE %s %s", globalC.getOrAdd(b), globalC.getOrAdd(c));
          X.logs("EDGE %s_%s %s_%s", globalC.getOrAdd(b), cstr(b, false), globalC.getOrAdd(c), cstr(c, false));
        if (visited.contains(b)) continue;
        if (b.length() == 0)
          X.errors("Invariant broken: glob can reach %s", cstr(c));
        visited.add(b);
        int bdist = cdist+1;
        dists.put(b, bdist);
        if (bdist < maxDist)
          queue.addLast(b);
      }*/
    }

    return distHistogram(dists);
  }

  // Step (REFINE): Refine the chosenInfSlivers to produce refinedSlivers and refinedContexts
  void refineSlivers() {
    X.logs("refineSlivers()");
    // Refine only chosenInfSlivers, not infSlivers
    int oldNumSlivers = refinedSlivers.size();
    int oldNumContexts = refinedContexts.size();
    refinedSlivers.clear();
    // Note: don't clear refinedContexts

    for (Ctxt c : infSlivers) { // For each candidate...
      assert c.length() == 0 || isAlloc(c.head());

      if (true) { // TMP
        refinedSlivers.add(c);
        continue;
      }

      // Keep as is
      if (!chosenInfSlivers.contains(c)) {
        refinedSlivers.add(c);
        continue;
      }

      if (c.length() == 0) // Should not be trying to refine the glob
        throw new RuntimeException("We are trying to refine the glob - something went wrong!");

      if (verbose >= 1) X.logs("Refining sliver %s:", cstr(c));
      List<Quad> extensions = c.length() == 1 ?
        callees.get(c.head().getMethod()) : // Call sites that call the containing method of this allocation site (c.head())
        rev_i2i.get(c.last()); // Call sites that lead to the last call site in the current chain

      // If didn't extend, then assume that the last call site (c.last()) exists in an initial method,
      // which is not called by anything.
      // ASSUMPTION: initial methods are never called by some intermediate method.
      if (extensions == null || extensions.size() == 0) {
        if (verbose >= 2) X.logs("  Keep same sliver (no extensions): %s", cstr(c));
        refinedSlivers.add(c);
      }
      else {
        for (Quad i : extensions) {
          refinedSlivers.add(c.append(i));
          if (verbose >= 2) X.logs("  New sliver: %s", cstr(c.append(i)));
        }
      }
    }

    // Update refinedContexts to support refinedSlivers
    for (Ctxt c : refinedSlivers) {
      for (int i = 0; i < c.length(); i++)
        refinedContexts.add(c.suffix(i));
    }

    X.logs("Refinement yielded %s refinedSlivers (previous: %s), %s refinedContexts (previous: %s) using %s/%s chosenInfSlivers",
        refinedSlivers.size(), oldNumSlivers, refinedContexts.size(), oldNumContexts, chosenInfSlivers.size(), infSlivers.size());
    X.logs("LENGTH(refinedSlivers): %s", lengthHistogram(refinedSlivers));
  }

  // Step (COMPUTE_C): Use refinedSlivers,refinedContexts to populate C,CC,CI,CH
  void computeC() {
    X.logs("computeC()");

    // Populate dom C
    if (true) {
      domC.clear();
      domC.getOrAdd(emptyCtxt); // Empty context
      for (Ctxt c : refinedSlivers) domC.getOrAdd(c);
      for (Ctxt c : refinedContexts) domC.getOrAdd(c);
      domC.save();
      X.logs("Converted %s refinedSlivers into %s domain C elements", refinedSlivers.size(), domC.size());
    }
    else { // TMP
      // Temporary test - if do this, results shouldn't change
      X.logs("Filling C with 0-CFA");
      domC.clear();
      domC.getOrAdd(emptyCtxt); // Empty context
      for (Quad h : hSet)
        domC.getOrAdd(emptyCtxt.append(h));
      domC.save();
    }

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
        else            relCI.add(c, q);
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
        for (Ctxt d : domC)
          if (d.length() <= 1) relCC.add(c, d);
        continue;
      }

      if (isAlloc(c.head())) continue;

      // Extend
      Quad i = c.head();
      for (Quad h : i2h.get(i)) extend(h, c);
      for (Quad j : i2i.get(i)) extend(j, c);
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
    }
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
