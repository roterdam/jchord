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
import gnu.trove.TIntIntProcedure;

/**
 * Thread-escape in the snapshot framework.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(name="ss-thread-escape")
public class ThreadEscapeAnalysis extends SnapshotAnalysis {
  public String propertyName() { return "thread-escape"; }

  static final int THREAD_FIELD_START = 99000;
  static final int THREAD_GLOBAL_OBJECT = 100000;

  TIntHashSet staticNodes = new TIntHashSet(); // set of global objects
  TIntIntHashMap e2o = new TIntIntHashMap(); // For batching queries

  @Override public String fstr(int f) { return f >= THREAD_FIELD_START ? "[T"+(f-THREAD_FIELD_START)+"]" : super.fstr(f); }
  @Override public String ostr(int o) { return o == THREAD_GLOBAL_OBJECT ? "(T)" : super.ostr(o); }

  @Override public void initAllPasses() {
    super.initAllPasses();
    abstraction.separateNodes = staticNodes;
  }

  int currThreadField = THREAD_FIELD_START;
  int newThreadField() { currThreadField++; return currThreadField-1; }
  //int newThreadField() { return currThreadField; } // TMP: PartitionAnalysis does this by accident

  // These methods add nodes to the graph
  @Override public void processPutstaticReference(int e, int t, int b, int f, int o) { // b.f = o, where b is static
    super.processPutstaticReference(e, t, b, f, o);
    setGlobal(b);
  }
  @Override public void processThreadStart(int i, int t, int o) {
    super.processThreadStart(i, t, o);
    int b = THREAD_GLOBAL_OBJECT;
    nodeCreated(t, b);
    setGlobal(b);
    edgeCreated(t, b, newThreadField(), o);
  }

  public void setGlobal(int o) {
    //if (verbose >= 1) X.logs("SETESCAPE a=%s", astr(a));
    staticNodes.add(o);
  }

  @Override public void fieldAccessed(int e, int t, int b, int f, int o) {
    super.fieldAccessed(e, t, b, f, o);
    if (queryOnlyAtSnapshot) {
      // Keep track of the queries, so we can answer them at the end
      e2o.put(e, b);
    }
    else {
      if (b > 0) {
        Query query = new ProgramPointQuery(e);
        if (shouldAnswerQueryHit(query)) {
          abstraction.ensureComputed();
          answerQuery(query, escapes(b));
        }
      }
      else
        X.errors("fieldAccessed at e=%s, t=%s, b=%s, f=%s, o=%s", estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
      assert (b > 0);
    }
  }

  @Override public SnapshotResult takeSnapshot() {
    final NodeBasedSnapshotResult result = new NodeBasedSnapshotResult();
    for (int start : staticNodes.toArray()) // Do flood-fill without abstraction
      reachable(start, -1, result.actualTrueNodes, false);
    for (int start : staticNodes.toArray()) // Do flood-fill with abstraction
      reachable(start, -1, result.proposedTrueNodes, true);

    if (queryOnlyAtSnapshot) {
      e2o.forEachEntry(new TIntIntProcedure() { public boolean execute(int e, int o) {
        Query query = new ProgramPointQuery(e);
        answerQuery(query, result.proposedTrueNodes.contains(o));
        return true;
      } });
    }

    return result;
  }

  // Does node a escape under the given global abstraction?
  private boolean escapes(int o) {
    for (int start : staticNodes.toArray())
      if (reachable(start, o, new TIntHashSet(), true)) return true;
    return false;
  }

  private boolean reachable(int o, int dest, TIntHashSet visitedNodes, boolean useAbstraction) {
    if (o == dest) return true;
    if (visitedNodes.contains(o)) return false; // Got into a cycle
    visitedNodes.add(o);

    // Try following edges in the graph
    for (Edge e : state.o2edges.get(o))
      if (reachable(e.o, dest, visitedNodes, useAbstraction)) return true;

    // Try jumping to nodes with the same abstract value
    if (useAbstraction) {
      Object a = abstraction.getValue(o);
      List<Integer> os = abstraction.a2os.get(a);
      if (os != null) {
        for (int oo : os)
          if (reachable(oo, dest, visitedNodes, useAbstraction)) return true;
      }
    }

    return false;
  }
}
