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
	public abstract void edgeCreated(int b, int f, int o);
	public abstract void edgeDeleted(int b, int f, int o);

	public void init(AbstractionInitializer initializer) {
		initializer.initAbstraction(this);
    if (require_a2o) a2os = new HashMap<Object, List<Integer>>(); 
	}

	// Return the value of the abstraction
	public Object getValue(int o) { return o2a.get(o); }
	
	public Set<Object> getAbstractValues() {
		if (require_a2o)
			return a2os.keySet();
		else {
			Set<Object> values = new HashSet<Object>();
			for (Object a : o2a.getValues())
				values.add(a);
			return values;
		}
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

interface AbstractionListener {
	// Called when the abstraction is changed
	void abstractionChanged(int o, Object a);
}

interface AbstractionInitializer {
	// Called when the abstraction is changed
	void initAbstraction(Abstraction abstraction);
}
