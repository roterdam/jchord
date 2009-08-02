/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Data structure for indexing a set of objects by the order in which
 * the objects are added to the set.
 * <p>
 * Maintains an array list and a set (an array set or a hash set).
 * <p>
 * The only mutating operation is {@link #add(Object)}, in particular,
 * objects cannot be removed.
 * <p>
 * Provides constant-time operations for adding a given object,
 * testing membership of a given object, and getting the object at a
 * given index.
 * <p>
 * Provides O(1) access to the object at a given index by maintaining
 * a list.
 * 
 * @param	<T>	The type of the objects added.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IndexSet<T> extends ArrayList<T> implements Set<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5017833508291785850L;
	protected final Set<T> set;
    public IndexSet(int size) {
        super(size);
        set = SetUtils.newSet(size);
    }
	public IndexSet() {
		this(SetUtils.DEFAULT_SIZE);
	}
    public void clear() {
        set.clear();
    	super.clear();
    }
    public boolean contains(Object val) {
        return set.contains(val);
    }
    /**
     * Adds a given object unless it already exists.
     * 
     * @param	val	An object.
     * 
     * @return	true iff the given object did not already exist
     * 			and was successfully added.
     */
    public boolean add(T val) {
        if (set.add(val)) {
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
