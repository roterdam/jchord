/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 *
 * Pointer analysis which performs refinement and pruning using slivers.
 *
 * A context c [Ctxt] is a chain of allocation/call sites.
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

  DomV domV;
  DomM domM;
  DomI domI;
  DomH domH;
  DomE domE;

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
      buf.append(isAlloc(q) ? hstr(q) : istr(q));
    }
    buf.append('}');
    return buf.toString();
  }
  String fstr(jq_Field f) { return f.getDeclaringClass()+"."+f.getName(); }
  String vstr(Register v) { return v+"@"+mstr(domV.getMethod(v)); }
  String mstr(jq_Method m) { return m.getDeclaringClass().shortName()+"."+m.getName(); }

  boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }

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
  Set<Query> queries = new LinkedHashSet<Query>();

  // Format: returnAbs H3,I5,,I8 H3, E4,2 E6,2
  // CfromJC contains [H3]+[I5,I8] => [H3,I5]
  @Override public String toString() {
    StringBuilder buf = new StringBuilder();
    if (returnAbs) buf.append("returnAbs");
    for (Pair<Quad,Ctxt> pair : abs.CfromJC.keySet()) {
      Quad j = pair.val0;
      Ctxt c = pair.val1;
      Ctxt d = abs.CfromJC.get(pair);
      int k = d.length();
      if (buf.length() > 0) buf.append(' ');
      buf.append(j2str(j));
      if (k == 1) buf.append(',');
      for (int i = 0; i < c.length(); i++) {
        buf.append(',');
        buf.append(j2str(c.get(i)));
        if (k == i+2) buf.append(',');
      }
    }
    for (Query q : queries) {
      buf.append(' ');
      buf.append('E');
      buf.append(G.domE.indexOf(q.e1));
      buf.append(',');
      buf.append(G.domE.indexOf(q.e2));
    }
    return buf.toString();
  }

  String j2str(Quad j) { return G.isAlloc(j) ? "H"+G.domH.indexOf(j) : "I"+G.domI.indexOf(j); }

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
        String[] u = t.split(",");
        Quad[] js = new Quad[u.length-1];
        int i = 0;
        int k = -1;
        for (String a : u) {
          if (a.length() == 0) k = i; // cut here
          else if (a.charAt(0) == 'H')
            js[i++] = (Quad)G.domH.get(Integer.parseInt(a.substring(1)));
          else if (a.charAt(0) == 'I')
            js[i++] = G.domI.get(Integer.parseInt(a.substring(1)));
          else
            throw new RuntimeException("Bad: "+a);
        }
        assert i == js.length;
        assert k != -1;
        Ctxt fullc = new Ctxt(js);
        msg.abs.CfromJC.put(new Pair(fullc.head(), fullc.tail()), fullc.prefix(k));
      }
    }
    msg.abs.computeS();
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

class Status {
  int numProven;
  int absHashCode;
  String absSummary;
}

class Abstraction {
  @Override public int hashCode() { return S.hashCode(); }
  @Override public boolean equals(Object _that) {
    Abstraction that = (Abstraction)_that;
    return S.equals(that.S);
  }

  void add(Abstraction that, boolean assertDisjoint) {
    for (Pair<Quad,Ctxt> pair : that.CfromJC.keySet()) {
      //if (assertDisjoint) assert !CfromJC.containsKey(pair) : pair + " " + CfromJC.get(pair) + " " + that.CfromJC.get(pair); // too slow
      CfromJC.put(pair, that.CfromJC.get(pair));
    }
    S.addAll(that.S);
  }

  void computeS() {
    S.clear();
    for (Pair<Quad,Ctxt> pair : CfromJC.keySet()) {
      Quad j = pair.val0;
      Ctxt c = pair.val1;
      Ctxt d = CfromJC.get(pair);
      S.add(c);
      S.add(d);
    }
  }

