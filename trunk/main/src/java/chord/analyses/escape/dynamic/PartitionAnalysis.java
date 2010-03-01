package chord.analyses.escape.dynamic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Collections;

import joeq.Compiler.Quad.Quad;

import chord.util.IntArraySet;
import chord.util.ChordRuntimeException;
import chord.project.Properties;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.Project;
import chord.program.Program;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

/**
 * Evaluate the precision and complexity of various heap abstractions.
 *
 * Two types of abstraction:
 *  - Update abstraction (used to do weak updates):
 *    Build the graph over abstract values obtained from this abstraction.
 *    Supports allocation sites.
 *  - Snapshot abstraction (used to do strong updates):
 *    Build the graph over concrete values (note that this enables us to do very strong updates).
 *    We then apply the abstraction at various snapshots of the graph to answer queries (very inefficient).
 *    Supports abstractions above plus reachability.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(name = "partition-java")
public class PartitionAnalysis extends DynamicAnalysis {
  static final int ARRAY_FIELD = -2;
  static final int THREAD_FIELD = -3;
  static final int NULL_OBJECT = 0;

  InstrScheme instrScheme;

  // Execution management/logging
  Execution X = new Execution();
  HashMap<Object,Object> output = new LinkedHashMap<Object,Object>();

  // Parameters of the analysis
  int verbose;
  boolean useStrongUpdates;
  LocalAbstraction updateAbstraction;
  GlobalAbstraction snapshotAbstraction;
  double queryFrac;
  Random random;
  int kCFA; // Number of call sites keep in k-CFA
  int kOS; // Number of object allocation sites to keep in k-OS
  boolean pointedTo; // For reachability
  boolean matchFirstField; // For reachability
  boolean matchLastField; // For reachability

  // We have a graph over abstract values (determined by updateAbstraction)
  TIntIntHashMap o2h = new TIntIntHashMap(); // o (object ID) -> heap allocation site h
  TIntObjectHashMap<Object> o2v = new TIntObjectHashMap<Object>(); // o (object ID) -> abstract value v
  IndexMap<Object> a2v = new IndexHashMap<Object>(); // a (node ID) -> abstract value v (along with reverse map)
  List<List<Edge>> a2edges = new ArrayList<List<Edge>>(); // a (node ID) -> list of outgoing edges from a
  int N; // Number of nodes

  TIntHashSet staticEscapedNodes = new TIntHashSet(); // set of a (node ID) [static nodes: monotonically growing (even with strong updates)]
  TIntHashSet escapedNodes = new TIntHashSet(); // set of a (node ID) [updated if using strong updates]

  TIntObjectHashMap<ThreadInfo> threadInfos = new TIntObjectHashMap<ThreadInfo>(); // t (thread) -> ThreadInfo

  // For each query, we maintain counts (number of escaping and non-escaping over the run)
  HashMap<Query, QueryResult> queryResults = new HashMap<Query, QueryResult>();

  LocalAbstraction parseLocalAbstraction(String abstractionType) {
    if (abstractionType.equals("none")) return new NoneLocalAbstraction();
    if (abstractionType.equals("alloc")) return new AllocLocalAbstraction();
    throw new RuntimeException("Unknown: "+abstractionType+" (possibilities: none|alloc)");
  }
  GlobalAbstraction parseGlobalAbstraction(String abstractionType) {
    if (abstractionType.startsWith("wrap:")) return new WrappedGlobalAbstraction(parseLocalAbstraction(abstractionType.substring(5)));
    if (abstractionType.equals("reachability")) return new ReachabilityGlobalAbstraction();
    throw new RuntimeException("Unknown: "+abstractionType+" (possibilities: wrap:<local>|reachability)");
  }

  String getStringArg(String key, String defaultValue) {
    return System.getProperty("chord.partition."+key, defaultValue);
  }
  boolean getBooleanArg(String key, boolean defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : s.equals("true");
  }
  int getIntArg(String key, int defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Integer.parseInt(s);
  }
  double getDoubleArg(String key, double defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Double.parseDouble(s);
  }

	public void run() {
    try {
      // Parse options
      verbose = getIntArg("verbose", 0);
      useStrongUpdates = getBooleanArg("useStrongUpdates", false);

      updateAbstraction = parseLocalAbstraction(getStringArg("updateAbstraction", ""));
      snapshotAbstraction = parseGlobalAbstraction(getStringArg("snapshotAbstraction", ""));

      if (useStrongUpdates && !(updateAbstraction instanceof NoneLocalAbstraction))
        throw new RuntimeException("Can only use strong updates with no abstract interpretation.  Use snapshots to have strong updates and abstractions.");

      queryFrac = getDoubleArg("queryFrac", 1.0);
      random = new Random(getIntArg("random", 1));

      kCFA = getIntArg("kCFA", 0);
      kOS = getIntArg("kOS", 0);
      pointedTo = getBooleanArg("pointedTo", false);
      matchFirstField = getBooleanArg("matchFirstField", false);
      matchLastField = getBooleanArg("matchLastField", false);

      // Save options
      HashMap<Object,Object> options = new LinkedHashMap<Object,Object>();
      options.put("program", System.getProperty("chord.work.dir"));
      options.put("verbose", verbose);
      options.put("useStrongUpdates", useStrongUpdates);
      options.put("updateAbstraction", updateAbstraction);
      options.put("snapshotAbstraction", snapshotAbstraction);
      options.put("kCFA", kCFA);
      options.put("kOS", kOS);
      options.put("queryFrac", queryFrac);
      X.writeMap("options.map", options);

      super.run();

      // Save output
      X.writeMap("output.map", output);
    } catch (Throwable t) {
      X.logs("ERROR: %s", t);
      for (StackTraceElement e : t.getStackTrace())
        X.logs("  %s", e);
      throw new RuntimeException(t);
    }
	}

  QueryResult queryResult(int e, int b) {
    Query q = new Query(e);
    QueryResult qr = queryResults.get(q);
    if (qr == null) {
      queryResults.put(q, qr = new QueryResult());
      qr.selected = random.nextDouble() < queryFrac; // Choose to answer this query with some probability
    }
    return qr;
  }

  void outputQueries() {
    PrintWriter out = X.openOut(X.path("queries.out"));
    StatFig fig = new StatFig();
    for (Query q : queryResults.keySet()) {
      QueryResult qr = queryResults.get(q);
      if (qr.selected) {
        out.println(String.format("%s | %s %s", estr(q.e), qr.numEscaping, qr.numNonEscaping));
        fig.add(qr.numEscaping + qr.numNonEscaping);
      }
      else
        out.println(String.format("%s | ?", estr(q.e)));
    }
    out.close();
    output.put("query.numHits", fig.mean());
    X.logs("  # hits per query: %s (%s total hits)", fig, fig.n);
  }

  ThreadInfo threadInfo(int t) {
    if (t == -1) return null;
    ThreadInfo info = threadInfos.get(t);
    if (info == null)
      threadInfos.put(t, info = new ThreadInfo());
    return info;
  }

  // The global abstraction partitions the nodes in the current graph, where each partition has the same value.
  // Then we want to compute whether a given node is reachable from a some node in initEscapedNodes
  // via 1) edges in the graph or 2) jumps between nodes with the same value.
  class SnapshotAnalysis {
    HashMap<Object, List<Integer>> v2as = new HashMap(); // abstract value to set of nodes
    GlobalAbstraction abstraction;

    public SnapshotAnalysis(GlobalAbstraction abstraction) {
      this.abstraction = abstraction;
      abstraction.init();

      // Compute mapping from nodes to abstract values
      for (int a = 0; a < N; a++) {
        Object value = abstraction.get(a);
        Utils.add(v2as, value, a);
      }
    }

    // Complexity of this abstraction
    int complexity() { return v2as.size(); } // Number of abstract values

    boolean reachable(int a, int dest, TIntHashSet visitedNodes) {
      if (a == dest) return true;
      if (visitedNodes.contains(a)) return false;
      visitedNodes.add(a);

      // Try following edges in the graph
      for (Edge e : a2edges.get(a))
        if (reachable(e.b, dest, visitedNodes)) return true;
      // Try jumping to nodes with the same value
      Object value = abstraction.get(a);
      for (int b : v2as.get(value))
        if (reachable(b, dest, visitedNodes)) return true;
      return false;
    }

    void followEscapeValues(int a, TIntHashSet visitedNodes, Set<Object> escapedValues) {
      if (visitedNodes.contains(a)) return;
      visitedNodes.add(a);
      Object value = abstraction.get(a);
      escapedValues.add(value);
      for (Edge e : a2edges.get(a))
        followEscapeValues(e.b, visitedNodes, escapedValues);
    }

    // Does node a escape under the given global abstraction?
    public boolean escapes(int a) {
      for (int start : staticEscapedNodes.toArray())
        if (reachable(start, a, new TIntHashSet())) return true;
      return false;
    }

    public void run() {
      // Which nodes are escaping under the abstraction?
      TIntHashSet abstractionEscapedNodes = new TIntHashSet();
      for (int start : staticEscapedNodes.toArray())
        reachable(start, -1, abstractionEscapedNodes);

      int n0 = staticEscapedNodes.size();
      X.logs("=== Snapshot abstraction %s (at end) ===", abstraction);
      X.logs("  complexity: %d values", complexity());
      X.logs("  precision: %d/%d = %.2f",
        escapedNodes.size()-n0, abstractionEscapedNodes.size()-n0,
        1.0*(escapedNodes.size()-n0)/(abstractionEscapedNodes.size()-n0));
      output.put("finalSnapshot.numTrueEscape", escapedNodes.size()-n0);
      output.put("finalSnapshot.numPropEscape", abstractionEscapedNodes.size()-n0);
      output.put("finalSnapshot.precision", 1.0*(escapedNodes.size()-n0)/(abstractionEscapedNodes.size()-n0));
      output.put("complexity", complexity());
    }
  }

  public InstrScheme getInstrScheme() {
    if (instrScheme != null) return instrScheme;
    instrScheme = new InstrScheme();

    instrScheme.setEnterAndLeaveMethodEvent();
    instrScheme.setEnterAndLeaveLoopEvent();

    instrScheme.setNewAndNewArrayEvent(true, true, true); // h, t, o

	  instrScheme.setGetstaticPrimitiveEvent(true, true, true, true); // e, t, b, f
	  instrScheme.setGetstaticReferenceEvent(true, true, true, true, true); // e, t, b, f, o
	  instrScheme.setPutstaticPrimitiveEvent(true, true, true, true); // e, t, b, f
    instrScheme.setPutstaticReferenceEvent(true, true, true, true, true); // e, t, b, f, o

    instrScheme.setGetfieldPrimitiveEvent(true, true, true, true); // e, t, b, f
    instrScheme.setPutfieldPrimitiveEvent(true, true, true, true); // e, t, b, f
    instrScheme.setGetfieldReferenceEvent(true, true, true, true, true); // e, t, b, f, o
    instrScheme.setPutfieldReferenceEvent(true, true, true, true, true); // e, t, b, f, o

    instrScheme.setAloadPrimitiveEvent(true, true, true, true); // e, t, b, i
    instrScheme.setAstorePrimitiveEvent(true, true, true, true); // e, t, b, i
    instrScheme.setAloadReferenceEvent(true, true, true, true, true); // e, t, b, i, o
    instrScheme.setAstoreReferenceEvent(true, true, true, true, true); // e, t, b, i, o

    instrScheme.setThreadStartEvent(true, true, true); // i, t, o
    instrScheme.setThreadJoinEvent(true, true, true); // i, t, o

    instrScheme.setAcquireLockEvent(true, true, true); // l, t, o
    instrScheme.setReleaseLockEvent(true, true, true); // r, t, o
    instrScheme.setWaitEvent(true, true, true); // i, t, o
    instrScheme.setNotifyEvent(true, true, true); // i, t, o

    instrScheme.setMethodCallEvent(true, true, true, true, true); // i, t, o, before, after

    instrScheme.setReturnPrimitiveEvent(true, true); // i, t
    instrScheme.setReturnReferenceEvent(true, true, true); // i, t, o

    instrScheme.setExplicitThrowEvent(true, true, true); // p, t, o
    instrScheme.setImplicitThrowEvent(true, true); // p, t

    instrScheme.setQuadEvent();
    instrScheme.setBasicBlockEvent();

    return instrScheme;
  }

  public void initAllPasses() {
    int E = instrumentor.getEmap().size();
    int H = instrumentor.getHmap().size();
    int F = instrumentor.getFmap().size();
    X.logs("initAllPasses: |E| = %s, |H| = %s, |F| = %s", E, H, F);
  }

  public void doneAllPasses() {
    X.logs("===== Results using updateAbstraction = %s, useStrongUpdates = %s =====", updateAbstraction, useStrongUpdates);

    // Evaluate on queries (real metric)
    int numEscaping = 0;
    for (QueryResult qr : queryResults.values())
      if (qr.escapes()) numEscaping++;

    X.logs("  %d/%d = %.2f queries proposed to be escaping",
      numEscaping, queryResults.size(), 1.0*numEscaping/queryResults.size());
    output.put("query.numEscape", numEscaping);
    output.put("query.numTotal", queryResults.size());
    output.put("query.fracEscape", 1.0*numEscaping/queryResults.size());
    outputQueries();

    if (!(updateAbstraction instanceof NoneLocalAbstraction) || !useStrongUpdates)
      X.logs("    (run with updateAbstraction = none and useStrongUpdates = true to get number actually escaping; divide to get precision of %s)", updateAbstraction);

    // Evaluate on final nodes (don't really care about this)
    for (int a = 0; a < N; a++) // Propagate escapes on nodes (hasn't been done if weak updates)
      if (staticEscapedNodes.contains(a)) followEscapeNodes(a);
    int numEscape = 0;
    for (int a = 0; a < N; a++)
      if (escapedNodes.contains(a)) numEscape++;
    int n0 = staticEscapedNodes.size();
    X.logs("  %d/%d = %.2f nodes proposed to be escaping at end (not including %d static nodes)",
        numEscape-n0, N-n0, 1.0*(numEscape-n0)/(N-n0), n0);
    output.put("finalNodes.numEscape", numEscape-n0);
    output.put("finalNodes.numTotal", N-n0);
    output.put("finalNodes.fracEscape", 1.0*(numEscape-n0)/(N-n0));
    output.put("finalObjects.numTotal", o2v.size());

    if (verbose >= 5) {
      X.logs("  staticEscapedNodes (static nodes to be ignored): %s", nodes_str(staticEscapedNodes));
      X.logs("  escapedNodes: %s", nodes_str(escapedNodes));
    }

    new SnapshotAnalysis(snapshotAbstraction).run();
  }

  //////////////////////////////

  int getNode(int t, int o) {
    //if (o == -1) return -1;
    if (o == NULL_OBJECT) return -1;
    Object v = o2v.get(o);
    if (v == null) o2v.put(o, v = updateAbstraction.get(t, o));
    int a = a2v.getOrAdd(v);
    if (a == N) {
      if (verbose >= 1) X.logs("NEWNODE o=%s => a=%s, v=%s", ostr(o), a, v);
      snapshotAbstraction.record(a, t, o);
      a2edges.add(new ArrayList<Edge>());
      N++;
    }
    return a;
  }

  void addEdge(int t, int oa, int f, int ob) {
    int a = getNode(t, oa);
    int b = getNode(t, ob);
    // Strong update: remove existing field pointer
    List<Edge> edges = a2edges.get(a);
    if (useStrongUpdates) {
      assert (updateAbstraction instanceof NoneLocalAbstraction); // Otherwise we have to materialize...
      for (int i = 0; i < edges.size(); i++) {
        if (edges.get(i).f == f) {
          edges.remove(i);
          break;
        }
      }
    }
    int numNewEscape = 0;
    if (b != -1) {
      edges.add(new Edge(f, b));
      if (!useStrongUpdates) {
        if (escapedNodes.contains(a))
          numNewEscape = followEscapeNodes(b);
      }
    }
    if (verbose >= 1)
      X.logs("ADDEDGE b=%s (%s) f=%s o=%s (%s): %s new nodes escaped", ostr(oa), astr(a), fstr(f), ostr(ob), astr(b), numNewEscape);
  }
  // Weak update: contaminate everyone downstream
  // Return number of new nodes marked as escaping
  int followEscapeNodes(int a) {
    if (escapedNodes.contains(a)) return 0;
    int n = 1;
    escapedNodes.add(a);
    for (Edge e : a2edges.get(a))
      n += followEscapeNodes(e.b);
    return n;
  }

  // Should only be called for nodes corresponding to static fields (which don't get counted)
  void setEscape(int a) {
    if (verbose >= 1) X.logs("SETESCAPE a=%s", astr(a));
    staticEscapedNodes.add(a);
    if (!useStrongUpdates) escapedNodes.add(a);
  }

  void makeQuery(int e, int t, int o) {
    // If we are using strong updates, then escapeNodes is not getting updated, so we can't answer queries (fast).
    QueryResult result = queryResult(e, o);
    if (result.selected) {
      int a = a2v.indexOf(o2v.get(o));
      if (!useStrongUpdates) // Weak updates: we are updating escape all the time, so easy to answer this query
        result.add(escapedNodes.contains(a));
      else {
        SnapshotAnalysis analysis = new SnapshotAnalysis(snapshotAbstraction);
        result.add(analysis.escapes(a)); // This is expensive - use the snapshot
      }
      if (verbose >= 1) X.logs("QUERY e=%s, t=%s, o=%s: result = %s", estr(e), tstackstr(t), ostr(o), result);
    }
  }

  public String nodes_str(TIntHashSet nodes) {
    StringBuilder buf = new StringBuilder();
    for (int a : nodes.toArray()) {
      if (buf.length() > 0) buf.append(' ');
      buf.append(astr(a));
    }
    return buf.toString();
  }

  String astr(int a) { return a == -1 ? "(none)" : a+":"+a2v.get(a); } // node
  String fstr(int f) { // field
    if (f == THREAD_FIELD) return "[T]";
    if (f == ARRAY_FIELD) return "[*]";
    return f == -1 ? "-" : instrumentor.getFmap().get(f);
  }
  String hstr(int h) { return h == -1 ? "-" : instrumentor.getHmap().get(h); } // heap allocation site
  String estr(int e) {
    if (e == -1) return "-";
    Quad quad = (Quad)instrumentor.getDomE().get(e);
    return Program.v().toJavaPosStr(quad)+" "+Program.v().toQuadStr(quad);
  }
  String mstr(int m) { return m == -1 ? "-" : instrumentor.getMmap().get(m); } // method
  String wstr(int w) { return w == -1 ? "-" : instrumentor.getWmap().get(w); } // loop
  String istr(int i) { return i == -1 ? "-" : instrumentor.getImap().get(i); } // call site
  String ostr(int o) { return o == -1 ? "-" : (o == NULL_OBJECT ? "null" : "O"+o); } // concrete object
  String tstr(int t) { return t == -1 ? "-" : "T"+t; } // thread
  String pstr(int p) { return p == -1 ? "-" : instrumentor.getPmap().get(p); } // simple statement?
  String stackstr(int t) {
    Stack<Integer> stack = threadInfo(t).callStack;
    return stack.size() == 0 ? "(empty)" : stack.size()+":"+mstr(stack.peek());
  }
  String tstackstr(int t) { return t == -1 ? "-" : tstr(t)+"["+stackstr(t)+"]"; }

  ////////////////////////////////////////////////////////////
  // Handlers

  public void processEnterMethod(int m, int t) {
    if (verbose >= 6) X.logs("EVENT enterMethod: m=%s, t=%s", mstr(m), tstackstr(t));
    threadInfo(t).callStack.push(m);
  }
  public void processLeaveMethod(int m, int t) {
    if (verbose >= 6) X.logs("EVENT leaveMethod: m=%s, t=%s", mstr(m), tstackstr(t));
    assert (threadInfo(t).callStack.pop() == m);
  }

  public void processEnterLoop(int w, int t) {
    if (verbose >= 5) X.logs("EVENT enterLoop: w=%s", w);
  }
  public void processLeaveLoop(int w, int t) {
    if (verbose >= 5) X.logs("EVENT leaveLoop: w=%s", w);
  }

  public void processNewOrNewArray(int h, int t, int o) { // new Object
    o2h.put(o, h);
    if (verbose >= 5) X.logs("EVENT new: h=%s, t=%s, o=%s", hstr(h), tstackstr(t), ostr(o));
    getNode(t, o); // Force creation of a new node
  }

  public void processGetstaticPrimitive(int e, int t, int b, int f) { }
  public void processGetstaticReference(int e, int t, int b, int f, int o) { // ... = b.f, where b.f = o and b is static
    if (verbose >= 5) X.logs("EVENT getStaticReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstackstr(t), ostr(b), fstr(f), ostr(o));
  }
  public void processPutstaticPrimitive(int e, int t, int b, int f) { }
  public void processPutstaticReference(int e, int t, int b, int f, int o) { // b.f = o, where b is static
    if (verbose >= 5) X.logs("EVENT putStaticReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstackstr(t), ostr(b), fstr(f), ostr(o));
    setEscape(getNode(t, b));
    addEdge(t, b, f, o);
  }

  public void processGetfieldPrimitive(int e, int t, int b, int f) {
    makeQuery(e, t, b);
  }
  public void processGetfieldReference(int e, int t, int b, int f, int o) { // ... = b.f, where b.f = o
    if (verbose >= 5) X.logs("EVENT getFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstackstr(t), ostr(b), fstr(f), ostr(o));
    makeQuery(e, t, b);
  }
  public void processPutfieldPrimitive(int e, int t, int b, int f) {
    makeQuery(e, t, b);
  }
  public void processPutfieldReference(int e, int t, int b, int f, int o) { // b.f = o
    if (verbose >= 5) X.logs("EVENT putFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstackstr(t), ostr(b), fstr(f), ostr(o));
    makeQuery(e, t, b);
    addEdge(t, b, f, o);
  }

  public void processAloadPrimitive(int e, int t, int b, int i) {
    makeQuery(e, t, b);
  }
  public void processAloadReference(int e, int t, int b, int i, int o) {
    if (verbose >= 5) X.logs("EVENT loadReference: e=%s, t=%s, b=%s, i=%s, o=%s", estr(e), tstackstr(t), ostr(b), i, ostr(o));
    makeQuery(e, t, b);
  }
  public void processAstorePrimitive(int e, int t, int b, int i) {
    makeQuery(e, t, b);
  }
  public void processAstoreReference(int e, int t, int b, int i, int o) {
    if (verbose >= 5) X.logs("EVENT storeReference: e=%s, t=%s, b=%s, i=%s, o=%s", estr(e), tstackstr(t), ostr(b), i, ostr(o));
    makeQuery(e, t, b);
    addEdge(t, b, ARRAY_FIELD, o);
  }

  // In o.start() acts like g_{t,i} = o, where g_{ti} is like a global variable specific to thread t.
  int threadToObjId(int i, int t) {
    return 100000 + 100*i + t;
  }

  public void processThreadStart(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadStart: i=%s, t=%s, o=%s", istr(i), tstackstr(t), ostr(o));
    int b = threadToObjId(i, t);
    setEscape(getNode(t, b));
    addEdge(t, b, THREAD_FIELD, o);
  }
  public void processThreadJoin(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadJoin: i=%s, t=%s, o=%s", istr(i), tstackstr(t), ostr(o));
    int b = threadToObjId(i, t);
    addEdge(t, b, THREAD_FIELD, NULL_OBJECT);
  }

  public void processAcquireLock(int l, int t, int o) { }
  public void processReleaseLock(int r, int t, int o) { }
  public void processWait(int i, int t, int o) { }
  public void processNotify(int i, int t, int o) { }

  public void processMethodCallBef(int i, int t, int o) {
    if (verbose >= 5) X.logs("EVENT methodCallBefore: i=%s, t=%s, o=%s", istr(i), tstackstr(t), ostr(o));
    threadInfo(t).callSites.push(i);
    threadInfo(t).callAllocs.push(o2h.get(o));
  }
  public void processMethodCallAft(int i, int t, int o) {
    if (verbose >= 5) X.logs("EVENT methodCallAfter: i=%s, t=%s, o=%s", istr(i), tstackstr(t), ostr(o));
    int ii = threadInfo(t).callSites.pop();
    assert (ii == i);
    int hh = threadInfo(t).callAllocs.pop();
    assert (hh == o2h.get(o));
  }

  public void processReturnPrimitive(int p, int t) { }
  public void processReturnReference(int p, int t, int o) { }
  public void processExplicitThrow(int p, int t, int o) { }
  public void processImplicitThrow(int p, int t, int o) { }

  public void processQuad(int p, int t) {
    if (verbose >= 7) X.logs("EVENT processQuad p=%s, t=%s", pstr(p), tstackstr(t));
  }
  public void processBasicBlock(int b, int t) { }

  ////////////////////////////////////////////////////////////
  // Abstractions

  // Local abstractions only depend on the object (used for abstract interpretation),
  // but can depend on the current thread (get information aobut where the object was allocated).
  abstract class LocalAbstraction {
    public abstract Object get(int t, int o);
  }

  class NoneLocalAbstraction extends LocalAbstraction {
    public String toString() { return "none"; }
    public Object get(int t, int o) { return o; }
  }

  class AllocLocalAbstraction extends LocalAbstraction {
    public String toString() {
      return String.format("alloc(kCFA=%d,kOS=%d)", kCFA, kOS);
    }

    public Object get(int t, int o) {
      ThreadInfo info = threadInfo(t);
      if (kCFA == 0 && kOS == 0) return o2h.get(o); // 0-CFA

      StringBuilder buf = new StringBuilder();
      buf.append(o2h.get(o));

      if (kCFA > 0) {
        for (int i = 0; i < kCFA; i++) {
          int j = info.callSites.size() - i - 1;
          if (j < 0) break;
          buf.append('_');
          buf.append(info.callSites.get(j));
        }
      }

      if (kOS > 0) {
        for (int i = 0; i < kCFA; i++) {
          int j = info.callAllocs.size() - i - 1;
          if (j < 0) break;
          buf.append('_');
          buf.append(info.callAllocs.get(j));
        }
      }

      return buf.toString();
    }
  }

  // Global abstractions depend on the graph (used for snapshots).
  abstract class GlobalAbstraction {
    public abstract Object get(int a); // Based on the node in the graph
    public void record(int a, int t, int o) { }
    public void init() { }
  }

  class WrappedGlobalAbstraction extends GlobalAbstraction {
    LocalAbstraction abstraction; // Just use this local abstraction
    TIntObjectHashMap<Object> a2v = new TIntObjectHashMap<Object>(); // a (node ID) -> abstract value v
    WrappedGlobalAbstraction(LocalAbstraction abstraction) { this.abstraction = abstraction; }
    public String toString() { return abstraction.toString(); }

    public Object get(int a) { return a2v.get(a); }
    @Override public void record(int a, int t, int o) {
      a2v.put(a, abstraction.get(t, o));
    }
  }

  class ReachabilityGlobalAbstraction extends GlobalAbstraction {
    @Override public String toString() {
      if (pointedTo) return "reach(point)";
      if (matchFirstField) return "reach(first_f)";
      if (matchLastField) return "reach(last_f)";
      return "reach";
    }

    // Ideally, we would have a set here, but that's too expensive
    ArrayList<String>[] a2vs;
    //String[] a2v; // Too sloppy

    // Source: variable or heap-allocation site.
    // General recipe for predicates is some function of the list of fields from source to the node.
    // Examples:
    //   Reachable-from: path must exist.
    //   Pointed-to-by: length must be one.
    //   Reachable-from-with-first-field
    //   Reachable-from-with-last-field
    //  Note that a node can fire on many predicates.  We just choose one arbitrarily for now.
    @Override public void init() {
      //a2v = new String[N];
      a2vs = new ArrayList[N];
      for (int a = 0; a < N; a++) a2vs[a] = new ArrayList<String>();
      
      // For each node, get it's source
      for (int a = 0; a < N; a++) {
        int o = a2o(a);
        String source = "H"+o2h.get(o);
        //X.logs("--- source=%s, a=%s", source, astr(a));
        search(source, -1, -1, 0, a);
      }
    }

    int a2o(int a) {
      assert (updateAbstraction instanceof NoneLocalAbstraction);
      return (Integer)PartitionAnalysis.this.a2v.get(a);
    }

    void search(String source, int first_f, int last_f, int len, int a) {
      //if (a2v[a] != null) return; // Already have a predicate
      
      String v = null;
      if (len > 0) { // Avoid trivial predicates
        if (pointedTo) {
          if (len == 1) v = source;
        }
        else if (matchFirstField)
          v = source+"."+first_f+".*";
        else if (matchLastField)
          v = source+".*."+last_f;
        else // Plain reachability
          v = source;
      }

      if (v != null && a2vs[a].indexOf(v) != -1) return; // Already have it

      if (v != null) a2vs[a].add(v);
      //X.logs("source=%s first_f=%s last_f=%s len=%s a=%s: v=%s", source, fstr(first_f), fstr(last_f), len, astr(a), a2vs[a]);

      if (pointedTo && len >= 1) return;

      // Recurse
      for (Edge e : a2edges.get(a)) {
        search(source, len == 0 ? e.f : first_f, e.f, len+1, e.b);
      }
    }

    public Object get(int a) {
      if (staticEscapedNodes.contains(a)) return "-";
      Collections.sort(a2vs[a]); // Canonicalize
      //int h = o2h.get(a2o(a));
      //return h + ":" + a2vs[a].toString();
      return a2vs[a].toString();
    }
  }
}

////////////////////////////////////////////////////////////

// Pointer via field f to node b
class Edge {
  public Edge(int f, int b) {
    this.f = f;
    this.b = b;
  }
  int f;
  int b;
}

class Utils {
  public static <S, T> void add(Map<S, List<T>> map, S key1, T key2) {
    List<T> s = map.get(key1);
    if(s == null) map.put(key1, s = new ArrayList<T>());
    s.add(key2);
  }
}

// Query for thread escape: is the object pointed to by the relvant variable thread-escaping at program point e?
class Query {
  Query(int e) { this.e = e; }
  int e; // Program point
  @Override public boolean equals(Object _that) {
    Query that = (Query)_that;
    return this.e == that.e;
  }
  @Override public int hashCode() {
    return e;
  }
}

class QueryResult {
  boolean selected; // Whether we are trying to answer this query or not
  int numEscaping = 0;
  int numNonEscaping = 0;

  boolean escapes() { return numEscaping > 0; } // Escapes if any escapes
  void add(boolean b) {
    if (b) numEscaping++;
    else numNonEscaping++;
  }

  @Override public String toString() { return numEscaping+"|"+numNonEscaping; }
}

class ThreadInfo {
  Stack<Integer> callStack = new Stack(); // Elements are methods m (for visualization)
  Stack<Integer> callSites = new Stack(); // Elements are call sites i (for kCFA)
  Stack<Integer> callAllocs = new Stack(); // Elements are object allocation sites h (for kOS)
}

class Execution {
  public Execution() {
    System.out.println(new File(".").getAbsolutePath());
    for (int i = 0; ; i++) {
      basePath = System.getProperty("chord.partition.execPoolPath", ".")+"/"+i+".exec";
      if (!new File(basePath).exists()) break;
    }
    System.out.println("Execution directory: "+basePath);
    new File(basePath).mkdir();
    logOut = openOut(path("log"));
    
    String view = System.getProperty("chord.partition.addToView", null);
    if (view != null) {
      PrintWriter out = openOut(path("addToView"));
      out.println(view);
      out.close();
    }
  }
  String path(String name) { return basePath+"/"+name; }

  PrintWriter openOut(String path) {
    try {
      return new PrintWriter(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void logs(String format, Object... args) {
    logOut.println(String.format(format, args));
    logOut.flush();
  }

  public void writeMap(String name, HashMap<Object,Object> map) {
    PrintWriter out = openOut(path(name));
    for (Object key : map.keySet()) {
      out.println(key+"\t"+map.get(key));
    }
    out.close();
  }

  String basePath;
	PrintWriter logOut;
}

class StatFig {
  double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, sum = 0;
  int n = 0;

  double mean() { return sum/n; }

  void add(double x) {
    sum += x;
    n += 1;
    min = Math.min(min, x);
    max = Math.max(max, x);
  }

  @Override public String toString() {
    return String.format("%.2f / %.2f / %.2f (%d)", min, sum/n, max, n);
  }
}
