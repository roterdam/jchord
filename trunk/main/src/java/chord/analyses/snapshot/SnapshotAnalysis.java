package chord.analyses.snapshot;

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
import joeq.Class.jq_Class;

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
import gnu.trove.TLongIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

/**
 * Evaluate the precision and complexity of various heap abstractions.
 * Build a graph over the concrete heap.
 * Abstractions are functions that operate on snapshots of the concrete heap.
 * Client will override this class.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public abstract class SnapshotAnalysis extends DynamicAnalysis {
  public abstract String propertyName();

  static final int ARRAY_FIELD = 88888;
  static final int NULL_OBJECT = 0;

  InstrScheme instrScheme;

  // Execution management/logging
  Execution X = new Execution();

  // Parameters of the analysis
  int verbose;
  boolean useStrongUpdates;
  Abstraction abstraction;
  double queryFrac, snapshotFrac;
  Random selectQueryRandom;
  Random selectSnapshotRandom;
  int kCFA; // Number of call sites keep in k-CFA
  int kOS; // Number of object allocation sites to keep in k-OS (note: this is not k-object sensitivity)
  ReachabilityAbstraction.Spec reachabilitySpec = new ReachabilityAbstraction.Spec();
  GraphMonitor graphMonitor;

  // We have a graph over abstract values (determined by updateAbstraction); each node impliciting representing a set of objects
  State state = new State();
  TIntObjectHashMap<ThreadInfo> threadInfos = new TIntObjectHashMap<ThreadInfo>(); // thread t -> ThreadInfo
  Set<jq_Class> excludedClasses = new HashSet<jq_Class>();

  HashMap<Query, QueryResult> queryResults = new HashMap<Query, QueryResult>();
  int numQueryHits;
  //StatFig snapshotPrecision = new StatFig();

  public Abstraction parseAbstraction(String abstractionType) {
    if (abstractionType.equals("none")) return new NoneAbstraction();
    if (abstractionType.equals("alloc")) return new AllocAbstraction(kCFA, kOS);
    if (abstractionType.equals("recency")) return new RecencyAbstraction();
    if (abstractionType.equals("reachability")) return new ReachabilityAbstraction(reachabilitySpec);
    throw new RuntimeException("Unknown abstraction: "+abstractionType+" (possibilities: none|alloc|recency|reachability)");
  }

  public String getStringArg(String key, String defaultValue) {
    return System.getProperty("chord.partition."+key, defaultValue);
  }
  public boolean getBooleanArg(String key, boolean defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : s.equals("true");
  }
  public int getIntArg(String key, int defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Integer.parseInt(s);
  }
  public double getDoubleArg(String key, double defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Double.parseDouble(s);
  }

	public void run() {
    boolean success = false;
    try {
      // Parse options
      verbose = getIntArg("verbose", 0);

      queryFrac = getDoubleArg("queryFrac", 1.0);
      snapshotFrac = getDoubleArg("snapshotFrac", 0.0);
      selectQueryRandom = new Random(getIntArg("selectQueryRandom", 1));
      selectSnapshotRandom = new Random(getIntArg("selectSnapshotRandom", 1));

      kCFA = getIntArg("kCFA", 0);
      kOS = getIntArg("kOS", 0);
      reachabilitySpec.pointedTo = getBooleanArg("pointedTo", false);
      reachabilitySpec.matchRepeatedFields = getBooleanArg("matchRepeatedFields", false);
      reachabilitySpec.matchFirstField = getBooleanArg("matchFirstField", false);
      reachabilitySpec.matchLastField = getBooleanArg("matchLastField", false);

      useStrongUpdates = getBooleanArg("useStrongUpdates", false);
      abstraction = parseAbstraction(getStringArg("abstraction", ""));

      graphMonitor = new SerializingGraphMonitor(X.path("graph"), getIntArg("graph.maxCommands", 100000));

      // Save options
      HashMap<Object,Object> options = new LinkedHashMap<Object,Object>();
      options.put("program", System.getProperty("chord.work.dir"));
      options.put("property", propertyName());
      options.put("verbose", verbose);
      options.put("useStrongUpdates", useStrongUpdates);
      options.put("abstraction", abstraction);
      options.put("queryFrac", queryFrac);
      options.put("snapshotFrac", snapshotFrac);
      X.writeMap("options.map", options);
      X.output.put("exec.status", "running");

      super.run();

      // Save output
      X.output.put("exec.status", "done");
      success = true;
    } catch (Throwable t) {
      X.output.put("exec.status", "failed");
      X.errors("%s", t);
      for (StackTraceElement e : t.getStackTrace())
        X.logs("  %s", e);
    }
    X.finish();
    if (!success) System.exit(1);
	}

  public void computedExcludedClasses() {
    String[] checkExcludedPrefixes = Properties.toArray(Properties.checkExcludeStr);
    Program program = Program.v();
    for (jq_Class c : program.getPreparedClasses()) {
      String cName = c.getName();
      for (String prefix : checkExcludedPrefixes) {
        if (cName.startsWith(prefix))
          excludedClasses.add(c);
      }
    }
  }
  public boolean isExcluded(int e) {
    Quad q = (Quad)instrumentor.getDomE().get(e);
    jq_Class c = Program.v().getMethod(q).getDeclaringClass();
    return excludedClasses.contains(c);
  }


  public ThreadInfo threadInfo(int t) {
    if (t == -1) return null;
    ThreadInfo info = threadInfos.get(t);
    if (info == null)
      threadInfos.put(t, info = new ThreadInfo());
    return info;
  }

  // The global abstraction partitions the nodes in the current graph, where each partition has the same value.
  /*abstract class Snapshot {
    public Snapshot() {
      abstraction.
    }

    int complexity() { return state.w2as.size(); } // Complexity of this abstraction (number of abstract values)

    public boolean computeIsTrue(int a) { return state.computeIsTrue(a); } // Expensive

    public void runFinal() {
      propertyState.computeAll();
      int actualNumTrue = propertyState.numTrue();

      state.computeAll();
      int propNumTrue = state.numTrue();

      if (graphMonitor != null) {
        for (int a = 0; a < N; a++) {
          boolean actualTrue = propertyState.isTrue(a);
          boolean propTrue = state.isTrue(a);
          String color = null;
          if (actualTrue && propTrue) color = "#00ff00"; // Good
          else if (!actualTrue && propTrue) color = "#ff0000"; // Bad (false positive)
          else if (!actualTrue && !propTrue) color = "#ffffff"; // Good
          else throw new RuntimeException("Got true negative - shouldn't happen (snapshot analysis is broken)");
          // Get the abstraction value (either local or global)
          String label = (abstraction instanceof NoneAbstraction) ? a2v.get(a).toString() : state.getAbstraction(a).toString();
          graphMonitor.setNodeLabel(a, label);
          graphMonitor.setNodeColor(a, color);
        }
      }

      X.logs("=== Snapshot abstraction %s (at end) ===", abstraction);
      X.logs("  complexity: %d values", complexity());
      X.logs("  precision: %d/%d = %.2f", actualNumTrue, propNumTrue, 1.0*actualNumTrue/propNumTrue);
      X.output.put("finalSnapshot.actualNumTrue", actualNumTrue);
      X.output.put("finalSnapshot.propNumTrue", propNumTrue);
      X.output.put("finalSnapshot.precision", 1.0*actualNumTrue/propNumTrue);
      X.output.put("complexity", complexity());
      if (propNumTrue > 0) snapshotPrecision.add(1.0*actualNumTrue/propNumTrue);

      PrintWriter out = Utils.openOut(X.path("snapshot-abstractions"));
      for (Object w : state.w2as.keySet())
        out.println(w);
      out.close();
    }
  }*/

  public InstrScheme getInstrScheme() {
    if (instrScheme != null) return instrScheme;
    instrScheme = new InstrScheme();

    //instrScheme.setEnterAndLeaveMethodEvent();
    //instrScheme.setEnterAndLeaveLoopEvent();

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
    //instrScheme.setThreadJoinEvent(true, true, true); // i, t, o

    //instrScheme.setAcquireLockEvent(true, true, true); // l, t, o
    //instrScheme.setReleaseLockEvent(true, true, true); // r, t, o
    //instrScheme.setWaitEvent(true, true, true); // i, t, o
    //instrScheme.setNotifyEvent(true, true, true); // i, t, o

    instrScheme.setMethodCallEvent(true, true, true, true, true); // i, t, o, before, after

    //instrScheme.setReturnPrimitiveEvent(true, true); // i, t
    //instrScheme.setReturnReferenceEvent(true, true, true); // i, t, o

    //instrScheme.setExplicitThrowEvent(true, true, true); // p, t, o
    //instrScheme.setImplicitThrowEvent(true, true); // p, t

    //instrScheme.setQuadEvent();
    //instrScheme.setBasicBlockEvent();

    return instrScheme;
  }

  public boolean shouldAnswerQueryHit(Query query) {
    return queryResult(query).selected;
  }
  public void answerQuery(Query query, boolean isTrue) {
    queryResult(query).add(isTrue);
    numQueryHits++;
  }
  private QueryResult queryResult(Query q) {
    QueryResult qr = queryResults.get(q);
    if (qr == null) {
      queryResults.put(q, qr = new QueryResult());
      qr.selected = selectQueryRandom.nextDouble() < queryFrac; // Choose to answer this query with some probability
    }
    return qr;
  }
  public void outputQueries() {
    PrintWriter out = Utils.openOut(X.path("queries.out"));
    StatFig fig = new StatFig();
    for (Query q : queryResults.keySet()) {
      QueryResult qr = queryResults.get(q);
      if (qr.selected) {
        out.println(String.format("%s | %s %s", q, qr.numTrue, qr.numFalse));
        fig.add(qr.numTrue + qr.numFalse);
      }
      else
        out.println(String.format("%s | ?", q));
    }
    out.close();
    X.output.put("query.numHits", fig.mean());
    X.logs("  # hits per query: %s (%s total hits)", fig, fig.n);
  }

  public void initAllPasses() {
    int E = instrumentor.getEmap().size();
    int H = instrumentor.getHmap().size();
    int F = instrumentor.getFmap().size();
    X.logs("initAllPasses: |E| = %s, |H| = %s, |F| = %s", E, H, F);

    abstraction.X = X;
    abstraction.state = state;
  }

  public void doneAllPasses() {
    // Evaluate on queries (real metric)
    int numTrue = 0;
    int numSelected = 0;
    for (QueryResult qr : queryResults.values()) {
      if (!qr.selected) continue;
      if (qr.isTrue()) numTrue++;
      numSelected++;
    }

    X.logs("  %d total queries; %d/%d = %.2f queries proposed to have property %s",
      queryResults.size(), numTrue, numSelected, 1.0*numTrue/numSelected, propertyName());
    X.output.put("query.numTrue", numTrue);
    X.output.put("query.numSelected", numSelected);
    X.output.put("query.numTotal", queryResults.size());
    X.output.put("query.fracTrue", 1.0*numTrue/numSelected);
    outputQueries();

    /*if (!(updateAbstraction instanceof NoneAbstraction) || !useStrongUpdates)
      X.logs("    (run with updateAbstraction = none and useStrongUpdates = true to get number actually escaping; divide to get precision of %s)", updateAbstraction);

    // Evaluate on final nodes: how many nodes have that property
    if (useStrongUpdates) propertyState.computeAll(); // Need to still do this
    numTrue = 0;
    for (int a = 0; a < N; a++)
      if (propertyState.isTrue(a)) numTrue++;
    X.logs("  %d/%d = %.2f nodes proposed to be escaping at end", numTrue, N, 1.0*numTrue/N);
    X.output.put("finalNodes.numTrue", numTrue);
    X.output.put("finalNodes.numTotal", N);
    X.output.put("finalNodes.fracTrue", 1.0*numTrue/N);
    X.output.put("finalObjects.numTotal", o2v.size());*/

    //new SnapshotAnalysis(snapshotAbstraction).runFinal();

    //X.logs("  snapshot precision: %s", snapshotPrecision);
    //X.output.put("snapshotPrecision", snapshotPrecision.mean());
    //X.output.put("query.totalNumHits", numQueryHits);

    if (graphMonitor != null) graphMonitor.finish();
  }

  //////////////////////////////
  // Override these graph construction handlers (remember to call super though)

  public void nodeCreated(int t, int o) {
    assert (o >= 0);
    if (o == NULL_OBJECT) return;
    if (state.o2edges.containsKey(o)) return; // Already exists
    state.o2h.putIfAbsent(o, -1); // Just in case we didn't get an allocation site
    state.o2edges.put(o, new ArrayList<Edge>());
    ThreadInfo info = threadInfo(t);
    abstraction.nodeCreated(info, o);
    if (graphMonitor != null) graphMonitor.addNode(o, null);
  }

  public void edgeCreated(int t, int b, int f, int o) {
    nodeCreated(t, b);
    nodeCreated(t, o);
    assert (b > 0);

    // Strong update: remove existing field pointer
    List<Edge> edges = state.o2edges.get(b);
    if (useStrongUpdates) {
      for (int i = 0; i < edges.size(); i++) {
        if (edges.get(i).f == f) {
          int old_o = edges.get(i).o;
          abstraction.edgeDeleted(b, f, old_o);
          if (graphMonitor != null) graphMonitor.deleteEdge(b, old_o);
          edges.remove(i);
          break;
        }
      }
    }

    if (o != -1) {
      edges.add(new Edge(f, o));
      abstraction.edgeCreated(b, f, o);
      if (graphMonitor != null) graphMonitor.addEdge(o, b, ""+f);
    }
    if (verbose >= 1)
      X.logs("ADDEDGE b=%s f=%s o=%s", ostr(b), fstr(f), ostr(o));
  }

  // Typically, this function is the source of queries
  public void fieldAccessed(int e, int t, int b, int f, int o) {
    nodeCreated(t, b);
    if (selectSnapshotRandom.nextDouble() < snapshotFrac)
      takeSnapshot();
  }

  public void takeSnapshot() { }

  //////////////////////////////
  // Pretty-printing

  public String fstr(int f) { // field
    if (f == ARRAY_FIELD) return "[*]";
    return f < 0 ? "-" : instrumentor.getFmap().get(f);
  }
  public String hstr(int h) { return h < 0 ? "-" : instrumentor.getHmap().get(h); } // heap allocation site
  public String estr(int e) {
    if (e < 0) return "-";
    Quad quad = (Quad)instrumentor.getDomE().get(e);
    return Program.v().toJavaPosStr(quad)+" "+Program.v().toQuadStr(quad);
  }
  public String mstr(int m) { return m < 0 ? "-" : instrumentor.getMmap().get(m); } // method
  public String wstr(int w) { return w < 0 ? "-" : instrumentor.getWmap().get(w); } // loop
  public String istr(int i) { return i < 0 ? "-" : instrumentor.getImap().get(i); } // call site
  public String ostr(int o) { return o < 0 ? "-" : (o == NULL_OBJECT ? "null" : "O"+o); } // concrete object
  public String tstr(int t) { return t < 0 ? "-" : "T"+t; } // thread
  public String pstr(int p) { return p < 0 ? "-" : instrumentor.getPmap().get(p); } // simple statement?

  ////////////////////////////////////////////////////////////
  // Handlers

  @Override public void processEnterMethod(int m, int t) {
    if (verbose >= 6) X.logs("EVENT enterMethod: m=%s, t=%s", mstr(m), tstr(t));
  }
  @Override public void processLeaveMethod(int m, int t) {
    if (verbose >= 6) X.logs("EVENT leaveMethod: m=%s, t=%s", mstr(m), tstr(t));
  }
  @Override public void processEnterLoop(int w, int t) {
    if (verbose >= 5) X.logs("EVENT enterLoop: w=%s", w);
  }
  @Override public void processLeaveLoop(int w, int t) {
    if (verbose >= 5) X.logs("EVENT leaveLoop: w=%s", w);
  }

  @Override public void processNewOrNewArray(int h, int t, int o) { // new Object
    state.o2h.put(o, h);
    nodeCreated(t, o);
    if (verbose >= 5) X.logs("EVENT new: h=%s, t=%s, o=%s", hstr(h), tstr(t), ostr(o));
  }

  @Override public void processGetstaticPrimitive(int e, int t, int b, int f) { }
  @Override public void processGetstaticReference(int e, int t, int b, int f, int o) { // ... = b.f, where b.f = o and b is static
    if (verbose >= 5) X.logs("EVENT getStaticReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
  }
  @Override public void processPutstaticPrimitive(int e, int t, int b, int f) { }
  @Override public void processPutstaticReference(int e, int t, int b, int f, int o) { // b.f = o, where b is static
    if (verbose >= 5) X.logs("EVENT putStaticReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
    edgeCreated(t, b, f, o);
  }

  @Override public void processGetfieldPrimitive(int e, int t, int b, int f) {
    if (!isExcluded(e)) fieldAccessed(e, t, b, f, -1);
  }
  @Override public void processGetfieldReference(int e, int t, int b, int f, int o) { // ... = b.f, where b.f = o
    if (verbose >= 5) X.logs("EVENT getFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
    if (!isExcluded(e)) fieldAccessed(e, t, b, f, o);
  }
  @Override public void processPutfieldPrimitive(int e, int t, int b, int f) {
    if (!isExcluded(e)) fieldAccessed(e, t, b, f, -1);
  }
  @Override public void processPutfieldReference(int e, int t, int b, int f, int o) { // b.f = o
    if (verbose >= 5) X.logs("EVENT putFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
    edgeCreated(t, b, f, o);
    if (!isExcluded(e)) fieldAccessed(e, t, b, f, o);
  }

  @Override public void processAloadPrimitive(int e, int t, int b, int i) {
    if (!isExcluded(e)) fieldAccessed(e, t, b, ARRAY_FIELD, -1);
  }
  @Override public void processAloadReference(int e, int t, int b, int i, int o) {
    if (verbose >= 5) X.logs("EVENT loadReference: e=%s, t=%s, b=%s, i=%s, o=%s", estr(e), tstr(t), ostr(b), i, ostr(o));
    if (!isExcluded(e)) fieldAccessed(e, t, b, ARRAY_FIELD, o);
  }
  @Override public void processAstorePrimitive(int e, int t, int b, int i) {
    if (!isExcluded(e)) fieldAccessed(e, t, b, ARRAY_FIELD, -1);
  }
  @Override public void processAstoreReference(int e, int t, int b, int i, int o) {
    if (verbose >= 5) X.logs("EVENT storeReference: e=%s, t=%s, b=%s, i=%s, o=%s", estr(e), tstr(t), ostr(b), i, ostr(o));
    edgeCreated(t, b, ARRAY_FIELD, o);
    if (!isExcluded(e)) fieldAccessed(e, t, b, ARRAY_FIELD, o);
  }

  @Override public void processThreadStart(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadStart: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
  }
  @Override public void processThreadJoin(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadJoin: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
  }

  @Override public void processAcquireLock(int l, int t, int o) { }
  @Override public void processReleaseLock(int r, int t, int o) { }
  @Override public void processWait(int i, int t, int o) { }
  @Override public void processNotify(int i, int t, int o) { }

  @Override public void processMethodCallBef(int i, int t, int o) {
    if (verbose >= 5) X.logs("EVENT methodCallBefore: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
    ThreadInfo info = threadInfo(t);
    info.callSites.push(i);
    info.callAllocs.push(state.o2h.get(o));
  }
  @Override public void processMethodCallAft(int i, int t, int o) {
    ThreadInfo info = threadInfo(t);
    if (verbose >= 5) X.logs("EVENT methodCallAfter: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
    if (info.callSites.size() == 0)
      X.errors("Tried to pop empty callSites stack");
    else {
      int ii = info.callSites.pop();
      if (ii != i) X.logs("pushed %s but popped %s", istr(i), istr(ii));
    }
    if (info.callAllocs.size() == 0)
      X.errors("Tried to pop empty callAllocs stack");
    else {
      int hh = info.callAllocs.pop();
      int h = state.o2h.get(o);
      if (hh != h) X.logs("pushed %s but popped %s", hstr(h), hstr(hh));
    }
  }

  @Override public void processReturnPrimitive(int p, int t) { }
  @Override public void processReturnReference(int p, int t, int o) { }
  @Override public void processExplicitThrow(int p, int t, int o) { }
  @Override public void processImplicitThrow(int p, int t, int o) { }

  @Override public void processQuad(int p, int t) {
    if (verbose >= 7) X.logs("EVENT processQuad p=%s, t=%s", pstr(p), tstr(t));
  }
  @Override public void processBasicBlock(int b, int t) { }

  // TODO
  //public void processFinalize(int b, int t) { }
  //public void processAddVariable(int b, int t) { }

  // Query for thread escape: is the object pointed to by the relvant variable thread-escaping at program point e?
  class ProgramPointQuery extends Query {
    public ProgramPointQuery(int e) { this.e = e; }
    public int e; // Program point

    @Override public boolean equals(Object _that) {
      if (_that instanceof ProgramPointQuery) {
        ProgramPointQuery that = (ProgramPointQuery)_that;
        return this.e == that.e;
      }
      return false;
    }
    @Override public int hashCode() { return e; }
    @Override public String toString() { return estr(e); }
  }
}

////////////////////////////////////////////////////////////

// Pointer via field f to object o
class Edge {
  public Edge(int f, int o) {
    this.f = f;
    this.o = o;
  }
  public int f;
  public int o;
}

abstract class Query {
}

class QueryResult {
  public boolean selected; // Whether we are trying to answer this query or not
  public int numTrue = 0;
  public int numFalse = 0;

  public boolean isTrue() { return numTrue > 0; } // Existential property
  public void add(boolean b) {
    if (b) numTrue++;
    else numFalse++;
  }

  @Override public String toString() { return numTrue+"|"+numFalse; }
}

class ThreadInfo {
  public Stack<Integer> callStack = new Stack(); // Elements are methods m (for visualization)
  public Stack<Integer> callSites = new Stack(); // Elements are call sites i (for kCFA)
  public Stack<Integer> callAllocs = new Stack(); // Elements are object allocation sites h (for kOS)
}

class State {
  TIntIntHashMap v2o = new TIntIntHashMap(); // variable v -> object o
  TIntIntHashMap o2h = new TIntIntHashMap(); // object o -> heap allocation site h
  TIntObjectHashMap<List<Edge>> o2edges = new TIntObjectHashMap<List<Edge>>(); // object o -> list of outgoing edges from o
}