  Histogram lengthHistogram(Set<Ctxt> slivers) {
    Histogram hist = new Histogram();
    for (Ctxt c : slivers) hist.counts[c.length()]++;
    return hist;
  }

  // Extract the sites which are not pruned
  Set<Quad> inducedSites() {
    Set<Quad> set = new HashSet<Quad>();
    for (Pair<Quad,Ctxt> pair : CfromJC.keySet())
      set.add(pair.val0);
    return set;
  }

  @Override public String toString() { return String.format("%s%s", S.size(), lengthHistogram(S)); }
  int size() { return S.size(); }

  HashMap<Pair<Quad,Ctxt>,Ctxt> CfromJC = new HashMap(); // j, c -> project([j]+c)
  HashSet<Ctxt> S = new HashSet<Ctxt>(); // Slivers: union of everything in CfromJC
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

  final Ctxt emptyCtxt = new Ctxt(new Quad[0]);

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

    this.verbose                 = X.getIntArg("verbose", 0);
    this.maxIters                = X.getIntArg("maxIters", 1);
    this.initK                   = X.getIntArg("initK", 0);
    this.useObjectSensitivity    = X.getBooleanArg("useObjectSensitivity", false);
    this.runAlwaysIncludePruned  = X.getBooleanArg("runAlwaysIncludePruned", false);
    this.inspectTransRels        = X.getBooleanArg("inspectTransRels", false);
    this.verifyAfterPrune        = X.getBooleanArg("verifyAfterPrune", false);
    this.masterHost              = X.getStringArg("masterHost", null);
    this.masterPort              = X.getIntArg("masterPort", 8888);
    this.useWorker               = X.getBooleanArg("useWorker", false);
    this.classifierPath          = X.getStringArg("classifierPath", null);
    this.learnClassifier         = X.getBooleanArg("learnClassifier", false);

    for (String name : X.getStringArg("taskName", "").split(","))
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
    if (!useObjectSensitivity)
      jSet.addAll(iSet);

    // jm: site to methods 
    for (Quad h : hSet) jm.put(h, new ArrayList<jq_Method>());
    if (!useObjectSensitivity) {
      for (Quad i : iSet) jm.put(i, new ArrayList<jq_Method>());
    }
    for (jq_Method m : G.domM) mj.put(m, new ArrayList<Quad>());

    // Extensions of sites depends on the target method
    if (!useObjectSensitivity) {
      ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("IM"); rel.load();
      PairIterable<Quad,jq_Method> result = rel.getAry2ValTuples();
      for (Pair<Quad,jq_Method> pair : result) {
        Quad i = pair.val0;
        jq_Method m = pair.val1;
        assert iSet.contains(i) : G.istr(i);
        jm.get(i).add(m);
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
    String focus = X.getStringArg("eFocus", null);
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
    for (Quad j : jSet) {
      Ctxt c = G.isAlloc(j) ? emptyCtxt.append(j) : emptyCtxt; // c = [j] (allocation sites) or [] (call sites)
      unprovenGroup.abs.CfromJC.put(new Pair(j, emptyCtxt), c); // j, [] => c=[j]
      //X.logs("CfromJC %s %s => %s", G.jstr(j), G.cstr(emptyCtxt), G.cstr(c));
      if (c.length() == 0) continue;
      for (jq_Method m : jm.get(j)) {
        for (Quad k : mj.get(m)) {
          Ctxt d = G.isAlloc(k) ? emptyCtxt.append(k) : emptyCtxt; // d = [k] (allocation sites) or [] (call sites)
          //X.logs("CfromJC %s %s => %s", G.jstr(k), G.cstr(c), G.cstr(d));
          unprovenGroup.abs.CfromJC.put(new Pair(k, c), d); // k, c => d
        }
      }
    }
    unprovenGroup.abs.computeS();
    for (int i = 0; i < initK; i++) {
      X.logs("  at length %s", i);
      unprovenGroup.refineAbstraction();
    }
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
          assert provenGroup.abs.S.contains(emptyCtxt);
          assert provenGroup.abs.inducedSites().equals(jSet);
          provenGroup.runAnalysis(runRelevantAnalysis);
        }
      }

