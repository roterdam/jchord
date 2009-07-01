/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for mapping each of N objects to a unique integer in the
 * range [0..N-1].
 * <p>
 * The integers are assigned in the order in which the objects are
 * added.
 * <p>
 * Provides O(1) access to the object given the corresponding integer
 * and vice versa.
 * 
 * @param	<T>	The type of the objects to which the integers are
 *			assigned.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IndexMap<T> implements java.io.Serializable, Iterable<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5450801013115547255L;
	protected final Map<T, Integer> hash;
    protected final List<T> list;
    public IndexMap(int size) {
        hash = new HashMap<T, Integer>(size);
        list = new ArrayList<T>(size);
    }
	public IndexMap() {
        hash = new HashMap<T, Integer>();
        list = new ArrayList<T>();
	}
    /**
     * Provides the total number of objects in the map.
     * 
     * @return	The total number of objects in the map.
     */
    public int size() {
    	return list.size();
    }
    /**
     * Removes all objects from the index.
     */
    public void clear() {
        hash.clear();
        list.clear();
    }
    /**
     * Determines whether a given object is present in the map.
     * 
     * @param	val	An object.
     * 
     * @return	true iff the given object is present in the map.
     */
    public boolean contains(T val) {
        return hash.containsKey(val);
    }
    /**
     * Provides the integer mapped to a given object in the map,
     * if it exists, and -1 otherwise.
     * 
     * @param	val	An object.
     * 
     * @return	The integer mapped to the given object in the map,
     * 			if it exists, and -1 otherwise.
     */
    public int get(T val) {
    	Integer idx = hash.get(val);
    	if (idx == null)
			return -1;
        return idx.intValue();
    }
    /**
     * Provides the object mapped to a given integer in the map,
     * if it exists, and null otherwise.
	 *
     * @param	idx	An integer.
     * 
     * @return	The object mapped to the given integer in the map,
     * 			if it exists, and null otherwise.
     */
    public T get(int idx) {
    	try {
    		return list.get(idx);
    	} catch (IndexOutOfBoundsException e) {
			return null;
    	}
    }
    /**
     * Provides the integer mapped to a given object in the map,
     * if it exists, and maps the object to a new unique integer
     * otherwise.
     * 
     * @param	val	An object.
     * 
     * @return	The integer mapped to the given object in the map.
     */
    public int set(T val) {
        Integer idx = hash.get(val);
        if (idx == null) {
        	idx = new Integer(list.size());
            hash.put(val, idx);
            list.add(val);
        }
        return idx.intValue();
    }
    /**
     * Provides the set of all objects in the map.
     * 
     * @return	The set of all objects in the map.
     */
    public Set<T> keySet() {
    	return hash.keySet();
    }
    /**
     * Provides an iterator over the objects in the map.
     * <p>
     * The objects are iterated in the order in which they were
     * added to the map or, equivalently, in increasing order of
     * the integers assigned to the objects in the map.
     */
    public Iterator<T> iterator() {
    	return list.iterator();
    }
/*
    private class Itr implements Iterator<T> {
    	int idx = 0;
    	public boolean hasNext() {
    		return idx < list.size();
    	}
    	public T next() {
    		T val = list.get(idx);
    		idx++;
    		return val;
    	}
    	public void remove() {
    		throw new UnsupportedOperationException(
    			"remove not supported in IndexMap iterator.");
    	}
    }
*/
}
