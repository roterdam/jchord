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

class Message {
  boolean returnAbs;
  Abstraction abs = new Abstraction();
  Set<Query> queries = new LinkedHashSet<Query>(); // If input, queries to prove; if output, queries that were not proven

  String j2str(Quad j) { return j == null ? "*" : (G.isAlloc(j) ? "H"+G.domH.indexOf(j) : "I"+G.domI.indexOf(j)); }
  // Format:
  //   [returnAbs] <site>,...,<site>[:<number of levels of additional refinement>] E<e1>,<e2>
  //   <site> = H<int> | I<int> | *
  // Example: returnAbs H3,I5 H3,I5:2 E4,2 E6,2

  @Override public String toString() {
    StringBuilder buf = new StringBuilder();
    if (returnAbs) buf.append("returnAbs");
    for (Ctxt c : abs.S) { // Output all the slivers
      if (buf.length() > 0) buf.append(' ');
      for (int i = 0; i < c.length(); i++) {
        if (i > 0) buf.append(',');
        buf.append(j2str(c.get(i)));
      }
    }
    for (Query q : queries) { // Output all the queries
      buf.append(' ');
      buf.append('E');
      buf.append(G.domE.indexOf(q.e1));
      buf.append(',');
      buf.append(G.domE.indexOf(q.e2));
    }
    return buf.toString();
  }

  static Message parse(String line) {
    Message msg = new Message();
    for (String t : line.split(" ")) {
      if (t.equals("returnAbs")) msg.returnAbs = true;
      else if (t.startsWith("E")) {
        String[] u = t.substring(1).split(",");
        Quad e1 = G.domE.get(Integer.parseInt(u[0]));
        Quad e2 = G.domE.get(Integer.parseInt(u[1]));
        msg.queries.add(new Query(e1, e2));
      }
      else {
        int j = t.indexOf(':');
        int addk = 0;
        if (j != -1) { addk = Integer.parseInt(t.substring(j+1)); t = t.substring(0, j); }
        String[] u = t.split(",");
        Quad[] js = new Quad[u.length];
        for (int i = 0; i < u.length; i++) {
          String a = u[i];
          if (a.equals("*"))
            js[i] = null;
          if (a.charAt(0) == 'H')
            js[i] = (Quad)G.domH.get(Integer.parseInt(a.substring(1)));
          else if (a.charAt(0) == 'I')
            js[i] = G.domI.get(Integer.parseInt(a.substring(1)));
          else
            throw new RuntimeException("Bad: "+a);
        }
        Ctxt c = new Ctxt(js);
        if (G.isSummary(c))
          msg.abs.addRefinements(c, addk);
        else {
          assert addk == 0;
          msg.abs.S.add(c);
        }
      }
    }
    return msg;
  }
}

////////////////////////////////////////////////////////////

// A run is an interaction with the static analysis.
class Run {
  Message in, out;
}

