package chord.analyses.snapshot;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import chord.util.ArraySet;

interface AbstractionListener {
	// Called when the abstraction is changed
	void abstractionChanged(int o, Object a);
}

interface AbstractionInitializer {
	// Called when the abstraction is changed
	void initAbstraction(Abstraction abstraction);
}

/**
 * An abstraction is a function from a node (object) to an abstract value, which
 * depends on the graph.
 * 
 * @author Percy Liang (pliang@cs.berkeley.edu)
 * @author omert (omertrip@post.tau.ac.il)
 */
public abstract class Abstraction {
	public Execution X;
	public State state;
	public AbstractionListener listener;
  public boolean require_a2o; // Whether we need to maintain the a2o map (e.g., for thread-escape)
	public TIntHashSet separateNodes = new TIntHashSet(); // (Optional): let these objects be distinct value (not used right now)
	// Build these as intermediate data structures
	protected TIntObjectHashMap<Object> o2a = new TIntObjectHashMap<Object>(); // object o -> abstract value a
	protected HashMap<Object, List<Integer>> a2os = null; // abstraction value a -> nodes o with that abstraction value
	// For incrementally creating the abstraction (if necessary).
	public abstract void nodeCreated(ThreadInfo info, int o);
	public abstract void nodeDeleted(int o);
	public void edgeCreated(int b, int f, int o) { }
  public void edgeDeleted(int b, int f, int o) { }

  public boolean requireImmutableAbstractValues() { return require_a2o; }

	public void init(AbstractionInitializer initializer) {
		initializer.initAbstraction(this);
    if (require_a2o) a2os = new HashMap<Object, List<Integer>>(); 
	}

	// Called before we start using this abstraction in arbitrary ways, so do
	// whatever is necessary.
	// Try to keep this function empty and incrementally update the abstraction.
	public abstract void ensureComputed();

	// Return the value of the abstraction (called after ensureComputed)
	public Object getValue(int o) {
		return o2a.get(o);
	}

	// Helpers
	protected void setValue(int o, Object a) {
		if (!state.o2edges.containsKey(o))
			throw new RuntimeException("" + o);
		if (separateNodes.contains(o)) a = "-";
    if (require_a2o) {
      Object old_a = o2a.get(o);
      if (old_a != null) { // There was an old abstraction there already
        if (old_a.equals(a))
          return; // Haven't changed the abstraction
        List<Integer> os = a2os.get(old_a);
        if (os != null)
          os.remove((Integer) o);
      }
      Utils.add(a2os, a, o);
    }
		o2a.put(o, a);
		listener.abstractionChanged(o, a);
	}

	protected void removeValue(int o) {
		Object a = o2a.remove(o);
		a2os.get(a).remove((Integer) o);
	}
}

abstract class LocalAbstraction extends Abstraction {
	public abstract Object computeValue(ThreadInfo info, int o);
}

////////////////////////////////////////////////////////////

class NoneAbstraction extends Abstraction {
	@Override
	public String toString() {
		return "none";
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
	}

	@Override
	public void nodeDeleted(int o) {
	}

	@Override
	public void ensureComputed() {
	}

	@Override
	public Object getValue(int o) {
		return o;
	}
}

class RandomAbstraction extends Abstraction {
  int size;
  Random random;
  public RandomAbstraction(int size) {
    this.size = size;
    this.random = new Random(1);
  }

	@Override public String toString() { return "random("+size+")"; }

	@Override public void nodeCreated(ThreadInfo info, int o) {
    setValue(o, random.nextInt(size));
	}
	@Override public void nodeDeleted(int o) { }

	@Override public void ensureComputed() { }
}

class AllocAbstraction extends LocalAbstraction {
	int kCFA, kOS;

	public AllocAbstraction(int kCFA, int kOS) {
		this.kCFA = kCFA;
		this.kOS = kOS;
	}

	@Override
	public String toString() {
		if (kCFA == 0 && kOS == 0) return "alloc";
		//return String.format("alloc(kCFA=%d,kOS=%d)", kCFA, kOS);
		return String.format("alloc(k=%d)", kCFA);
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		setValue(o, computeValue(info, o));
	}

	@Override
	public void nodeDeleted(int o) {
		removeValue(o);
	}

	@Override
	public void ensureComputed() {
	}

	public Object computeValue(ThreadInfo info, int o) {
		if (kCFA == 0 && kOS == 0)
			return state.o2h.get(o); // No context

		StringBuilder buf = new StringBuilder();
		buf.append(state.o2h.get(o));

		if (kCFA > 0) {
			for (int i = 0; i < kCFA; i++) {
				int j = info.callSites.size() - i - 1;
				if (j < 0)
					break;
				buf.append('_');
				buf.append(info.callSites.get(j));
			}
		}

		return buf.toString();
	}
}



