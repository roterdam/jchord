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
 * Does not reclaim global variables.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
@Chord(name="ss-thread-escape2")
public class ThreadEscapeAnalysis2 extends SnapshotAnalysis {
  public String propertyName() { return "thread-escape2"; }

  class Event {
    public Event(int e, int b) {
      this.e = e;
      this.b = b;
    }
    int e;
    int b;
  }

  TIntHashSet escapedNodes = new TIntHashSet(); // Set of objects reachable from some global at any point in time
  List<Event> events = new ArrayList<Event>(); // For batching

  @Override
	protected InstrScheme getBaselineScheme() {
	  InstrScheme instrScheme = new InstrScheme();
	  instrScheme.setThreadStartEvent(false, true, true);
	  return instrScheme;
	}
  
  // These methods add nodes to the graph
  @Override public void processPutstaticReference(int e, int t, int b, int f, int o) { // b.f = o, where b is static
    super.processPutstaticReference(e, t, b, f, o);
    setEscape(o);
  }
  @Override public void processThreadStart(int i, int t, int o) {
    super.processThreadStart(i, t, o);
    setEscape(o);
  }

  @Override public void edgeCreated(int t, int b, int f, int o) {
    super.edgeCreated(t, b, f, o);
    // NOTE: the addition of this edge changes the abstraction and could in principle
    // cause more nodes to escape.  Ignore this effect for now.
    if (escapedNodes.contains(b))
      setEscape(o);  
  }

  @Override public void abstractionChanged(int o, Object a) {
    // When the abstraction changes, more things might get smashed together, so
    // we need to propagate more escaping.
    List<Integer> os = abstraction.a2os.get(a);
    // Does anything escape?
    boolean escaped = false;
    for (int oo : os) {
      if (escapedNodes.contains(oo)) {
        escaped = true;
        break;
      }
    }
    // If escapes, then everything escapes.
    if (escaped) {
      for (int oo : os)
        setEscapeRecurse(oo);
    }
  }

  public void setEscape(int o) {
    if (o <= 0) return;
    abstraction.ensureComputed();
    setEscapeRecurse(o);
  }
  private void setEscapeRecurse(int o) {
    if (escapedNodes.contains(o)) return;
    escapedNodes.add(o);
    // Follow edges
    for (Edge e : state.o2edges.get(o))
      setEscapeRecurse(e.o);
    // Follow abstractions
    Object a = abstraction.getValue(o);
    List<Integer> os = abstraction.a2os.get(a);
    if (os != null) {
      for (int oo : os) setEscapeRecurse(oo);
    }
  }

  @Override public void fieldAccessed(int e, int t, int b, int f, int o) {
    super.fieldAccessed(e, t, b, f, o);
    if (queryOnlyAtSnapshot)
      events.add(new Event(e, b));
    else {
      assert (b > 0);
      Query query = new ProgramPointQuery(e);
      if (shouldAnswerQueryHit(query))
        answerQuery(query, escapedNodes.contains(b)); // Query is already computed for us
    }
  }

  @Override public SnapshotResult takeSnapshot() {
    if (queryOnlyAtSnapshot) {
      for (Event event : events) { 
        Query query = new ProgramPointQuery(event.e);
        answerQuery(query, escapedNodes.contains(event.b));
      }
      events.clear();
    }
    return null;
  }
}
