/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Set related utilities.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class SetUtils {
	public static final int DEFAULT_SIZE = 10;
	public static final int THRESHOLD = 10;
	public static <T> IndexSet<T> newIndexSet() {
		return newIndexSet(DEFAULT_SIZE);
	}
	public static <T> IndexSet<T> newIndexSet(int size) {
		IndexSet<T> set;
		if (size < THRESHOLD) {
			set = new ArraySet<T>(size);
		} else {
			set = new IndexHashSet<T>(size);
		}
		return set;
	}
	public static <T> Set<T> newSet() {
		return newSet(DEFAULT_SIZE);
	}
	public static <T> Set<T> newSet(int size) {
		Set<T> set;
		if (size < THRESHOLD) {
			set = new ArraySet<T>(size);
		} else {
			set = new HashSet<T>(size);
		}
		return set;
	}
	public static <T> Set<T> newSet(Set<T> c) {
		Set<T> set = newSet(c.size());
		set.addAll(c);
		return set;
	}
    public static <T> Set<T> iterableToSet(Iterable<T> c, int size) {
		Set<T> set = newSet(size);
        for (T e : c)
            set.add(e);
        return set;
    }
}
