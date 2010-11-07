/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 *
 * Pointer analysis which performs refinement and pruning using slivers.
 *
 * An abstraction specifies a partitioning of chains (a chain is a list of allocation/call sites).
 * A sliver specifies a set of chains in one of two ways:
 *  - [h1, h2, h3] represents exactly this one sequence.
 *  - [h1, h2, h3, null] represents all sequences with the given prefix.
 * Assume that the abstraction is specified by a consistent set of slivers.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
package chord.analyses.alias;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.net.Socket;
import java.net.ServerSocket;

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
import chord.bddbddb.Rel.QuadIterable;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.HextIterable;
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
import chord.project.analyses.DlogAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IndexMap;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ChordRuntimeException;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.Utils;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;
import static chord.analyses.alias.GlobalInfo.G;

class GlobalInfo {
  static GlobalInfo G;

  // Slivers
  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);
  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; } // Is an allocation site?
  boolean hasHeadSite(Ctxt c) { return c.length() > 0 && c.head() != null; }
  boolean isSummary(Ctxt c) { return c.length() > 0 && c.last() == null; }
  boolean isAtom(Ctxt c) { return c.length() == 0 || c.last() != null; }
  Ctxt summarize(Ctxt c) { assert isAtom(c); return c.append(null); } // add null
  Ctxt atomize(Ctxt c) { assert isSummary(c); return c.prefix(c.length()-1); } // remove null
  int summaryLen(Ctxt c) { assert isSummary(c); return c.length()-1; } // don't count null
  int atomLen(Ctxt c) { assert isAtom(c); return c.length(); }

  DomV domV;
  DomM domM;
  DomI domI;
  DomH domH;
  DomE domE;
  HashMap<jq_Method,List<Quad>> rev_jm;
  Set<Quad> jSet;

  void sleep(int seconds) {
    try { Thread.sleep(seconds*1000); } catch(InterruptedException e) { }
  }

  public GlobalInfo() {
    domV = (DomV) ClassicProject.g().getTrgt("V"); ClassicProject.g().runTask(domV);
    domM = (DomM) ClassicProject.g().getTrgt("M"); ClassicProject.g().runTask(domM);
    domI = (DomI) ClassicProject.g().getTrgt("I"); ClassicProject.g().runTask(domI);
    domH = (DomH) ClassicProject.g().getTrgt("H"); ClassicProject.g().runTask(domH);
    domE = (DomE) ClassicProject.g().getTrgt("E"); ClassicProject.g().runTask(domE);
  }

  // Helpers for displaying stuff
  jq_Type h2t(Quad h) {
    Operator op = h.getOperator();
    if (op instanceof New) 
      return New.getType(h).getType();
    else if (op instanceof NewArray)
      return NewArray.getType(h).getType();
    else if (op instanceof MultiNewArray)
      return MultiNewArray.getType(h).getType();
    else
      return null;
  }
  String hstr(Quad h) {
    String path = new File(h.toJavaLocStr()).getName();
    jq_Type t = h2t(h);
    return path+"("+(t == null ? "?" : t.shortName())+")";
  }
  String istr(Quad i) {
    String path = new File(i.toJavaLocStr()).getName();
    jq_Method m = InvokeStatic.getMethod(i).getMethod();
    return path+"("+m.getName()+")";
  }
  String jstr(Quad j) { return isAlloc(j) ? hstr(j) : istr(j); }
  String estr(Quad e) {
    String path = new File(e.toJavaLocStr()).getName();
    Operator op = e.getOperator();
    return path+"("+op+")";
  }
  String cstr(Ctxt c) {
    StringBuilder buf = new StringBuilder();
    //buf.append(domC.indexOf(c));
    buf.append('{');
    for (int i = 0; i < c.length(); i++) {
      if (i > 0) buf.append(" | ");
      Quad q = c.get(i);
      buf.append(q == null ? "+" : jstr(q));
    }
    buf.append('}');
    return buf.toString();
  }
  String fstr(jq_Field f) { return f.getDeclaringClass()+"."+f.getName(); }
  String vstr(Register v) { return v+"@"+mstr(domV.getMethod(v)); }
  String mstr(jq_Method m) { return m.getDeclaringClass().shortName()+"."+m.getName(); }

  String qcode(Query q) { return "E"+G.domE.indexOf(q.e1)+","+G.domE.indexOf(q.e2); }

  String render(Object o) {
    if (o == null) return "NULL";
    if (o instanceof String) return (String)o;
    if (o instanceof Ctxt) return cstr((Ctxt)o);
    if (o instanceof jq_Field) return fstr((jq_Field)o);
    if (o instanceof jq_Method) return mstr((jq_Method)o);
    if (o instanceof Register) return vstr((Register)o);
    if (o instanceof Quad) {
      Quad q = (Quad)o;
      if (domH.indexOf(q) != -1) return hstr(q);
      if (domI.indexOf(q) != -1) return istr(q);
      if (domE.indexOf(q) != -1) return estr(q);
      throw new RuntimeException("Quad not H, I, or E: " + q);
    }
    throw new RuntimeException("Unknown object (not abstract object, contextual variable or field: "+o);
  }

  PrintWriter getOut(Socket s) throws IOException { return new PrintWriter(s.getOutputStream(), true); }
  BufferedReader getIn(Socket s) throws IOException { return new BufferedReader(new InputStreamReader(s.getInputStream())); }
}

////////////////////////////////////////////////////////////

class Query {
  Quad e1, e2;
  Query(Quad e1, Quad e2) { this.e1 = e1; this.e2 = e2; }
  @Override public int hashCode() { return e1.hashCode() * 37 + e2.hashCode(); }
  @Override public boolean equals(Object _that) {
    Query that = (Query)_that;
    return e1.equals(that.e1) && e2.equals(that.e2);
  }
}

////////////////////////////////////////////////////////////

// Current status
class Status {
  int numUnproven;
  int absHashCode;
  String absSummary;
}

class Abstraction {
  @Override public int hashCode() { return S.hashCode(); }
  @Override public boolean equals(Object _that) {
    Abstraction that = (Abstraction)_that;
    return S.equals(that.S);
  }

  Set<Ctxt> getSlivers() { return S; }

  void add(Ctxt c) { S.add(c); }
  void add(Abstraction that) { S.addAll(that.S); }
  boolean contains(Ctxt c) { return S.contains(c); }

  Histogram lengthHistogram(Set<Ctxt> slivers) {
    Histogram hist = new Histogram();
    for (Ctxt c : slivers) hist.counts[G.isAtom(c) ? G.atomLen(c) : G.summaryLen(c)]++;
    return hist;
  }

  // Extract the sites which are not pruned
  Set<Quad> inducedHeadSites() {
    Set<Quad> set = new HashSet<Quad>();
    for (Ctxt c : S)
      if (G.hasHeadSite(c))
        set.add(c.head());
    return set;
  }

  // assertDisjoint: this is when we are pruning slivers (make sure never step on each other's toes)
  void addRefinements(Ctxt c, int addk, boolean assertDisjoint) {
    assert addk >= 0;
    if (assertDisjoint) assert !S.contains(c);
    if (addk == 0)
      S.add(c);
    else {
      assert G.isSummary(c);
      Ctxt d = G.atomize(c);
      addRefinements(d, 0, assertDisjoint);
      Collection<Quad> extensions = G.atomLen(d) == 0 ?
        G.jSet : // All sites
        G.rev_jm.get(d.last().getMethod()); // sites that match
      for (Quad j : extensions) // Append possible j's
        addRefinements(G.summarize(d.append(j)), addk-1, assertDisjoint);
    }
  }