// A minimal abstraction for a query that we can learn from.
// Each scenario corresponds to a query and the set of sites that need to be refined to prove the query.
// Produced by a sequence of runs.
class Scenario {
  Quad e1, e2; // For this query
  // These sites need to be refined
  HashSet<Quad> relevantH;
  HashSet<Quad> relevantI;
}

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

  void add(Abstraction that) { S.addAll(that.S); }

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

  void addRefinements(Ctxt c, int addk) {
    assert addk >= 0;
    assert !S.contains(c) && G.isSummary(c) && G.summaryLen(c) > 0;
    if (addk == 0) S.add(c);
    else {
      Ctxt d = G.atomize(c);
      S.add(d);
      for (Quad j : G.rev_jm.get(d.last().getMethod())) // Append possible j's
        addRefinements(G.summarize(d.append(j)), addk-1);
    }
  }

  // Return the minimum set of slivers in S that covers sliver c (covering defined with respect to set of chains)
  Ctxt project(Ctxt c) {
    if (G.isAtom(c)) { // atom
      if (S.contains(c)) return c; // Exact match
      for (int k = G.atomLen(c); k >= 0; k--) { // Take length k prefix
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      return null;
    }
    else { // summary
      // WARNING: assuming the result is unique!
      // This is valid only if all the summary slivers are of the same length, so that
      // if we project ab* (by prepending a to b*) onto {ab,abc*,abd*,...}, we should return all 3 values.
      for (int k = G.summaryLen(c); k >= 0; k--) { // Take length k prefix (consider {ab*, a*, *}, exactly one should match)
        Ctxt d = G.summarize(c.prefix(k));
        if (S.contains(d)) return d;
      }
      // To be correct, now we need to take every sliver that starts with ab, summary or not
      return null;
    }
  }

  void assertIsValid() {
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

  HashSet<Ctxt> S = new HashSet<Ctxt>(); // The set of slivers
}

class FeedbackTask {
  public FeedbackTask(String name) {
    this.name = name;
    assert name.endsWith("dlog") : name;
  }
  String name;
  String initName()     { return name.replace("dlog", "init-dlog"); }
  String relevantName() { return name.replace("dlog", "relevant-dlog"); }
  String transName()    { return name.replace("dlog", "trans-dlog"); }
}

////////////////////////////////////////////////////////////

@Chord(
  name = "sliver-ctxts-java",
  produces = { "C", "HfromC", "CfromHC", "CfromIC", "objI", "inQuery" },
  namesOfTypes = { "C" },
  types = { DomC.class }
)
public class SliverCtxtsAnalysis extends JavaAnalysis {
  private Execution X;

  // Options
  int verbose;
  int maxIters;
  int initK;
  boolean useObjectSensitivity;
  boolean runAlwaysIncludePruned;
  boolean inspectTransRels;
  boolean verifyAfterPrune;
  String masterHost;
  int masterPort;
  boolean isWorker() { return masterHost != null; }
  boolean useWorker;
  String classifierPath; // Path to classifier
  boolean useClassifier() { return classifierPath != null; }
  boolean learnClassifier; // Path to data to collected data
  List<FeedbackTask> tasks = new ArrayList<FeedbackTask>();
  FeedbackTask mainTask;

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

  ////////////////////////////////////////////////////////////

  // Initialization to do anything.
  private void init() {
    X = Execution.v();

    G = new GlobalInfo();
    G.rev_jm = rev_jm;

    this.verbose                 = X.getIntArg("verbose", 0);
    this.maxIters                = X.getIntArg("maxIters", 1);
    this.initK                   = X.getIntArg("initK", 1);
    this.useObjectSensitivity    = X.getBooleanArg("useObjectSensitivity", false);
    this.runAlwaysIncludePruned  = X.getBooleanArg("runAlwaysIncludePruned", false);
    this.inspectTransRels        = X.getBooleanArg("inspectTransRels", false);
    this.verifyAfterPrune        = X.getBooleanArg("verifyAfterPrune", false);
    this.masterHost              = X.getStringArg("masterHost", null);
    this.masterPort              = X.getIntArg("masterPort", 8888);
    this.useWorker               = X.getBooleanArg("useWorker", false);
    this.classifierPath          = X.getStringArg("classifierPath", null);
    this.learnClassifier         = X.getBooleanArg("learnClassifier", false);

    assert initK > 0;
    for (String name : X.getStringArg("taskNames", "").split(","))
      this.tasks.add(new FeedbackTask(name));
    this.mainTask = tasks.get(tasks.size()-1);

    X.putOption("version", 1);
    X.putOption("program", System.getProperty("chord.work.dir"));
    X.flushOptions();

    for (FeedbackTask task : tasks)
      ClassicProject.g().runTask(task.initName());

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
    else {
      Master master;
      if (useWorker) { master = new Master(); master.start(); }
      refinePruneLoop();
      if (learnClassifier) {
        findMinimalAbstractions();
      }
    }
    finish();
  }

  String callMaster(String line) {
    try {
      Socket master = new Socket(masterHost, masterPort);
      writeLine(master, line);
      return readLine(master);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  void writeLine(Socket s, String line) {
    try {
      PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
      out.println(line);
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  String readLine(Socket s) {
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
      String line = in.readLine();
      in.close();
      return line;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void runWorker() {
    X.logs("Starting worker...");
    while (true) {
      // Get a job
      String line = callMaster("GET");
      if (line.equals("EXIT")) break;
      else if (line.equals("WAIT")) { }
      else {
        Message msg = Message.parse(line);

        QueryGroup group = new QueryGroup();
        group.abs = msg.abs;
        group.queries = msg.queries;
        group.runAnalysis(msg.returnAbs);
        group.removeProvenQueries();
        if (msg.returnAbs)
          group.pruneAbstraction();

        msg.abs = group.abs;
        msg.queries = group.queries;

        line = callMaster("PUT "+msg.toString());
        X.logs("Sent result to master, got reply: %s", line);
      }

      sleep();
    }
  }

  void sleep() {
    try { Thread.sleep(5000); } catch(InterruptedException e) { }
  }

  LinkedList<Message> msgQueue = new LinkedList<Message>(); // Inputs to send to worker
  HashMap<Integer,Message> msgIds = new HashMap<Integer,Message>(); // When send worker, move from queue to this
  int newId = 0;
  List<Run> runs = new ArrayList<Run>();

  class Master extends Thread {
    @Override public void run() {
      X.logs("Starting master (listening at port %s)", masterPort);
      try {
        ServerSocket master = new ServerSocket(masterPort);
        while (true) {
          Socket worker = master.accept();
          X.logs("Got connection from worker %s", worker);
          String cmd = readLine(worker);
          if (cmd.equals("GET")) {
            X.logs("  GET (we have %s msgs)", msgQueue.size());
            if (msgQueue.size() == 0) writeLine(worker, "WAIT");
            else {
              Message msg;
              synchronized(msgQueue) {
                msg = msgQueue.removeFirst();
                msgIds.put(newId++, msg);
              }
              writeLine(worker, msg.toString());
            }
          }
          else if (cmd.startsWith("PUT")) {
            String[] tokens = cmd.split(" ", 3); // PUT <ID> <...>
            if (msgQueue.size() == 0) writeLine(worker, "WAIT");
            int id = Integer.parseInt(tokens[1]);
            X.logs("  PUT id=%s", id);
            Run run = new Run();
            synchronized(msgQueue) {
              run.in = msgIds.remove(id);
              run.out = Message.parse(tokens[2]);
              runs.add(run);
            }
            writeLine(worker, "OK");
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  void refinePruneLoop() {
    X.logs("Initializing abstraction with length %s slivers (|jSet|=%s)", initK, jSet.size());
    // Initialize with k-CFA
    // Slivers are length k for call sites and k+1 for allocation sites
    unprovenGroup.abs.S.add(G.emptyCtxt);
    for (Quad j : jSet)
      unprovenGroup.abs.addRefinements(G.summarize(G.emptyCtxt.append(j)), initK-1);

    X.logs("Unproven group with %s queries", allQueries.size());
    unprovenGroup.queries.addAll(allQueries);

    for (int iter = 1; ; iter++) {
      X.logs("====== Iteration %s", iter);
      boolean runRelevantAnalysis = iter < maxIters;
      unprovenGroup.runAnalysis(runRelevantAnalysis);
      backupRelations(iter);
      if (inspectTransRels) unprovenGroup.inspectAnalysisOutput();
      QueryGroup provenGroup = unprovenGroup.removeProvenQueries();
      if (provenGroup != null) provenGroups.add(provenGroup);
      if (runRelevantAnalysis) {
        unprovenGroup.pruneAbstraction();
        if (verifyAfterPrune && provenGroup != null) {
          X.logs("verifyAfterPrune");
          // Make sure this is a complete abstraction
          assert provenGroup.abs.S.contains(G.emptyCtxt);
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
    HashSet<Ctxt> S = new HashSet<Ctxt>();
    for (QueryGroup g : provenGroups) {
      S.addAll(g.abs.S);
      assert g.prunedAbs.S.size() == 0;
    }
    S.addAll(unprovenGroup.prunedAbs.S);
    S.addAll(unprovenGroup.abs.S);
   
    X.addSaveFiles("abstraction.S."+iter);
    {
      PrintWriter out = OutDirUtils.newPrintWriter("abstraction.S."+iter);
      for (Ctxt a : S)
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
    X.putOutput("absSize", S.size());
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

      // Include pruned abstractions (ensures soundness)
      // These should get pruned out again
      if (runAlwaysIncludePruned)
        abs.add(prunedAbs);

      // Domain (these are the slivers)
      DomC domC = (DomC) ClassicProject.g().getTrgt("C");
      assert abs.S.contains(G.emptyCtxt);
      domC.clear();
      domC.getOrAdd(G.emptyCtxt); // This must go first!
      for (Ctxt c : abs.S) domC.add(c);
      domC.save();

      // Relations
      ProgramRel relHfromC = (ProgramRel) ClassicProject.g().getTrgt("HfromC");
      relHfromC.zero();
      for (Ctxt c : abs.S) {
        if (G.hasHeadSite(c) && G.isAlloc(c.head()))
          relHfromC.add(c.head(), c);
      }
      relHfromC.save();

      ProgramRel relCfromHC = (ProgramRel) ClassicProject.g().getTrgt("CfromHC");
      ProgramRel relCfromIC = (ProgramRel) ClassicProject.g().getTrgt("CfromIC");
      relCfromHC.zero();
      relCfromIC.zero();
      for (Ctxt c : abs.S) { // From sliver c...
        if (G.hasHeadSite(c)) {
          for (jq_Method m : jm.get(c.head())) {
            for (Quad j : mj.get(m)) { // Extend with site j that could be prepended
              Ctxt d = abs.project(c.prepend(j));
              if (d != null)
                (G.isAlloc(j) ? relCfromHC : relCfromIC).add(d, j, c);
            }
          }
        }
        else {
          for (Quad j : jSet) { // Extend with any site j
            Ctxt d = abs.project(c.prepend(j));
            if (d != null)
              (G.isAlloc(j) ? relCfromHC : relCfromIC).add(d, j, c);
          }
        }
      }
      relCfromHC.save();
      relCfromIC.save();

      ProgramRel relobjI = (ProgramRel) ClassicProject.g().getTrgt("objI");
      relobjI.zero();
      if (useObjectSensitivity) {
        for (Quad i : iSet) relobjI.add(i);
      }
      relobjI.save();

      ProgramRel relInQuery = (ProgramRel) ClassicProject.g().getTrgt("inQuery");
      relInQuery.zero();
      for (Query q : queries)
        relInQuery.add(q.e1, q.e2);
      relInQuery.save();

      ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
      ClassicProject.g().setTaskDone(SliverCtxtsAnalysis.this); // We are generating all this stuff, so mark it as done...
      ClassicProject.g().setTrgtDone(domC);
      ClassicProject.g().setTrgtDone(relHfromC);
      ClassicProject.g().setTrgtDone(relCfromHC);
      ClassicProject.g().setTrgtDone(relCfromIC);
      ClassicProject.g().setTrgtDone(relobjI);
      ClassicProject.g().setTrgtDone(relInQuery);

      for (FeedbackTask task : tasks)
        ClassicProject.g().runTask(task.name);
      if (runRelevantAnalysis) ClassicProject.g().runTask(mainTask.relevantName());
      if (inspectTransRels) ClassicProject.g().runTask(mainTask.transName());
    }

    void pruneAbstraction() {
      // From Datalog, read out the pruned abstraction
      Abstraction newAbs = new Abstraction();
      for (String relName : new String[] {"r_CfromHC", "r_CfromIC"}) {
        ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(relName); rel.load();
        TrioIterable<Ctxt,Quad,Ctxt> result = rel.getAry3ValTuples();
        for (Trio<Ctxt,Quad,Ctxt> trio : result) {
          newAbs.S.add(trio.val0);
          newAbs.S.add(trio.val2);
        }
        rel.close();
      }
      X.logs("STATUS pruneAbstraction: %s -> %s", abs, newAbs);

      // Record the pruned slivers (abs - newAbs)
      for (Ctxt c : abs.S) {
        if (newAbs.S.contains(c)) continue; // Not pruned
        if (!runAlwaysIncludePruned) assert !prunedAbs.S.contains(c);
        prunedAbs.S.add(c); // Add to pruned
      }

      abs = newAbs;
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
      if (!runAlwaysIncludePruned) assert abs.size()+prunedAbs.size() == provenGroup.abs.size(); // No duplicates
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
        String dlogPath = ((DlogAnalysis)ClassicProject.g().getTask(mainTask.transName())).getFileName();
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

      HashSet<Ctxt> oldS = new HashSet<Ctxt>(abs.S);
      abs.S.clear();
      for (Ctxt c : oldS) { // For each sliver
        if (G.isAtom(c)) abs.S.add(c); // Leave atomic ones alone (already precise as possible)
        else abs.addRefinements(c, 1);
      }

      String newAbsStr = abs.toString();

      X.logs("STATUS refineAbstraction: %s -> %s", oldAbsStr, newAbsStr);
    }
  }

  // For the proven queries, find a minimal abstraction.
  void findMinimalAbstractions() {
    X.logs("findMinimalAbstractions: %s groups", provenGroups.size());
    assert useWorker;

    synchronized (msgQueue) {
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

        String[] names = new String[] { "CfromHC", "CfromIC", "inQuery", "outQuery" };
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
