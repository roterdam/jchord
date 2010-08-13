/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
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
 * @author omertripp (omertrip@post.tau.ac.il)
 */
public abstract class Abstraction {
	public Execution X;
	public State state;
	public AbstractionListener listener;
  public boolean require_a2o; // Whether we need to maintain the a2o map (e.g., for thread-escape)
	// Build these as intermediate data structures

	private TIntObjectHashMap<Object> o2a = new TIntObjectHashMap<Object>(); // object o -> abstract value a
	private HashMap<Object, TIntArrayList> a2os = null; // abstraction value a -> nodes o with that abstraction value

	// For incrementally creating the abstraction (if necessary).  Override.
	public abstract void nodeCreated(ThreadInfo info, int o);
	public abstract void edgeCreated(int b, int f, int o);
	public abstract void edgeDeleted(int b, int f, int o);
  public abstract boolean requireGraph(); // Whether we're going to get edgeCreated/edgeDeleted calls.

	public void init(AbstractionInitializer initializer) {
		initializer.initAbstraction(this);
    if (require_a2o) a2os = new HashMap<Object, TIntArrayList>(); 
	}

	// Return the value of the abstraction
	public Object getValue(int o) { return o2a.get(o); }
  // Return all objects with that abstraction.
  public TIntArrayList getObjects(Object a) { return a2os.get(a); }
	
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
    if (require_a2o) {
      Object old_a = o2a.get(o);
      if (old_a != null) { // There was an old abstraction there already
        if (old_a.equals(a)) return; // Haven't changed the abstraction
        TIntArrayList os = a2os.get(old_a);
        if (os != null) os.remove(os.indexOf(o));
      }
      TIntArrayList L = a2os.get(a);
      if (L == null) a2os.put(a, L = new TIntArrayList());
      L.add(o);
    }
		o2a.put(o, a);
		listener.abstractionChanged(o, a);
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