  // Return the minimum set of slivers in S that covers sliver c (covering defined with respect to set of chains)
  Ctxt project(Ctxt c) {
    if (G.isAtom(c)) { // atom
      if (S.contains(c)) return c; // Exact match
      // Assume there's at most one that matches
      for (int k = G.atomLen(c); k >= 0; k--) { // Take length k prefix
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      return null;
    }
    else { // summary
      // If we project ab* (by prepending a to b*) onto S={ab,abc*,abd*,...}, we should return all 3 values.
      // Generally, take every sliver that starts with ab, summary or not.
      // TODO: we don't handle this case, which is okay if all the longest summary slivers differ by length at most one.
      { // Match ab?
        Ctxt d = G.atomize(c);
        if (S.contains(d)) return d;
      }
      for (int k = G.summaryLen(c); k >= 0; k--) { // Take length k prefix (consider {ab*, a*, *}, exactly one should match)
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      return null;
    }
  }

  // Need this assumption if we're going to prune!
  void assertDisjoint() {
    // Make sure each summary sliver doesn't contain proper summary prefixes
    assert !S.contains(G.summarize(G.emptyCtxt));
    for (Ctxt c : S) {
      if (G.isAtom(c)) continue;
      assert !S.contains(G.atomize(c)) : G.cstr(c); // if x* exists, x can't exist
      for (int k = G.summaryLen(c)-1; k >= 0; k--) // if xy* exists, x* can't exist
        assert !S.contains(G.summarize(c.prefix(k))) : G.cstr(c);
    }
  }

  @Override public String toString() {
    int numSummaries = 0;
    for (Ctxt c : S) if (G.isSummary(c)) numSummaries++;
    return String.format("%s(%s)%s", S.size(), numSummaries, lengthHistogram(S));
  }
  int size() { return S.size(); }

  private HashSet<Ctxt> S = new HashSet<Ctxt>(); // The set of slivers
}

////////////////////////////////////////////////////////////

interface BlackBox {
  public String apply(String line);
}

@Chord(
  name = "sliver-ctxts-java",
  produces = { "C", "CC", "CH", "CI", "objI", "kcfaSenM", "kobjSenM", "ctxtCpyM", "inQuery" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis implements BlackBox {
  private Execution X;

  // Options
  int verbose;
  int maxIters;
  boolean useObjectSensitivity;
  boolean inspectTransRels;
  boolean verifyAfterPrune;
  boolean pruneSlivers, refineSites;

  String masterHost;
  int masterPort;
  String mode; // worker or master or null
  boolean minimizeAbstraction; // Find the minimal abstraction via repeated calls
  int minH, maxH, minI, maxI;
  String classifierPath; // Path to classifier (for determining which sites are relevant)
  List<String> initTasks = new ArrayList<String>();
  List<String> tasks = new ArrayList<String>();
  String relevantTask;
  String transTask;

  // Compute once using 0-CFA
  Set<Quad> hSet = new HashSet<Quad>();
  Set<Quad> iSet = new HashSet<Quad>();
  Set<Quad> jSet = new HashSet<Quad>(); // hSet union iSet
  HashMap<jq_Method,List<Quad>> rev_jm = new HashMap<jq_Method,List<Quad>>(); // method m -> sites that be the prefix of a context for m
  HashMap<Quad,List<jq_Method>> jm = new HashMap<Quad,List<jq_Method>>(); // site to methods
  HashMap<jq_Method,List<Quad>> mj = new HashMap<jq_Method,List<Quad>>(); // method to sites

  List<Query> allQueries = new ArrayList<Query>();
  List<Status> statuses = new ArrayList<Status>(); // Status of the analysis over iterations of refinement
  QueryGroup unprovenGroup = new QueryGroup();
  List<QueryGroup> provenGroups = new ArrayList<QueryGroup>();
  int maxRunAbsSize = -1;
  int lastRunAbsSize = -1;

  boolean useClassifier() { return classifierPath != null; }
  boolean isMaster() { return mode != null && mode.equals("master"); }
  boolean isWorker() { return mode != null && mode.equals("worker"); }

  ////////////////////////////////////////////////////////////

  // Initialization to do anything.
  private void init() {
    X = Execution.v();

    G = new GlobalInfo();
    G.rev_jm = rev_jm;
    G.jSet = jSet;

    this.verbose                 = X.getIntArg("verbose", 0);
    this.maxIters                = X.getIntArg("maxIters", 1);
    this.useObjectSensitivity    = X.getBooleanArg("useObjectSensitivity", false);
    this.inspectTransRels        = X.getBooleanArg("inspectTransRels", false);
    this.verifyAfterPrune        = X.getBooleanArg("verifyAfterPrune", false);
    this.pruneSlivers            = X.getBooleanArg("pruneSlivers", false);
    this.refineSites             = X.getBooleanArg("refineSites", false);

    this.masterHost              = X.getStringArg("masterHost", null);
    this.masterPort              = X.getIntArg("masterPort", 8888);
    this.mode                    = X.getStringArg("mode", null);
    this.minimizeAbstraction     = X.getBooleanArg("minimizeAbstraction", false);
    this.classifierPath          = X.getStringArg("classifierPath", null);

    this.minH = X.getIntArg("minH", 1);
    this.maxH = X.getIntArg("maxH", 2);
    this.minI = X.getIntArg("minI", 1);
    this.maxI = X.getIntArg("maxI", 2);

    this.initTasks.add("sliver-init-dlog");
    for (String name : X.getStringArg("initTaskNames", "").split(","))
      this.initTasks.add(name);
    for (String name : X.getStringArg("taskNames", "").split(","))
      this.tasks.add(name);
    this.relevantTask = X.getStringArg("relevantTaskName", null);
    this.transTask = X.getStringArg("transTaskName", null);

    X.putOption("version", 1);
    X.putOption("program", System.getProperty("chord.work.dir"));
    X.flushOptions();

    // Initialization Datalog programs
    for (String task : initTasks)
      ClassicProject.g().runTask(task);

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
    X.logs("Finished 0-CFA: |hSet| = %s, |iSet| = %s", hSet.size(), iSet.size());
    jSet.addAll(hSet);
    if (!useObjectSensitivity) jSet.addAll(iSet); // need call sites

    // Allocate memory
    for (Quad h : hSet) jm.put(h, new ArrayList<jq_Method>());
    if (!useObjectSensitivity) {
      for (Quad i : iSet) jm.put(i, new ArrayList<jq_Method>());
    }
    for (jq_Method m : G.domM) mj.put(m, new ArrayList<Quad>());
    for (jq_Method m : G.domM) rev_jm.put(m, new ArrayList<Quad>());

    // Extensions of sites depends on the target method
    if (!useObjectSensitivity) {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("IM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad i = pair.val0;
        jq_Method m = pair.val1;
        assert iSet.contains(i) : G.istr(i);
        jm.get(i).add(m);
        rev_jm.get(m).add(i);
      }
      rel.close();
    }
    else {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("HtoM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad h = pair.val0;
        jq_Method m = pair.val1;
        assert hSet.contains(h) : G.hstr(h);
        jm.get(h).add(m);
        rev_jm.get(m).add(h);
      }
      rel.close();
    }

    // Sites contained in a method
    if (!useObjectSensitivity) {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("MI"); rel.load();
      PairIterable<jq_Method,Quad> result = rel.getAry2ValTuples();
      for (Pair<jq_Method,Quad> pair : result) {
        jq_Method m = pair.val0;
        Quad i = pair.val1;
        mj.get(m).add(i);
      }
      rel.close();
    }
    { // Note: both allocation and call sites need this
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("MH"); rel.load();
      PairIterable<jq_Method,Quad> result = rel.getAry2ValTuples();
      for (Pair<jq_Method,Quad> pair : result) {
        jq_Method m = pair.val0;
        Quad h = pair.val1;
        mj.get(m).add(h);
      }
      rel.close();
    }

    // Compute statistics on prependings (for analysis) and extensions (for refinement)
    { // prepends
      Histogram hist = new Histogram();
      for (Quad j : jSet) {
        int n = 0;
        for (jq_Method m : jm.get(j))
          n += mj.get(m).size();
        hist.add(n);
      }
      X.logs("For analysis (building CH,CI,CC): # prependings of sites: %s", hist);
    }
    { // extensions
      Histogram hist = new Histogram();
      for (Quad j : jSet)
        hist.add(rev_jm.get(j.getMethod()).size());
      X.logs("For refinement (growing slivers): # extensions of sites: %s", hist);
    }

    // Compute which queries we should answer in the whole program
    String focus = X.getStringArg("focusQuery", null);
    if (focus != null) {
      String[] tokens = focus.split(",");
      Quad e1 = G.domE.get(Integer.parseInt(tokens[0]));
      Quad e2 = G.domE.get(Integer.parseInt(tokens[1]));
      allQueries.add(new Query(e1, e2));
    }
    else {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("query"); rel.load();
      PairIterable<Quad,Quad> result = rel.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result) allQueries.add(new Query(pair.val0, pair.val1));
      rel.close();
    }
    X.logs("Starting with %s total queries", allQueries.size());
  }

  void finish() { X.finish(null); }

  public void run() {
    init();
    if (isWorker()) runWorker();
    else if (minimizeAbstraction) {
      Client client = new AbstractionMinimizer(allQueries, jSet, minH, maxH, minI, maxI, this);
      new Master(masterPort, client);
    }
    else refinePruneLoop();
    finish();
  }

  String callMaster(String line) {
    try {
      Socket master = new Socket(masterHost, masterPort);
      BufferedReader in = G.getIn(master);
      PrintWriter out = G.getOut(master);
      out.println(line);
      out.flush();
      line = in.readLine();
      in.close();
      out.close();
      master.close();
      return line;
    } catch (IOException e) {
      return null;
    }
  }

  public String apply(String line) {
    QueryGroup group = new QueryGroup();
    decodeAbstractionQueries(line, group.abs, group.queries);
    boolean fastWrongHack = false;
    if (fastWrongHack) {
      // Fast and wrong hack (for testing master/worker connection):
      // Prove a query if we've given context sensitivity to an allocation site in the containing method.
      Set<jq_Method> methods = new HashSet(); // Good methods
      for (Ctxt c : group.abs.getSlivers()) {
        if (G.isSummary(c) && G.summaryLen(c) > (G.isAlloc(c.head()) ? minH : minI))
          methods.add(c.head().getMethod());
      }
      Set<Query> outQueries = new HashSet();
      for (Query q : group.queries)
        if (!methods.contains(q.e1.getMethod()) && !methods.contains(q.e2.getMethod()))
          outQueries.add(q);
      return encodeQueries(outQueries);
    }
    else {
      group.runAnalysis(false);
      group.removeProvenQueries();
      return encodeQueries(group.queries);
    }
  }

  void runWorker() {
    X.logs("Starting worker...");
    int numJobs = 0;
    while (true) {
      X.logs("============================================================");
      // Get a job
      String line = callMaster("GET");
      if (line == null) { X.logs("Got null, something bad happened to master..."); G.sleep(5); }
      else if (line.equals("WAIT")) { X.logs("Waiting..."); G.sleep(5); X.putOutput("exec.status", "waiting"); }
      else if (line.equals("EXIT")) { X.logs("Exiting..."); break; }
      else {
        X.putOutput("exec.status", "running"); X.flushOutput();

        String[] tokens = line.split(" ", 2);
        String id = tokens[0];
        String input = tokens[1];
        String output = apply(input);
        line = callMaster("PUT "+id+" "+output);
        X.logs("Sent result to master, got reply: %s", line);
        numJobs++;

        X.putOutput("numJobs", numJobs); X.flushOutput();
      }
    }
  }

  // Format: H*:1 I*:0 H3:5 I6:1 ## E2,4 (things not shown are 0)
  void decodeAbstractionQueries(String line, Abstraction abs, Set<Query> queries) {
    HashMap<Quad,Integer> lengths = new HashMap();
    int minH = 0;
    int minI = 0;
    boolean includeAllQueries = true;
    //X.logs("DECODE %s", line);
    for (String s : line.split(" ")) {
      if (s.charAt(0) == 'E') {
        String[] tokens = s.substring(1).split(",");
        Quad e1 = G.domE.get(Integer.parseInt(tokens[0]));
        Quad e2 = G.domE.get(Integer.parseInt(tokens[1]));
        queries.add(new Query(e1, e2));
      }
      else if (s.charAt(0) == 'H' || s.charAt(0) == 'I') {
        String[] tokens = s.substring(1).split(":");
        int len = Integer.parseInt(tokens[1]);
        if (tokens[0].equals("*")) {
          if (s.charAt(0) == 'H') minH = len;
          else if (s.charAt(0) == 'I') minI = len;
          else throw new RuntimeException(s);
        }
        else {
          int n = Integer.parseInt(tokens[0]);
          Quad q = null;
          if (s.charAt(0) == 'H') q = (Quad)G.domH.get(n);
          else if (s.charAt(0) == 'I') q = G.domI.get(n);
          else throw new RuntimeException(s);
          assert jSet.contains(q);
          lengths.put(q, len);
        }
      }
      else if (s.equals("##"))
        includeAllQueries = false;
      else throw new RuntimeException("Bad: " + line);
    }
    for (Quad q : jSet)
      if (!lengths.containsKey(q))
        lengths.put(q, G.isAlloc(q) ? minH : minI);
    assert lengths.size() == jSet.size();

    if (includeAllQueries) queries.addAll(allQueries);
    X.logs("decodeAbstractionQueries: got minH=%s, minI=%s, %s queries", minH, minI, queries.size());

    // No pruning allowed!
    assert !pruneSlivers;
    abs.add(G.summarize(G.emptyCtxt));
    for (Quad q : lengths.keySet()) {
      int len = lengths.get(q);
      if (len > 0)
        abs.addRefinements(G.summarize(G.emptyCtxt.append(q)), len-1, pruneSlivers);
    }
  }

  String encodeQueries(Set<Query> queries) {
    X.logs("encodeQueries: |Y|=%s", queries.size());
    StringBuilder buf = new StringBuilder();
    for (Query q : queries) {
      if (buf.length() > 0) buf.append(' ');
      buf.append(G.qcode(q));
    }
    return buf.toString();
  }

  // Special case this: include [*]
  // Only do this when pruning slivers.
  boolean is0CFA() { return minH == 1 && minI == 0 && !useObjectSensitivity; }

  void refinePruneLoop() {
    X.logs("Initializing abstraction with length minH=%s,minI=%s slivers (|jSet|=%s)", minH, minI, jSet.size());
    // Initialize abstraction
    unprovenGroup.abs.add(pruneSlivers && !is0CFA() ? G.emptyCtxt : G.summarize(G.emptyCtxt));
    for (Quad j : jSet) {
      int len = G.isAlloc(j) ? minH : minI;
      if (pruneSlivers && !is0CFA()) assert len > 0;
      if (len > 0)
        unprovenGroup.abs.addRefinements(G.summarize(G.emptyCtxt.append(j)), len-1, pruneSlivers);
    }

    X.logs("Unproven group with %s queries", allQueries.size());
    unprovenGroup.queries.addAll(allQueries);

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      boolean runRelevantAnalysis = iter < maxIters && (pruneSlivers || refineSites);
      unprovenGroup.runAnalysis(runRelevantAnalysis);
      backupRelations(iter);
      if (inspectTransRels) unprovenGroup.inspectAnalysisOutput();
      QueryGroup provenGroup = unprovenGroup.removeProvenQueries();
      if (provenGroup != null) provenGroups.add(provenGroup);

      if (pruneSlivers && runRelevantAnalysis) {
        unprovenGroup.pruneAbstraction();
        if (verifyAfterPrune && provenGroup != null) {
          X.logs("verifyAfterPrune");
          // Make sure this is a complete abstraction
          assert provenGroup.abs.inducedHeadSites().equals(jSet);
          provenGroup.runAnalysis(runRelevantAnalysis);
        }
      }

      outputStatus(iter);

      if (statuses.get(statuses.size()-1).numUnproven == 0) {
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

      unprovenGroup.refineAbstraction();
    }
  }

  int numUnproven() {
    int n = 0;
    for (QueryGroup g : provenGroups)
      n += g.queries.size();
    return allQueries.size()-n;
  }

  int[] parseIntArray(String s) {
    String[] tokens = s.split(",");
    int[] l = new int[tokens.length];
    for (int i = 0; i < l.length; i++)
      l[i] = Integer.parseInt(tokens[i]);
    return l;
  }

  int relSize(String name) {
    ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(name); rel.load();
    int n = rel.size();
    rel.close();
    return n;
  }

  boolean converged() {
    if (statuses.size() < 2) return false;
    Status a = statuses.get(statuses.size()-2);
    Status b = statuses.get(statuses.size()-1);
    return a.absHashCode == b.absHashCode;
  }

  void outputStatus(int iter) {
    X.logs("outputStatus(iter=%s)", iter);
    // Get the total size of the abstraction across all queries
    Abstraction totalAbs = new Abstraction();
    for (QueryGroup g : provenGroups) {
      totalAbs.add(g.abs);
      assert g.prunedAbs.size() == 0;
    }
    totalAbs.add(unprovenGroup.prunedAbs);
    totalAbs.add(unprovenGroup.abs);
   
    X.addSaveFiles("abstraction.S."+iter);
    {
      PrintWriter out = OutDirUtils.newPrintWriter("abstraction.S."+iter);
      for (Ctxt a : totalAbs.getSlivers())
        out.println(G.cstr(a));
      out.close();
    }

    int numUnproven = numUnproven();
    Status status = new Status();
    status.numUnproven = numUnproven;
    status.absHashCode = unprovenGroup.abs.hashCode();
    status.absSummary = unprovenGroup.abs.toString();
    statuses.add(status);

    X.putOutput("currIter", iter);
    X.putOutput("absSize", totalAbs.size());
    X.putOutput("maxRunAbsSize", maxRunAbsSize);
    X.putOutput("lastRunAbsSize", lastRunAbsSize);
    X.putOutput("numQueries", allQueries.size());
    X.putOutput("numProven", allQueries.size()-numUnproven);
    X.putOutput("numUnproven", numUnproven);
    X.putOutput("numUnprovenHistory", numUnprovenHistory());
    X.flushOutput();
  }

  String numUnprovenHistory() {
    StringBuilder buf = new StringBuilder();
    for (Status s : statuses) {
      if (buf.length() > 0) buf.append(',');
      buf.append(s.numUnproven);
    }
    return buf.toString();
  }

  // Group of queries which have the same abstraction and be have the same as far as we can tell.
  class QueryGroup {
    Set<Query> queries = new LinkedHashSet<Query>();
    // Invariant: abs + prunedAbs is a full abstraction and gets same results as abs
    Abstraction abs = new Abstraction(); // Current abstraction for this query
    Abstraction prunedAbs = new Abstraction(); // This abstraction keeps all the slivers that have been pruned

    void runAnalysis(boolean runRelevantAnalysis) {
      X.logs("runAnalysis: %s", abs);
      maxRunAbsSize = Math.max(maxRunAbsSize, abs.size());
      lastRunAbsSize = abs.size();

      // Domain (these are the slivers)
      DomC domC = (DomC) ClassicProject.g().getTrgt("C");
      domC.clear();
      assert abs.project(G.emptyCtxt) != null;
      domC.getOrAdd(abs.project(G.emptyCtxt)); // This must go first!
      for (Ctxt c : abs.getSlivers()) domC.add(c);
      domC.save();

      /*for (Ctxt c : abs.getSlivers()) {
        if (G.hasHeadSite(c) && G.isAlloc(c.head()))
          X.logs("OLD %s %s", G.jstr(c.head()), G.cstr(c));
      }*/

      // Relations
      ProgramRel CH = (ProgramRel) ClassicProject.g().getTrgt("CH");
      ProgramRel CI = (ProgramRel) ClassicProject.g().getTrgt("CI");
      ProgramRel CC = (ProgramRel) ClassicProject.g().getTrgt("CC");
      CH.zero();
      CI.zero();
      CC.zero();
      for (Ctxt c : abs.getSlivers()) { // From sliver c...
        if (G.hasHeadSite(c)) {
          for (jq_Method m : jm.get(c.head())) {
            for (Quad j : mj.get(m)) { // Extend with some site j that could be prepended
              Ctxt d = abs.project(c.prepend(j));
              if (!pruneSlivers) assert d != null;
              if (d != null) {
                (G.isAlloc(j) ? CH : CI).add(d, j);
                //if (G.isAlloc(j)) X.logs("NEW %s %s %s", G.cstr(c), G.jstr(j), G.cstr(d));
                CC.add(c, d);
              }
            }
          }
        }
        else {
          for (Quad j : jSet) { // Extend with any site j
            Ctxt d = abs.project(c.prepend(j));
            if (!pruneSlivers) assert d != null;
            if (d != null) {
              (G.isAlloc(j) ? CH : CI).add(d, j);
              //if (G.isAlloc(j)) X.logs("NEW2 %s %s", G.jstr(j), G.cstr(d));
              CC.add(c, d);
            }
          }
        }
      }
      CH.save();
      CI.save();
      CC.save();

      // Determine CFA or object-sensitivity
      ProgramRel relobjI = (ProgramRel) ClassicProject.g().getTrgt("objI");
      relobjI.zero();
      if (useObjectSensitivity) {
        for (Quad i : iSet) relobjI.add(i);
      }
      relobjI.save();
      ProgramRel relKcfaSenM = (ProgramRel) ClassicProject.g().getTrgt("kcfaSenM");
      ProgramRel relKobjSenM = (ProgramRel) ClassicProject.g().getTrgt("kobjSenM");
      ProgramRel relCtxtCpyM = (ProgramRel) ClassicProject.g().getTrgt("ctxtCpyM");
      relKcfaSenM.zero();
      relKobjSenM.zero();
      relCtxtCpyM.zero();
      if (useObjectSensitivity) {
        for (jq_Method m : G.domM) {
          if (m.isStatic()) relCtxtCpyM.add(m);
          else              relKobjSenM.add(m);
        }
      }
      else {
        for (jq_Method m : G.domM) relKcfaSenM.add(m);
      }
      relKcfaSenM.save();
      relKobjSenM.save();
      relCtxtCpyM.save();

      ProgramRel relInQuery = (ProgramRel) ClassicProject.g().getTrgt("inQuery");
      relInQuery.zero();
      for (Query q : queries)
        relInQuery.add(q.e1, q.e2);
      relInQuery.save();

      ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
      ClassicProject.g().setTaskDone(SliverCtxtsAnalysis.this); // We are generating all this stuff, so mark it as done...
      ClassicProject.g().setTrgtDone(domC);
      ClassicProject.g().setTrgtDone(CH);
      ClassicProject.g().setTrgtDone(CI);
      ClassicProject.g().setTrgtDone(CC);
      ClassicProject.g().setTrgtDone(relobjI);
      ClassicProject.g().setTrgtDone(relKcfaSenM);
      ClassicProject.g().setTrgtDone(relKobjSenM);
      ClassicProject.g().setTrgtDone(relCtxtCpyM);
      ClassicProject.g().setTrgtDone(relInQuery);

      for (String task : tasks)
        ClassicProject.g().runTask(task);
      if (runRelevantAnalysis) ClassicProject.g().runTask(relevantTask);
      if (inspectTransRels) ClassicProject.g().runTask(transTask);
    }

    Abstraction relevantAbs() {
      // From Datalog, read out the pruned abstraction
      Abstraction relevantAbs = new Abstraction(); // These are the slivers we keep
      relevantAbs.add(abs.project(G.emptyCtxt)); // Always keep this, because it probably won't show up in CH or CI
      for (String relName : new String[] {"r_CH", "r_CI"}) {
        ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(relName); rel.load();
        PairIterable<Ctxt,Quad> result = rel.getAry2ValTuples();
        for (Pair<Ctxt,Quad> pair : result)
          relevantAbs.add(pair.val0);
        rel.close();
      }
      return relevantAbs;
    }

    void pruneAbstraction() {
      assert pruneSlivers;
      Abstraction newAbs = relevantAbs();

      // Record the pruned slivers (abs - newAbs)
      for (Ctxt c : abs.getSlivers())
        if (!newAbs.contains(c))
          prunedAbs.add(c);

      X.logs("STATUS pruneAbstraction: %s -> %s", abs, newAbs);
      abs = newAbs;
      if (!is0CFA())
        abs.assertDisjoint();
    }

    // Remove queries that have been proven
    QueryGroup removeProvenQueries() {
      // From Datalog, read out all unproven queries
      Set<Query> unproven = new HashSet<Query>();
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("outQuery"); rel.load();
      PairIterable<Quad,Quad> result = rel.getAry2ValTuples();
      for (Pair<Quad,Quad> pair : result)
        unproven.add(new Query(pair.val0, pair.val1));
      rel.close();

      // Put all proven queries in a new group
      QueryGroup provenGroup = new QueryGroup();
      provenGroup.abs.add(prunedAbs); // Build up complete abstraction
      provenGroup.abs.add(abs);
      assert abs.size()+prunedAbs.size() == provenGroup.abs.size(); // No duplicates
      for (Query q : queries)
        if (!unproven.contains(q))
          provenGroup.queries.add(q);
      for (Query q : provenGroup.queries)
        queries.remove(q);

      X.logs("STATUS %s/%s queries unproven", queries.size(), allQueries.size());

      return provenGroup;
    }

    void inspectAnalysisOutput() {
      // Display the transition graph over relations
      try {
        RelGraph graph = new RelGraph();
        String dlogPath = ((DlogAnalysis)ClassicProject.g().getTask(transTask)).getFileName();
        X.logs("Reading transitions from "+dlogPath);
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dlogPath)));
        String line;
        while ((line = in.readLine()) != null) {
          if (!line.startsWith("# TRANS")) continue;
          String[] tokens = line.split(" ");
          graph.loadTransition(tokens[2], tokens[3], tokens[4], tokens[5], parseIntArray(tokens[6]), parseIntArray(tokens[7]));
        }
        in.close();
        graph.display();

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    void refineAbstraction() {
      String oldAbsStr = abs.toString();
      Abstraction newAbs = new Abstraction();

      if (refineSites) {
        Set<Quad> relevantSites = relevantAbs().inducedHeadSites();
        X.logs("%s/%s sites relevant", relevantSites.size(), jSet.size());
        for (Ctxt c : abs.getSlivers()) { // For each sliver...
          if (G.isSummary(c) && (!G.hasHeadSite(c) || relevantSites.contains(c.head()))) // If have a relevant head site (or empty)
            newAbs.addRefinements(c, 1, pruneSlivers);
          else
            newAbs.add(c);
        }
      }
      else {
        for (Ctxt c : abs.getSlivers()) { // For each sliver
          if (G.isSummary(c)) {
            if (G.summaryLen(c) == 0 && is0CFA()) { // Special case 0-CFA to avoid overlapping (relevant if pruning)
              newAbs.addRefinements(G.emptyCtxt, 0, pruneSlivers);
              for (Quad i : iSet) // Only extend [*] to [i,*] for call sites i, because [h,*] already exist (really [*] represented [*]-[h,*])
                newAbs.addRefinements(G.summarize(G.emptyCtxt.prepend(i)), 0, pruneSlivers);
            }
            else
              newAbs.addRefinements(c, 1, pruneSlivers);
          }
          else newAbs.add(c); // Leave atomic ones alone (already precise as possible)
        }
        if (pruneSlivers)
          newAbs.assertDisjoint();
      }

      abs = newAbs;
      String newAbsStr = abs.toString();

      X.logs("STATUS refineAbstraction: %s -> %s", oldAbsStr, newAbsStr);
    }
  }

  void backupRelations(int iter) {
    try {
      if (X.getBooleanArg("saveRelations", false)) {
        X.logs("backupRelations");
        String path = X.path(""+iter);
        new File(path).mkdir();

        DomC domC = (DomC) ClassicProject.g().getTrgt("C");
        domC.save(path, true);

        String[] names = new String[] { "CH", "CI", "CH", "inQuery", "outQuery" };
        for (String name : names) {
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
  }
}

class Histogram {
  int[] counts = new int[1000];
  void clear() {
    for (int i = 0; i < counts.length; i++)
      counts[i] = 0;
  }
  public void add(int i) {
    if (i >= counts.length) {
      int[] newCounts = new int[Math.max(counts.length*2,i+1)];
      System.arraycopy(counts, 0, newCounts, 0, counts.length);
      counts = newCounts;
    }
    counts[i]++;
  }
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

// For visualization
class RelNode {
  Execution X = Execution.v();
  List<Object> rel;
  List<RelNode> edges = new ArrayList<RelNode>();
  List<String> names = new ArrayList<String>();
  boolean visited;
  boolean root = true;

  RelNode(List<Object> rel) { this.rel = rel; }

  // Assume: if node visited iff children are also visited
  void clearVisited() {
    if (!visited) return;
    visited = false;
    for (RelNode node : edges)
      node.clearVisited();
  }

  String nameContrib(String name) { return name == null ? "" : "("+name+") "; }

  String extra() { return ""; }

  void display(String prefix, String parentName) {
    X.logs(prefix + extra() + nameContrib(parentName) + this + (edges.size() > 0 ? " {" : ""));
    String newPrefix = prefix+"  ";
    visited = true;
    for (int i = 0; i < edges.size(); i++) {
      RelNode node = edges.get(i);
      String name = names.get(i);
      if (node.visited) X.logs(newPrefix + node.extra() + nameContrib(name) + node + " ...");
      else node.display(newPrefix, name);
    }
    if (edges.size() > 0) X.logs(prefix+"}");
  }

  @Override public String toString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < rel.size(); i++) {
      b.append(G.render(rel.get(i)));
      if (i == 0) b.append('[');
      else if (i == rel.size()-1) b.append(']');
      else b.append(' ');
    }
    return b.toString();
  }
}

// Graph where nodes are relations r_X and edges are transitions t_X_Y
class RelGraph {
  Execution X = Execution.v();
  HashMap<List<Object>,RelNode> nodes = new HashMap<List<Object>,RelNode>();

  RelNode getNode(List<Object> rel) {
    RelNode node = nodes.get(rel);
    if (node == null)
      nodes.put(rel, node = new RelNode(rel));
    return node;
  }

  void add(List<Object> s, String name, List<Object> t) {
    RelNode node_s = getNode(s);
    RelNode node_t = getNode(t);
    //X.logs("EDGE | %s | %s", node_s, node_t);
    node_s.names.add(name);
    node_s.edges.add(node_t);
    node_t.root = false;
  }

  void display() {
    X.logs("===== GRAPH =====");
    for (RelNode node : nodes.values()) {
      if (node.root) {
        node.clearVisited();
        node.display("", null);
      }
    }
  }

  List<Object> buildRel(String relName, Object[] l, int[] indices) {
    List<Object> rel = new ArrayList<Object>();
    rel.add(relName);
    for (int i : indices) rel.add(l[i]);
    return rel;
  }

  void loadTransition(String name, String relName, String rel_s, String rel_t, int[] indices_s, int[] indices_t) {
    if (name.equals("-")) name = null;
    ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(relName); rel.load();
    for (Object[] l : rel.getAryNValTuples()) {
      List<Object> s = buildRel(rel_s, l, indices_s);
      List<Object> t = buildRel(rel_t, l, indices_t);
      add(t, name, s); // Backwards
    }
    rel.close();
  }
}

////////////////////////////////////////////////////////////

class Scenario {
  private static int newId = 0;

  int id = newId++;
  String in, out;
  public Scenario(String line) {
    String[] tokens = line.split(" ## ");
    in = tokens[0];
    out = tokens[1];
  }
  public Scenario(String in, String out) { this.in = in; this.out = out; }
  @Override public String toString() { return in + " ## " + out; }
}

interface Client {
  Scenario createJob(); // Ask client for job
  void onJobResult(Scenario scenario); // Return job result
  boolean isDone();
  void saveState();
  int maxWorkersNeeded();
}

class Master {
  boolean shouldExit;
  Execution X = Execution.v();
  int port;

  HashMap<Integer,Scenario> inprogress = new HashMap<Integer,Scenario>();
  HashMap<String,Long> lastContact = new HashMap();
  Client client;

  final boolean waitForWorkersToExit = true;

  int numWorkers() { return lastContact.size(); }

  public Master(int port, Client client) {
    this.port = port;
    this.client = client;
    boolean exitFlag = false;

    X.logs("MASTER: listening at port %s", port);
    try {
      ServerSocket master = new ServerSocket(port);
      while (true) {
        if (exitFlag && (!waitForWorkersToExit || lastContact.size() == 0)) break;
        X.putOutput("numWorkers", numWorkers());
        X.flushOutput();
        X.logs("============================================================");
        boolean clientIsDone = client.isDone();
        if (clientIsDone) {
          if (!waitForWorkersToExit || lastContact.size() == 0) break;
          X.logs("Client is done but still waiting for %s workers to exit...", lastContact.size());
        }
        Socket worker = master.accept();
        String hostname = worker.getInetAddress().getHostAddress();
        BufferedReader in = G.getIn(worker);
        PrintWriter out = G.getOut(worker);

        X.logs("MASTER: Got connection from worker %s", worker);
        String cmd = in.readLine();
        if (cmd.equals("GET")) {
          lastContact.put(hostname, System.currentTimeMillis()); // Only add if it's getting stuff
          if (clientIsDone || lastContact.size() > client.maxWorkersNeeded() + 1) { // 1 for extra buffer
            // If client is done or we have more workers than we need, then quit
            out.println("EXIT");
            lastContact.remove(hostname);
          }
          else {
            Scenario scenario = client.createJob();
            if (scenario == null)
              out.println("WAIT");
            else {
              inprogress.put(scenario.id, scenario);
              out.println(scenario.id + " " + scenario); // Response: <ID> <task spec>
              X.logs("  GET => id=%s", scenario.id);
            }
          }
        }
        else if (cmd.equals("SAVE")) {
          client.saveState();
          out.println("Saved");
        }
        else if (cmd.equals("EXIT")) {
          exitFlag = true;
          out.println("Going to exit...");
        }
        else if (cmd.equals("FLUSH")) {
          // Flush dead workers
          HashMap<String,Long> newLastContact = new HashMap();
          for (String name : lastContact.keySet()) {
            long t = lastContact.get(name);
            if (System.currentTimeMillis() - t < 60*60*1000)
              newLastContact.put(name, t);
          }
          lastContact = newLastContact;
          X.logs("%d workers", lastContact.size());
          out.println(lastContact.size()+" workers left");
        }
        else if (cmd.startsWith("PUT")) {
          String[] tokens = cmd.split(" ", 3); // PUT <ID> <task result>
          int id = Integer.parseInt(tokens[1]);
          Scenario scenario = inprogress.remove(id);
          if (scenario == null) {
            X.logs("  PUT id=%s, but doesn't exist", id);
            out.println("INVALID");
          }
          else {
            X.logs("  PUT id=%s", id);
            scenario.out = tokens[2];
            client.onJobResult(scenario);
            out.println("OK");
          }
        }

        in.close();
        out.close();
      }
      master.close();
      client.saveState();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

class AbstractionMinimizer implements Client {
  Execution EX = Execution.v();
  Random random = new Random();
  String defaults;
  
  boolean[] isAlloc;
  ArrayList<Quad> sites;
  IndexMap<String> components;
  int C;
  int[] bottomX, topX;
  HashMap<String,Query> y2queries;
  double initIncrProb = 0.5;
  double incrThetaStepSize = 0.1;
  int scanThreshold = 30;

  int numScenarios = 0; // Total number of calls to the analysis oracle
  List<Group> groups = new ArrayList();

  Set<String> allY() { return y2queries.keySet(); }

  public AbstractionMinimizer(List<Query> allQueries, Set<Quad> jSet,
      int minH, int maxH, int minI, int maxI, BlackBox box) {
    this.C = jSet.size();
    this.isAlloc = new boolean[C];
    this.sites = new ArrayList();
    this.components = new IndexMap();
    this.bottomX = new int[C];
    this.topX = new int[C];
    int c = 0;
    for (Quad q : jSet) {
      sites.add(q);
      if (G.isAlloc(q)) {
        components.add("H"+G.domH.indexOf(q));
        isAlloc[c] = true;
        bottomX[c] = minH;
        topX[c] = maxH;
      }
      else {
        components.add("I"+G.domI.indexOf(q));
        isAlloc[c] = false;
        bottomX[c] = minI;
        topX[c] = maxI;
      }
      c++;
    }
    this.defaults = "H*:"+minH + " " + "I*:"+minI;

    // Run the analysis twice to find the queries that differ between top and bottom
    Set<String> bottomY = decodeY(box.apply(encodeX(bottomX)));
    Set<String> topY = decodeY(box.apply(encodeX(topX)));
    EX.logs("bottom (kobj=%s,kcfa=%s) : %s/%s queries unproven", minH, minI, bottomY.size(), allQueries.size());
    EX.logs("top (kobj=%s,kcfa=%s) : %s/%s queries unproven", maxH, maxI, topY.size(), allQueries.size());

    this.y2queries = new HashMap();
    for (Query q : allQueries)
      y2queries.put(G.qcode(q), q);

    // Keep only queries that bottom was unable to prove but top was able to prove
    HashSet<String> Y = new HashSet();
    for (Query q : allQueries) {
      String y = G.qcode(q);
      if (bottomY.contains(y) && !topY.contains(y)) // Unproven by bottom, proven by top
        Y.add(y);
      else
        y2queries.remove(y); // Don't need this
    }
    assert Y.size() == y2queries.size();
    EX.logs("|Y| = %s", Y.size());
    EX.putOutput("numY", Y.size());
    EX.putOutput("topComplexity", complexity(topX));

    groups.add(new Group(Y));

    outputStatus();
    loadScenarios();
  }

  void loadScenarios() {
    String scenariosPath = EX.path("scenarios");
    if (!new File(scenariosPath).exists()) return;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(scenariosPath)));
      String line;
      while ((line = in.readLine()) != null)
        incorporateScenario(new Scenario(line), false);
      in.close();
      outputStatus();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  String encodeX(int[] X) {
    StringBuilder buf = new StringBuilder();
    buf.append(defaults);
    for (int c = 0; c < C; c++) {
      if (X[c] == bottomX[c]) continue;
      buf.append(' ' + components.get(c) + ':' + X[c]);
    }
    return buf.toString();
  }
  String encodeY(Set<String> Y) {
    StringBuilder buf = new StringBuilder();
    for (String y : Y) {
      if (buf.length() > 0) buf.append(' ');
      buf.append(y);
    }
    return buf.toString();
  }
  int[] decodeX(String line) {
    int[] X = new int[C];
    for (int c = 0; c < C; c++) X[c] = -1;
    int minH = 0, minI = 0; // Defaults
    for (String s : line.split(" ")) {
      String[] tokens = s.split(":");
      assert tokens.length == 2 : s;
      int len = Integer.parseInt(tokens[1]);
      if (tokens[0].equals("H*")) minH = len;
      else if (tokens[0].equals("I*")) minI = len;
      else {
        int c = components.indexOf(tokens[0]);
        assert c != -1 : s;
        X[c] = len;
      }
    }
    // Fill in defaults
    for (int c = 0; c < C; c++)
      if (X[c] == -1)
        X[c] = isAlloc[c] ? minH : minI;
    return X;
  }
  HashSet<String> decodeY(String line) {
    HashSet<String> Y = new HashSet<String>();
    for (String y : line.split(" ")) Y.add(y);
    return Y;
  }

  // General utilities
  int complexity(int[] X) {
    int sum = 0;
    for (int c = 0; c < C; c++) {
      assert X[c] >= bottomX[c] : c + " " + X[c] + " " + bottomX[c];
      sum += X[c] - bottomX[c];
    }
    return sum;
  }
  int[] copy(int[] X) {
    int[] newX = new int[C];
    System.arraycopy(X, 0, newX, 0, C);
    return newX;
  }
  void set(int[] X1, int[] X2) { System.arraycopy(X2, 0, X1, 0, C); }
  boolean eq(int[] X1, int[] X2) {
    for (int c = 0; c < C; c++)
      if (X1[c] != X2[c]) return false;
    return true;
  }
  boolean lessThanEq(int[] X1, int[] X2) {
    for (int c = 0; c < C; c++)
      if (X1[c] > X2[c]) return false;
    return true;
  }
  int findUniqueDiff(int[] X1, int[] X2) {
    int diffc = -1;
    for (int c = 0; c < C; c++) {
      int d = Math.abs(X1[c]-X2[c]);
      if (d > 1) return -1; // Not allowed
      if (d == 1) {
        if (diffc != -1) return -1; // Can't have two diff
        diffc = c;
      }
    }
    return diffc;
  }

  double logistic(double theta) { return 1/(1+Math.exp(-theta)); }
  double invLogistic(double mu) { return Math.log(mu/(1-mu)); }

  class Group {
    boolean done;
    boolean scanning;
    int[] lowerX;
    int[] upperX;
    HashSet<String> Y; // Unproven 
    double incrTheta; // For the step size
    HashMap<Integer,Integer> jobCounts; // job ID -> number of jobs in the queue at the time when this job was created

    boolean inRange(int[] X) { return lessThanEq(lowerX, X) && lessThanEq(X, upperX); }

    @Override public String toString() {
      String status = done ? "done" : (scanning ? "scan" : "rand");
      return String.format("Group(%s,%s<=|X|<=%s,|Y|=%s,incrProb=%.2f,#wait=%s)",
        status, complexity(lowerX), complexity(upperX), Y.size(), logistic(incrTheta), jobCounts.size());
    }

    public Group(HashSet<String> Y) {
      this.done = false;
      this.scanning = false;
      this.lowerX = copy(bottomX);
      this.upperX = copy(topX);
      this.Y = Y;
      this.incrTheta = invLogistic(initIncrProb);
      this.jobCounts = new HashMap();
    }

    public Group(Group g, HashSet<String> Y) {
      this.done = g.done;
      this.scanning = g.scanning;
      this.lowerX = copy(g.lowerX);
      this.upperX = copy(g.upperX);
      this.Y = Y;
      this.incrTheta = g.incrTheta;
      this.jobCounts = new HashMap(g.jobCounts);
    }

    boolean wantToLaunchJob() {
      if (done) return false;
      if (scanning) return jobCounts.size() == 0; // Don't parallelize
      return true;
    }

    Scenario createNewScenario() {
      double incrProb = logistic(incrTheta);
      EX.logs("createNewScenario %s: incrProb=%.2f", this, incrProb);
      if (scanning) {
        if (jobCounts.size() == 0) { // This is sequential - don't waste energy parallelizing
          int diff = complexity(upperX) - complexity(lowerX);
          assert diff > 0 : diff;
          int target_j = random.nextInt(diff);
          EX.logs("Scanning: dipping target_j=%s of diff=%s", target_j, diff);
          // Sample a minimal dip from upperX
          int j = 0;
          int[] X = new int[C];
          for (int c = 0; c < C; c++) {
            X[c] = lowerX[c];
            for (int i = lowerX[c]; i < upperX[c]; i++, j++)
              if (j != target_j) X[c]++;
          }
          return createScenario(X);
        }
        else {
          EX.logs("Scanning: not starting new job, still waiting for %s (shouldn't happen)", jobCounts.keySet());
          return null;
        }
      }
      else {
        // Sample a random element between the upper and lower bounds
        int[] X = new int[C];
        for (int c = 0; c < C; c++) {
          X[c] = lowerX[c];
          for (int i = lowerX[c]; i < upperX[c]; i++)
            if (random.nextDouble() < incrProb) X[c]++;
        }
        if (!eq(X, lowerX) && !eq(X, upperX)) // Don't waste time
          return createScenario(X);
        else
          return null;
      }
    }

    Scenario createScenario(int[] X) {
      //Scenario scenario = new Scenario(encodeX(X), encodeY(Y));
      Scenario scenario = new Scenario(encodeX(X), encodeY(allY())); // Always include all the queries, otherwise, it's unclear what the reference set is
      jobCounts.put(scenario.id, 1+jobCounts.size());
      return scenario;
    }

    void update(int id, int[] X, boolean unproven) {
      if (done) return;

      // Update refinement probability to make p(y=1) reasonable
      // Only update probability if we were responsible for launching this run
      // This is important in the initial iterations when getting data for updateLower to avoid polarization of probabilities.
      if (jobCounts.containsKey(id)) {
        double oldIncrProb = logistic(incrTheta);
        double singleTargetProb = Math.exp(-1); // Desired p(y=1)

        // Exploit parallelism: idea is that probability that two of the number of processors getting y=1 should be approximately p(y=1)
        double numProcessors = jobCounts.size(); // Approximate number of processors (for this group) with the number of things in the queue.
        double targetProb = 1 - Math.pow(1-singleTargetProb, 1.0/numProcessors);

        // Due to parallelism, we want to temper the amount of probability increment
        double stepSize = incrThetaStepSize / Math.sqrt(jobCounts.get(id)); // Size of jobCounts at the time the job was created
        if (!unproven) incrTheta -= (1-targetProb) * stepSize; // Proven -> cheaper abstractions
        else incrTheta += targetProb * stepSize; // Unproven -> more expensive abstractions

        EX.logs("    targetProb = %.2f (%.2f eff. proc), stepSize = %.2f/sqrt(%d) = %.2f, incrProb : %.2f -> %.2f [unproven=%s]",
            targetProb, numProcessors,
            incrThetaStepSize, jobCounts.get(id), stepSize,
            oldIncrProb, logistic(incrTheta), unproven);
        jobCounts.remove(id);
      }

      // Detect minimal dip: negative scenario that differs by upperX by one site (that site must be necessary)
      // This should only really be done in the scanning part
      if (unproven) {
        int c = findUniqueDiff(X, upperX);
        if (c != -1) {
          EX.logs("    updateLowerX %s: found that c=%s is necessary", this, components.get(c));
          lowerX[c] = upperX[c];
        }
      }
      else { // Proven
        EX.logs("    updateUpperX %s: reduced |upperX|=%s to |upperX|=%s", this, complexity(upperX), complexity(X));
        set(upperX, X);
      }

      if (scanning) {
        if (eq(lowerX, upperX)) {
          EX.logs("    DONE with group %s!", this);
          done = true;
        }
      }
      else {
        int lowerComplexity = complexity(lowerX);
        int upperComplexity = complexity(upperX);
        int diff = upperComplexity-lowerComplexity;

        if (upperComplexity == 1) { // Can't do better than 1
          EX.logs("    DONE with group %s!", this);
          done = true;
        }
        else if (diff <= scanThreshold) {
          EX.logs("    SCAN group %s now!", this);
          scanning = true;
        }
      }
    }
  }

  int sample(double[] weights) {
    double sumWeight = 0;
    for (double w : weights) sumWeight += w;
    double target = random.nextDouble() * sumWeight;
    double accum = 0;
    for (int i = 0; i < weights.length; i++) {
      accum += weights[i];
      if (accum >= target) return i;
    }
    throw new RuntimeException("Bad");
  }

  List<Group> getCandidateGroups() {
    List<Group> candidates = new ArrayList();
    for (Group g : groups)
      if (g.wantToLaunchJob())
        candidates.add(g);
    return candidates;
  }

  public Scenario createJob() {
    List<Group> candidates = getCandidateGroups();
    if (candidates.size() == 0) return null;
    // Sample a group proportional to the number of effects in that group
    // This is important in the beginning to break up the large groups
    double[] weights = new double[candidates.size()];
    for (int i = 0; i < candidates.size(); i++)
      weights[i] = candidates.get(i).Y.size();
    int chosen = sample(weights);
    Group g = candidates.get(chosen);
    return g.createNewScenario();
  }

  public boolean isDone() {
    for (Group g : groups)
      if (!g.done) return false;
    return true;
  }

  public void onJobResult(Scenario scenario) {
    incorporateScenario(scenario, true);
    outputStatus();
  }

  String render(int[] X, Set<String> Y) { return String.format("|X|=%s,|Y|=%s", complexity(X), Y.size()); }

  // Incorporate the scenario into all groups
  void incorporateScenario(Scenario scenario, boolean saveToDisk) {
    numScenarios++;
    if (saveToDisk) {
      PrintWriter f = Utils.openOutAppend(EX.path("scenarios"));
      f.println(scenario);
      f.close();
    }

    int[] X = decodeX(scenario.in);
    Set<String> Y = decodeY(scenario.out);

    EX.logs("Incorporating scenario id=%s,%s into %s groups (numScenarios = %s)", scenario.id, render(X, Y), groups.size(), numScenarios);
    List<Group> newGroups = new ArrayList();
    boolean changed = false;
    for (Group g : groups)
      changed |= incorporateScenario(scenario.id, X, Y, g, newGroups);
    groups = newGroups;
    if (!changed) // Didn't do anything - probably an outdated scenario
      EX.logs("  Useless: |X|=%s,|Y|=%s", complexity(X), Y.size());
  }

  // Incorporate into group g
  boolean incorporateScenario(int id, int[] X, Set<String> Y, Group g, List<Group> newGroups) {
    // Don't need this since Y is with respect to allY
    // Don't update on jobs we didn't ask for! (Important because we are passing around subset of queries which make sense only with respect to the group that launched the job)
    /*if (!g.jobCounts.containsKey(id)) {
      newGroups.add(g);
      return false;
    }*/

    if (!g.inRange(X)) { // We asked for this job, but now it's useless
      g.jobCounts.remove(id);
      newGroups.add(g);
      return false;
    }

    // Now we can make an impact
    EX.logs("  into %s", g);

    HashSet<String> Y0 = new HashSet();
    HashSet<String> Y1 = new HashSet();
    for (String y : g.Y) {
      if (Y.contains(y)) Y1.add(y);
      else               Y0.add(y);
    }
    if (Y0.size() == 0 || Y1.size() == 0) { // Don't split: all of Y still behaves the same
      assert !(Y0.size() == 0 && Y1.size() == 0); // At least one must be true
      g.update(id, X, Y1.size() > 0);
      newGroups.add(g);
    }
    else {
      Group g0 = new Group(g, Y0);
      Group g1 = new Group(g, Y1);
      g0.update(id, X, false);
      g1.update(id, X, true);
      newGroups.add(g0);
      newGroups.add(g1);
    }
    return true;
  }

  void outputStatus() {
    int numDone = 0, numScanning = 0;
    for (Group g : groups) {
      if (g.done) numDone++;
      else if (g.scanning) numScanning++;
    }

    EX.putOutput("numScenarios", numScenarios);
    EX.putOutput("numDoneGroups", numDone);
    EX.putOutput("numScanGroups", numScanning);
    EX.putOutput("numGroups", groups.size());

    // Print groups
    EX.logs("%s groups", groups.size());
    int sumComplexity = 0;
    int[] X = new int[C];
    for (Group g : groups) {
      EX.logs("  %s", g);
      sumComplexity += complexity(g.upperX);
      for (int c = 0; c < C; c++)
        X[c] = Math.max(X[c], g.upperX[c]);
    }
    EX.putOutput("sumComplexity", sumComplexity);
    EX.putOutput("complexity", complexity(X));

    EX.flushOutput();
  }

  public void saveState() {
    // Save to disk
    {
      PrintWriter out = Utils.openOut(EX.path("groups"));
      for (Group g : groups)
        out.println(logistic(g.incrTheta) + " ## " + encodeX(g.lowerX) + " ## " + encodeX(g.upperX) + " ## " + encodeY(g.Y));
      out.close();
    }
    {
      PrintWriter out = Utils.openOut(EX.path("groups.txt"));
      for (Group g : groups) {
        out.println("=== "+g);
        out.println("Sites ("+defaults+"):");
        for (int c = 0; c < C; c++)
          if (g.upperX[c] != bottomX[c])
            out.println("  "+components.get(c)+":"+g.upperX[c]+ " "+G.jstr(sites.get(c)));
        out.println("Queries:");
        for (String y : g.Y) {
          Query q = y2queries.get(y);
          out.println("  "+G.estr(q.e1)+" | "+G.estr(q.e2));
        }
      }
      out.close();
    }
  }

  public int maxWorkersNeeded() {
    // If everyone's scanning, just need one per scan
    // Otherwise, need as many workers as we can get.
    int n = 0;
    for (Group g : groups) {
      if (g.scanning) n++;
      else if (!g.done) return 10000;
    }
    return n;
  }
}