class ReachableFromAllocPlusFieldsAbstraction extends LabelBasedAbstraction {

	private static class AllocPlusFieldLabel implements Label {
		private static final int SELF = Integer.MIN_VALUE;
		public final int h;
		public final int f;

		public AllocPlusFieldLabel(int h, int f) {
			this.h = h;
			this.f = f;
		}

		public AllocPlusFieldLabel(int h) {
			this(h, SELF);
		}

		@Override
		public String toString() {
			return "<h,f>: " + h + (f == SELF ? "" : "." + f);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + f;
			result = prime * result + h;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AllocPlusFieldLabel other = (AllocPlusFieldLabel) obj;
			if (f != other.f)
				return false;
			if (h != other.h)
				return false;
			return true;
		}
	}

	private final static TIntHashSet EMPTY = new TIntHashSet();
	
	private final TIntObjectHashMap<TIntHashSet> alloc2objects = new TIntObjectHashMap<TIntHashSet>();

	@Override
	public String toString() {
		return "alloc-x-field-reachability";
	}

	@Override
	protected TIntHashSet getRootsImpl(Label l) {
		assert (l instanceof AllocPlusFieldLabel);
		AllocPlusFieldLabel apfl = (AllocPlusFieldLabel) l;
		if (apfl.f == AllocPlusFieldLabel.SELF) {
			return alloc2objects.get(apfl.h);
		} else {
			TIntHashSet alloced = alloc2objects.get(apfl.h);
			if (alloced == null) {
				assert (apfl.h < 0);
				return EMPTY;
			} else {
				TIntHashSet result = new TIntHashSet();
				for (TIntIterator it = alloced.iterator(); it.hasNext(); ) {
					int next = it.next();
					TIntIntHashMap M = heapGraph.get(next);
					if (M != null && M.containsKey(apfl.f)) {
						int o = M.get(apfl.f);
						if (o != 0) {
							result.add(o);
						}
					}
				}
				return result;
			}
		}
	}

	@Override
	protected Collection<Label> freshLabels(int b, int f, int o) {
		int h = state.o2h.get(b);
		return Collections.<Label> singleton(new AllocPlusFieldLabel(h, f));
	}

	@Override
	public void ensureComputed() {
		// This is a no-op.
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		int h = state.o2h.get(o);
		if (o != 0 && h >= 0) {
			Set<Label> S = new HashSet<Label>(1);
			S.add(new AllocPlusFieldLabel(h));
			object2labels.put(o, S);
			mySetValue(o, S);
			TIntHashSet T = alloc2objects.get(h);
			if (T == null) {
				T = new TIntHashSet();
				alloc2objects.put(h, T);
			}
			T.add(o);
		}
	}

	@Override
	public void nodeDeleted(int o) {
		throw new RuntimeException(
				"Operation 'nodeDeleted' not currently supported.");
	}

	@Override
	protected Set<Label> getLabelsRootedAt(final int o) {
		assert (o != 0);
		final Set<Label> result = new HashSet<Label>(4);
		int h = state.o2h.get(o);
		if (h >= 0) {
			result.add(new AllocPlusFieldLabel(h));
		}
		TIntArrayList preds = object2predecessors.get(o);
		if (preds != null) {
			preds.forEach(new TIntProcedure() {
				@Override
				public boolean execute(final int pred) {
					if (state.o2h.get(pred) >= 0) {
						TIntIntHashMap M = heapGraph.get(pred);
						assert (M != null);
						M.forEachEntry(new TIntIntProcedure() {
							@Override
							public boolean execute(int f, int val) {
								if (val == o) {
									result.add(new AllocPlusFieldLabel(state.o2h.get(pred), f));
								}
								return true;
							}
						});
					}
					return true;
				}
			});
		}
		return result;
	}
}

class ReachableFromAllocAbstraction extends LabelBasedAbstraction {

	private static class AllocationSiteLabel implements Label {
		public final int h;

		public AllocationSiteLabel(int h) {
			this.h = h;
		}

		@Override
		public String toString() {
			return "<h>: " + h;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + h;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AllocationSiteLabel other = (AllocationSiteLabel) obj;
			if (h != other.h)
				return false;
			return true;
		}
	}

	private final TIntObjectHashMap<TIntHashSet> alloc2objects = new TIntObjectHashMap<TIntHashSet>();

	@Override
	public String toString() {
		return "alloc-reachability";
	}

	@Override
	protected TIntHashSet getRootsImpl(Label l) {
		assert (l instanceof AllocationSiteLabel);
		AllocationSiteLabel allocLabel = (AllocationSiteLabel) l;
		return alloc2objects.get(allocLabel.h);
	}