      outputStatus(iter);

      if (statuses.get(statuses.size()-1).numProven == allQueries.size()) {
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

  int numProven() {
    int n = 0;
    for (QueryGroup g : provenGroups)
      n += g.queries.size();
    return n;
  }

  // For visualization
  class RelNode {
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

    int numProven = numProven();
    Status status = new Status();
    status.numProven = numProven;
    status.absHashCode = unprovenGroup.abs.hashCode();
    status.absSummary = unprovenGroup.abs.toString();
    statuses.add(status);

    X.putOutput("currIter", iter);
    X.putOutput("absSize", S.size());
    X.putOutput("numQueries", allQueries.size());
    X.putOutput("numProven", numProven);
    X.putOutput("numUnproven", allQueries.size()-numProven);
    X.putOutput("numProvenHistory", numProvenHistory());
    X.flushOutput();
  }

  String numProvenHistory() {
    StringBuilder buf = new StringBuilder();
    for (Status s : statuses) {
      if (buf.length() > 0) buf.append(',');
      buf.append(s.numProven);
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
        abs.add(prunedAbs, true);

      // Domain (these are the slivers)
      DomC domC = (DomC) ClassicProject.g().getTrgt("C");
      assert abs.S.contains(emptyCtxt);
      domC.clear();
      domC.getOrAdd(emptyCtxt); // This must go first!
      for (Ctxt c : abs.S) domC.add(c);
      domC.save();

      // Relations
      ProgramRel relInQuery = (ProgramRel) ClassicProject.g().getTrgt("inQuery");
      relInQuery.zero();
      for (Query q : queries)
        relInQuery.add(q.e1, q.e2);
      relInQuery.save();

      ProgramRel relHfromC = (ProgramRel) ClassicProject.g().getTrgt("HfromC");
      relHfromC.zero();
      for (Ctxt c : abs.S) {
        if (c.length() > 0 && G.isAlloc(c.head()))
          relHfromC.add(c.head(), c);
      }
      relHfromC.save();

      ProgramRel relCfromHC = (ProgramRel) ClassicProject.g().getTrgt("CfromHC");
      ProgramRel relCfromIC = (ProgramRel) ClassicProject.g().getTrgt("CfromIC");
      relCfromHC.zero();
      relCfromIC.zero();
      for (Pair<Quad,Ctxt> pair : abs.CfromJC.keySet()) {
        Quad j = pair.val0;
        Ctxt c = pair.val1;
        Ctxt d = abs.CfromJC.get(pair);
        //X.logs("REL %s %s %s", j, c, d);
        (G.isAlloc(j) ? relCfromHC : relCfromIC).add(d, j, c);
      }
      relCfromHC.save();
      relCfromIC.save();

      ProgramRel relobjI = (ProgramRel) ClassicProject.g().getTrgt("objI");
      relobjI.zero();
      if (useObjectSensitivity) {
        for (Quad i : iSet) relobjI.add(i);
      }
      relobjI.save();

      ClassicProject.g().resetTrgtDone(domC); // Make everything that depends on domC undone
      ClassicProject.g().setTaskDone(SliverCtxtsAnalysis.this); // We are generating all this stuff, so mark it as done...
      ClassicProject.g().setTrgtDone(domC);
      ClassicProject.g().setTrgtDone(relInQuery);
      ClassicProject.g().setTrgtDone(relCfromHC);
      ClassicProject.g().setTrgtDone(relCfromIC);
      ClassicProject.g().setTrgtDone(relobjI);

      for (FeedbackTask task : tasks)
        ClassicProject.g().runTask(task.name);
      if (runRelevantAnalysis) ClassicProject.g().runTask(mainTask.relevantName());
      if (inspectTransRels) ClassicProject.g().runTask(mainTask.transName());
    }

    void pruneAbstraction() {
      // From Datalog, read out the pruned abstraction
      Abstraction newAbs = new Abstraction();
      {
        ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("r_CfromHC"); rel.load();
        TrioIterable<Ctxt,Quad,Ctxt> result = rel.getAry3ValTuples();
        for (Trio<Ctxt,Quad,Ctxt> trio : result)
          newAbs.CfromJC.put(new Pair<Quad,Ctxt>(trio.val1, trio.val2), trio.val0);
        rel.close();
      }
      {
        ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("r_CfromIC"); rel.load();
        TrioIterable<Ctxt,Quad,Ctxt> result = rel.getAry3ValTuples();
        for (Trio<Ctxt,Quad,Ctxt> trio : result)
          newAbs.CfromJC.put(new Pair<Quad,Ctxt>(trio.val1, trio.val2), trio.val0);
        rel.close();
      }
      newAbs.computeS();
      X.logs("STATUS pruneAbstraction: %s -> %s", abs, newAbs);

      // Record the pruned slivers (abs - newAbs)
      for (Pair<Quad,Ctxt> pair : abs.CfromJC.keySet()) {
        if (newAbs.CfromJC.containsKey(pair)) continue; // Not pruned
        if (!runAlwaysIncludePruned)
          assert !prunedAbs.CfromJC.containsKey(pair);
        prunedAbs.CfromJC.put(pair, abs.CfromJC.get(pair)); // Add to pruned
      }
      prunedAbs.computeS();

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
      provenGroup.abs.add(prunedAbs, true); // Build up complete abstraction
      provenGroup.abs.add(abs, !runAlwaysIncludePruned); // if runAlwaysIncludePruned = true, will be duplicates, so don't assert
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
      // TODO: use learning to refine selectively
      String oldAbsStr = abs.toString();

      List<Ctxt> newCtxts = new ArrayList<Ctxt>();
      for (Pair<Quad,Ctxt> pair : abs.CfromJC.keySet()) {
        Quad j = pair.val0;
        Ctxt c = pair.val1;
        Ctxt d = abs.CfromJC.get(pair);
        if (d.length() == 1+c.length()) continue; // Already exact, no refinements

        // d is a prefix of [j]+c
        Ctxt newd = c.prepend(j, d.length()+1);
        assert !abs.S.contains(newd) : G.jstr(j) + " | " + G.cstr(c) + " | " + G.cstr(d) + " | " + G.cstr(newd);
        abs.CfromJC.put(pair, newd); // newd is one longer than d
        newCtxts.add(newd);
      }
      abs.computeS(); 

      // Need to specify how to grow the new contexts
      for (Ctxt c : newCtxts) { // [j]+c -> projection onto current S
        for (jq_Method m : jm.get(c.head())) {
          for (Quad j : mj.get(m)) {
            Ctxt d = extend(j, c, abs.S);
            if (d != null && d.length() > 0) abs.CfromJC.put(new Pair(j, c), d);
          }
        }
      }
      String newAbsStr = abs.toString();

      X.logs("STATUS refineAbstraction: %s -> %s", oldAbsStr, newAbsStr);
    }
  }

  Ctxt extend(Quad j, Ctxt c, Set<Ctxt> ref) {
    // Create d = [j, c] but truncate to length k, where k is the maximum such that d is in ref
    for (int k = c.length()+1; k >= 0; k--) { // Try to extend
      Ctxt d = c.prepend(j, k);
      if (ref.contains(d)) return d;
    }
    return null;
  }

  // For the proven queries, find a minimal abstraction.
  void findMinimalAbstractions() {
    X.logs("findMinimalAbstractions: %s groups", provenGroups.size());
    assert useWorker;

    synchronized(msgQueue) {
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
