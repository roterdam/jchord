/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import gnu.trove.TIntArrayList;

/**
 * Array-based implementation of a set.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IntArraySet extends TIntArrayList {
	public IntArraySet(IntArraySet c) {
		addAll(c);
	}
	public IntArraySet() { }
	public IntArraySet(int initialCapacity) {
		super(initialCapacity);
	}
	public boolean add(int e) {
		if (contains(e))
			return false;
		return super.add(e);
	}
	/**
	 * Adds a given value to the set without checking if it already
	 * exists in the set.
	 * 
	 * @param	e	A value to be added to the set.
	 */
	public void addForcibly(int e) {
		super.add(e);
	}
	public boolean addAll(IntArraySet c) {
		boolean modified = false;
		int n = c.size();
		for (int i = 0; i < n; i++) {
			int e = c.get(i);
			if (add(e))
				modified = true;
		}
		return modified;
	}
	public boolean overlaps(IntArraySet c) {
		int n = c.size();
		for (int i = 0; i < n; i++) {
			int e = c.get(i);
			if (contains(e))
				return true;
		}
		return false;
	}
    public boolean equals(Object o) {
   		if (o == this)
        	return true;
    	if (!(o instanceof IntArraySet))
        	return false;
    	IntArraySet c = (IntArraySet) o;
		int n = c.size();
    	if (n != size())
        	return false;
		for (int i = 0; i < n; i++) {
			int e = c.get(i);
			if (!contains(e))
				return false;
		}
		return true;
    }
	public int hashCode() {
		int h = 0;
		int n = size();
		for (int i = 0; i < n; i++) {
			int e = get(i);
			h += e;
		}
		return h;
	}
}