	@Override
	public void ensureComputed() {
		// This is a no-op.
	}

	@Override
	public void nodeDeleted(int o) {
		throw new RuntimeException(
				"Operation 'nodeDeleted' not currently supported.");
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		int h = state.o2h.get(o);
		if (o != 0 && h >= 0) {
			Set<Label> S = new HashSet<Label>(1);
			S.add(new AllocationSiteLabel(h));
			object2labels.put(o, S);
			mySetValue(o, S);
			TIntHashSet T = alloc2objects.get(h);
			if (T == null) {
				T = new TIntHashSet();
				alloc2objects.put(h, T);
			}
			T.add(o);
		}
	}

	@Override
	protected Set<Label> getLabelsRootedAt(int o) {
		int h = state.o2h.get(o);
		if (h >= 0) {
			return Collections.<Label> singleton(new AllocationSiteLabel(h));
		} else {
			return Collections.emptySet();
		}
	}
}

class PointedToByAbstraction extends Abstraction {
	
	private static class Value {
		
		protected final static Value EMPTY_VALUE = new Value(new int[0]);
		
		private final int[] values;
		
		public Value(int[] values) {
			this.values = values;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(values);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Value other = (Value) obj;
			if (!Arrays.equals(values, other.values))
				return false;
			return true;
		}
	}

	private final TIntObjectHashMap<TIntIntHashMap> object2pointers = new TIntObjectHashMap<TIntIntHashMap>();

	@Override
	public String toString() {
		return "pointed-to";
	}

	@Override
	public void ensureComputed() {
		// This is a no-op.
	}

	@Override
	public void edgeCreated(int b, int f, int o) {
		super.edgeCreated(b, f, o);
		if (b != 0 && o != 0 && f != 0) {
			int h = state.o2h.get(b);
			if (h >= 0) {
				TIntIntHashMap M = object2pointers.get(o);
				if (M == null) {
					M = new TIntIntHashMap();
					object2pointers.put(o, M);
				}
				boolean hasChanged = !M.containsValue(h);
				M.put(f, h);
				if (hasChanged) {
					Value v = new Value(M.getValues()); // XXX: these should be sorted?
					setValue(o, v);
				}
			}
		}
	}

	@Override
	public void edgeDeleted(int b, int f, int o) {
		super.edgeDeleted(b, f, o);
		if (b != 0 && o != 0 && f != 0) {
			int h = state.o2h.get(b);
			if (h >= 0) {
				TIntIntHashMap M = object2pointers.get(o);
				assert (M != null);
				M.remove(f);
				boolean hasChanged = !M.containsValue(h);
				if (hasChanged) {
					Value v = new Value(M.getValues()); // XXX: these should be sorted?
					setValue(o, v);
				}
			}
		}
	}

	@Override
	public void nodeDeleted(int o) {
		throw new RuntimeException(
				"Operation 'nodeDeleted' not currently supported.");
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		// We assign a fresh object the default abstraction.
		object2pointers.put(o, new TIntIntHashMap());
		setValue(o, Value.EMPTY_VALUE);
	}
}

/*
 * class RecencyAbstraction extends Abstraction { TIntIntHashMap h2count = new
 * TIntIntHashMap(); // heap allocation site h -> number of objects that have
 * been allocated at h TIntIntHashMap o2count = new TIntIntHashMap(); // object
 * -> count
 * 
 * public String toString() { return "recency"; }
 * 
 * @Override public void nodeCreated(ThreadInfo info, int o) { int h =
 * state.o2h.get(o); h2count.adjustOrPutValue(h, 1, 1); o2count.put(o,
 * h2count.get(h)); //X.logs("nodeCreated: o=%s, h=%s; count = %s", o, h,
 * o2count.get(o)); }
 * 
 * @Override public void nodeDeleted(int o) { o2count.remove(o); }
 * 
 * @Override public void ensureComputed() {
 * //X.logs("RecencyAbstraction.ensureComputed: %s %s", state.o2h.size(),
 * o2count.size());
 * 
 * state.o2h.forEachKey(new TIntProcedure() { public boolean execute(int o) {
 * setValue(o, computeValue(o)); return true; } }); }
 * 
 * private Object computeValue(int o) { int h = state.o2h.get(o); boolean
 * mostRecent = o2count.get(o) == h2count.get(h); return mostRecent ? h+"R" : h;
 * } }
 */

class RecencyAbstraction extends Abstraction {
	HashMap<Object,TIntArrayList> val2lastObjects = new HashMap<Object,TIntArrayList>(); // preliminary value -> latest objects
	LocalAbstraction abstraction;
  int order; // Number of old objects to keep around

