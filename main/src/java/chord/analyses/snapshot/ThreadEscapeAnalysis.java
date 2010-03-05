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
import chord.doms.DomT;

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
 * Thread-escape in the snapshot framework.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(name="ss-thread-escape")
public class ThreadEscapeAnalysis extends SnapshotAnalysis {
  public String propertyName() { return "thread-escape"; }

  static final int THREAD_FIELD = 99999;
  static final int THREAD_GLOBAL_OBJECT = 100000;

  TIntHashSet staticNodes = new TIntHashSet(); // set of global objects
  TIntHashSet escapedNodes = new TIntHashSet(); // set of an object [for weak updates]

  @Override public String fstr(int f) { return f == THREAD_FIELD ? "[T]" : super.fstr(f); }
  @Override public String ostr(int o) { return o == THREAD_GLOBAL_OBJECT ? "(T)" : super.ostr(o); }

  // These methods add nodes to the graph
  @Override public void processPutstaticReference(int e, int t, int b, int f, int o) { // b.f = o, where b is static
    super.processPutstaticReference(e, t, b, f, o);
    setGlobal(b);
  }
  @Override public void processThreadStart(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadStart: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
    int b = THREAD_GLOBAL_OBJECT;
    nodeCreated(t, b);
    setGlobal(b);
    edgeCreated(t, b, THREAD_FIELD, o);
  }
  @Override public void processThreadJoin(int i, int t, int o) {
    if (verbose >= 4) X.logs("EVENT threadJoin: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
  }

  public void setGlobal(int a) {
    //if (verbose >= 1) X.logs("SETESCAPE a=%s", astr(a));
    staticNodes.add(a);
    if (!useStrongUpdates) escapedNodes.add(a);
  }

  @Override public void fieldAccessed(int e, int t, int b, int f, int o) {
    super.fieldAccessed(e, t, b, f, o);
    Query query = new ProgramPointQuery(e);
    if (shouldAnswerQueryHit(query)) {
      abstraction.ensureComputed();
      answerQuery(query, escapes(o));
    }
  }

  // Does node a escape under the given global abstraction?
  private boolean escapes(int o) {
    for (int start : staticNodes.toArray())
      if (reachable(start, o, new TIntHashSet())) return true;
    return false;
  }

  private boolean reachable(int o, int dest, TIntHashSet visitedNodes) {
    if (o == dest) return true;
    if (visitedNodes.contains(o)) return false; // Got into a cycle
    visitedNodes.add(o);

    // Try following edges in the graph
    for (Edge e : state.o2edges.get(o))
      if (reachable(e.o, dest, visitedNodes)) return true;

    // Try jumping to nodes with the same abstract value
    Object a = abstraction.getValue(o);
    List<Integer> os = abstraction.a2os.get(a);
    if (os != null) {
      for (int oo : os)
        if (reachable(oo, dest, visitedNodes)) return true;
    }

    return false;
  }
}
