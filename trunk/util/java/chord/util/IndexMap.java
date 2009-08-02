/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Data structure for indexing a set of objects by the order in which
 * the objects are added to the set.
 * <p>
 * Maintains an array list and a hash map.
 * <p>
 * The only mutating operations are {@link #getOrAdd(Object)} and
 * {@link #add(Object)}, in particular, objects cannot be removed.
 * <p>
 * Provides constant-time operations for adding a given object,
 * testing membership of a given object, getting the index of a given
 * object, and getting the object at a given index.
 * <p>
 * Provides O(1) access to the object at a given index by maintaining
 * a list.
 * <p>
 * Provides O(1) access to the index of a given object by maintaining
 * a hash map.
 * 
 * @param	<T>	The type of the objects added.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IndexMap<T> extends ArrayList<T> implements Set<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2754948912995001350L;
	protected final Map<T, Integer> map;
    public IndexMap(int size) {
        super(size);
        map = new HashMap<T, Integer>(size);
    }
	public IndexMap() {
        map = new HashMap<T, Integer>();
	}
    public void clear() {
        map.clear();
    	super.clear();
    }
    public boolean contains(Object val) {
        return map.containsKey(val);
    }
    /**
     * Provides the index of a given object, if it exists, and -1
     * otherwise.
     * <p>
     * Takes O(1) time.
     * 
     * @param	val	An object.
     * 
     * @return	The index of the given object, if it exists,
     * 			and -1 otherwise.
     */
    public int indexOf(Object val) {
    	Integer idx = map.get(val);
    	if (idx == null)
			return -1;
        return idx.intValue();
    }
    /**
     * Adds and indexes a given object, unless it already exists,
     * and provides its index in both cases.
     * 
     * @param	val	An object.
     * 
     * @return	The index of the given object.
     */
    public int getOrAdd(T val) {
        Integer idx = map.get(val);
        if (idx == null) {
        	idx = new Integer(size());
            map.put(val, idx);
            super.add(val);
        }
        return idx.intValue();
    }
    /**
     * Adds and indexes a given object unless it already exists.
     * 
     * @param	val	An object.
     * 
     * @return	true iff the given object did not already exist
     * 			and was successfully added and indexed.
     */
    public boolean add(T val) {
        Integer idx = map.get(val);
        if (idx == null) {
        	idx = new Integer(size());
            map.put(val, idx);
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