	public RecencyAbstraction(LocalAbstraction abstraction, int order) {
		this.abstraction = abstraction;
    this.order = order;
	}

	@Override
	public void init(AbstractionInitializer initializer) {
		super.init(initializer);
		abstraction.init(initializer);
	}

	@Override
	public String toString() {
		return "recency" + order + "(" + abstraction + ")";
	}

	@Override
	public void ensureComputed() {
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		Object val = abstraction.computeValue(info, o);
    TIntArrayList objects = val2lastObjects.get(val);
    if (objects == null) 
      val2lastObjects.put(val, objects = new TIntArrayList());
    // i = 0: most recent
    // ...
    // i = order-1
    // i >= order: oldest
    int n = objects.size();
    if (n == 0)
      objects.add(o);
    else {
      // Shift everyone to the right
      for (int i = n-1; i >= 0; i--) {
        int old_o = objects.get(i);
        if (i == n-1) { // The last one
          if (n <= order) { // Expand
            objects.add(old_o);
            setValue(old_o, val+"~"+(i+1));
          }
        }
        else {
          objects.set(i+1, old_o);
          setValue(old_o, val+"~"+(i+1));
        }
      }
      objects.set(0, o);
    }
    setValue(o, val);
	}

	@Override
	public void nodeDeleted(int o) {
	}
}

// SLOW
class ReachabilityAbstraction extends Abstraction {
	static class Spec {
		public boolean pointedTo, matchRepeatedFields, matchFirstField,
				matchLastField;
	}

	Spec spec;
	TIntObjectHashMap<List<String>> o2pats = new TIntObjectHashMap<List<String>>(); // o

	// ->
	// list
	// of
	// path
	// patterns
	// that
	// describe
	// o

	public ReachabilityAbstraction(Spec spec) {
		this.spec = spec;
	}

	@Override
	public String toString() {
		if (spec.pointedTo)
			return "reach(point)";
		if (spec.matchRepeatedFields)
			return "reach(f*)";
		if (spec.matchFirstField)
			return "reach(first_f)";
		if (spec.matchLastField)
			return "reach(last_f)";
		return "reach";
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
	}

	@Override
	public void nodeDeleted(int o) {
	}

	@Override
	public void ensureComputed() {
		o2pats.clear();
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				o2pats.put(o, new ArrayList<String>());
				return true;
			}
		});

		// For each node, compute reachability from sources
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				String source = "H" + state.o2h.get(o);
				// X.logs("--- source=%s, a=%s", source, astr(a));
				search(source, -1, -1, 0, o);
				return true;
			}
		});

		// Compute the values
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				setValue(o, computeValue(o));
				return true;
			}
		});
	}

	// Source: variable or heap-allocation site.
	// General recipe for predicates is some function of the list of fields from
	// source to the node.
	// Examples:
	// Reachable-from: path must exist.
	// Pointed-to-by: length must be one.
	// Reachable-following-a-single field
	// Reachable-from-with-first-field
	// Reachable-from-with-last-field

	private void search(String source, int first_f, int last_f, int len, int o) {
		String pat = null;
		/* if (len > 0) */{ // Avoid trivial predicates
			if (spec.pointedTo) {
				if (len == 1)
					pat = source;
			} else if (spec.matchRepeatedFields) {
				assert (first_f == last_f);
				pat = source + "." + first_f + "*";
			} else if (spec.matchFirstField)
				pat = source + "." + first_f + ".*";
			else if (spec.matchLastField)
				pat = source + ".*." + last_f;
			else
				// Plain reachability
				pat = source;
		}

		List<String> pats = o2pats.get(o);

		if (pat != null && pats.indexOf(pat) != -1)
			return; // Already have it

		if (pat != null)
			pats.add(pat);
		// X.logs("source=%s first_f=%s last_f=%s len=%s a=%s: v=%s", source,
		// fstr(first_f), fstr(last_f), len, astr(a), a2ws[a]);

		if (spec.pointedTo && len >= 1)
			return;

		// Recurse
		List<Edge> edges = state.o2edges.get(o);
		if (edges == null) {
			X.errors("o=%s returned no edges (shouldn't happen!)", o);
		} else {
			for (Edge e : edges) {
				if (spec.matchRepeatedFields && first_f != -1 && first_f != e.f)
					continue; // Must have same field
				search(source, len == 0 ? e.f : first_f, e.f, len + 1, e.o);
			}
		}
	}

	private Object computeValue(int o) {
		List<String> pats = o2pats.get(o);
		Collections.sort(pats); // Canonicalize
		return pats.toString();
	}
}
