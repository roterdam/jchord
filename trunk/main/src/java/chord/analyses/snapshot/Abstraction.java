package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * An abstraction is a function from a node (object) to an abstract value,
 * which depends on the graph.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public abstract class Abstraction {
  public Execution X;
  public State state;
  public TIntHashSet separateNodes = new TIntHashSet(); // (Optional): let these values have a distinct value

  // Build these as intermediate data structures
  protected TIntObjectHashMap<Object> o2a = new TIntObjectHashMap<Object>(); // object o -> abstract value a
  protected HashMap<Object,List<Integer>> a2os = new HashMap<Object,List<Integer>>(); // abstraction value a -> nodes o with that abstraction value

  // For incrementally creating the abstraction (if necessary).
  public abstract void nodeCreated(ThreadInfo info, int o);
  public abstract void nodeDeleted(int o);
  public void edgeCreated(int b, int f, int o) { }
  public void edgeDeleted(int b, int f, int o) { }

  // Called before we start using this abstraction in arbitrary ways, so do whatever is necessary.
  public abstract void ensureComputed();

  // Return the value of the abstraction (called after ensureComputed)
  public Object getValue(int o) { return o2a.get(o); }

  // Helpers
  protected void setValue(int o, Object a) {
    if (separateNodes.contains(o)) a = "-";
    Object old_a = o2a.get(o);
    if (old_a != null) { // There was an old abstraction there already
      if (old_a == a) return;
      a2os.get(old_a).remove((Integer)o);
    }
    o2a.put(o, a);
    Utils.add(a2os, a, o);
  }
  protected void removeValue(int o) {
    Object a = o2a.remove(o);
    a2os.get(a).remove((Integer)o);
  }
}

class NoneAbstraction extends Abstraction {
  @Override public String toString() { return "none"; }
  @Override public void nodeCreated(ThreadInfo info, int o) { }
  @Override public void nodeDeleted(int o) { }
  @Override public void ensureComputed() { }
  @Override public Object getValue(int o) { return o; }
}

class AllocAbstraction extends Abstraction {
  int kCFA, kOS;

  public AllocAbstraction(int kCFA, int kOS) {
    this.kCFA = kCFA;
    this.kOS = kOS;
  }

  @Override public String toString() {
    if (kCFA == 0 && kOS == 0) return "alloc";
    return String.format("alloc(kCFA=%d,kOS=%d)", kCFA, kOS);
  }

  @Override public void nodeCreated(ThreadInfo info, int o) {
    setValue(o, computeValue(info, o));
  }
  @Override public void nodeDeleted(int o) {
    removeValue(o);
  }

  @Override public void ensureComputed() { }

  private Object computeValue(ThreadInfo info, int o) {
    if (kCFA == 0 && kOS == 0) return state.o2h.get(o); // No context

    StringBuilder buf = new StringBuilder();
    buf.append(state.o2h.get(o));

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

// SLOW
class RecencyAbstraction extends Abstraction {
  TIntIntHashMap h2count = new TIntIntHashMap(); // heap allocation site h -> number of objects that have been allocated at h
  TIntIntHashMap o2count = new TIntIntHashMap(); // object -> count

  public String toString() { return "recency"; }

  @Override public void nodeCreated(ThreadInfo info, int o) {
    int h = state.o2h.get(o);
    h2count.adjustOrPutValue(h, 1, 1);
    o2count.put(o, h2count.get(h));
    //X.logs("nodeCreated: o=%s, h=%s; count = %s", o, h, o2count.get(o));
  }
  @Override public void nodeDeleted(int o) {
    o2count.remove(o);
  }

  @Override public void ensureComputed() {
    //X.logs("RecencyAbstraction.ensureComputed: %s %s", state.o2h.size(), o2count.size());

    state.o2h.forEachKey(new TIntProcedure() { public boolean execute(int o) { 
      setValue(o, computeValue(o));
      return true;
    } });
  }

  private Object computeValue(int o) {
    int h = state.o2h.get(o);
    boolean mostRecent = o2count.get(o) == h2count.get(h);
    return mostRecent ? h+"R" : h;
  }
}

// SLOW
class ReachabilityAbstraction extends Abstraction {
  static class Spec {
    public boolean pointedTo, matchRepeatedFields, matchFirstField, matchLastField;
  }
  Spec spec;
  TIntObjectHashMap<List<String>> o2pats = new TIntObjectHashMap(); // o -> list of path patterns that describe o

  public ReachabilityAbstraction(Spec spec) {
    this.spec = spec;
  }

  @Override public String toString() {
    if (spec.pointedTo) return "reach(point)";
    if (spec.matchRepeatedFields) return "reach(f*)";
    if (spec.matchFirstField) return "reach(first_f)";
    if (spec.matchLastField) return "reach(last_f)";
    return "reach";
  }

  @Override public void nodeCreated(ThreadInfo info, int o) { }
  @Override public void nodeDeleted(int o) { }

  @Override public void ensureComputed() {
    o2pats.clear();
    state.o2h.forEachKey(new TIntProcedure() { public boolean execute(int o) {
      o2pats.put(o, new ArrayList<String>());
      return true;
    } });

    // For each node, compute reachability from sources
    state.o2h.forEachKey(new TIntProcedure() { public boolean execute(int o) {
      String source = "H"+state.o2h.get(o);
      //X.logs("--- source=%s, a=%s", source, astr(a));
      search(source, -1, -1, 0, o);
      return true;
    } });

    // Compute the values
    state.o2h.forEachKey(new TIntProcedure() { public boolean execute(int o) { 
      setValue(o, computeValue(o));
      return true;
    } });
  }

  // Source: variable or heap-allocation site.
  // General recipe for predicates is some function of the list of fields from source to the node.
  // Examples:
  //   Reachable-from: path must exist.
  //   Pointed-to-by: length must be one.
  //   Reachable-following-a-single field
  //   Reachable-from-with-first-field
  //   Reachable-from-with-last-field

  private void search(String source, int first_f, int last_f, int len, int o) {
    String pat = null;
    if (len > 0) { // Avoid trivial predicates
      if (spec.pointedTo) {
        if (len == 1) pat = source;
      }
      else if (spec.matchRepeatedFields) {
        assert (first_f == last_f);
        pat = source+"."+first_f+"*";
      } 
      else if (spec.matchFirstField)
        pat = source+"."+first_f+".*";
      else if (spec.matchLastField)
        pat = source+".*."+last_f;
      else // Plain reachability
        pat = source;
    }

    List<String> pats = o2pats.get(o);

    if (pat != null && pats.indexOf(pat) != -1) return; // Already have it

    if (pat != null) pats.add(pat);
    //X.logs("source=%s first_f=%s last_f=%s len=%s a=%s: v=%s", source, fstr(first_f), fstr(last_f), len, astr(a), a2ws[a]);

    if (spec.pointedTo && len >= 1) return;

    // Recurse
    List<Edge> edges = state.o2edges.get(o);
    if (edges == null) {
      X.errors("o=%s returned no edges", o);
    }
    else {
      for (Edge e : edges) {
        if (spec.matchRepeatedFields && first_f != -1 && first_f != e.f) continue; // Must have same field
        search(source, len == 0 ? e.f : first_f, e.f, len+1, e.o);
      }
    }
  }

  private Object computeValue(int o) {
    List<String> pats = o2pats.get(o);
    Collections.sort(pats); // Canonicalize
    return pats.toString();
  }
}
