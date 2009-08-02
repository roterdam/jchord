/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Implementation for indexing a set of objects by the order in which
 * the objects are added to the set.
 * <p>
 * Maintains an array list and a hash set.
 * <p>
 * Provides O(1) access to the object at a given index by maintaining
 * an array list.
 * <p>
 * Provides O(1) membership testing for a given object by maintaining
 * a hash set.
 * 
 * @param	<T>	The type of the objects.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IndexHashSet<T> extends ArrayList<T> implements IndexSet<T> {
	protected final HashSet<T> hashSet;
    public IndexHashSet(int size) {
        super(size);
        hashSet = new HashSet<T>(size);
    }
	public IndexHashSet() {
		hashSet = new HashSet<T>();
	}
    public void clear() {
    	super.clear();
        hashSet.clear();
    }
    public boolean contains(Object val) {
        return hashSet.contains(val);
    }
    /**
     * Adds a given object, unless it already exists, in O(1) time.
     * 
     * @param	val	An object.
     * 
     * @return	true iff the given object did not already exist and
     * 			was successfully added.
     */
    public boolean add(T val) {
        if (hashSet.add(val)) {
        	super.add(val);
        	return true;
        }
        return false;
    }
    public T remove(int idx) {
    	throw new RuntimeException();
    }
    public T set(int idx, T val) {
    	throw new RuntimeException();
    }
    public void add(int idx, T val) {
    	throw new RuntimeException();
    }
    public boolean addAll(Collection<? extends T> c) {
    	throw new RuntimeException();
    }
    public boolean remove(Object val) {
    	throw new RuntimeException();
    }
    public boolean removeAll(Collection<?> c) {
    	throw new RuntimeException();
    }
    public boolean retainAll(Collection<?> c) {
    	throw new RuntimeException();
    }
}
