/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Collection related utilities.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CollectionUtils {
	/**
	 * Determines whether a given collection contains duplicate
	 * values.
	 * 
	 * @param	<T>	The type of the collection elements.
	 * @param	elems	A collection.
	 * 
	 * @return	true iff the given collection contains duplicate
	 * 			values.
	 */
	public static <T> boolean hasDuplicates(List<T> elems) {
		for (int i = 0; i < elems.size() - 1; i++) {
			T elem = elems.get(i);
			for (int j = i + 1; j < elems.size(); j++) {
				if (elems.get(j).equals(elem))
					return true;
			}
		}
		return false;
	}
	public static <T> String toString(Collection<T> a,
			String begStr, String sepStr, String endStr) {
		int n = a.size();
		if (n == 0)
			return begStr + endStr;
		Iterator<T> it = a.iterator();
		String s = begStr + it.next();
		while (it.hasNext())
			s += sepStr + it.next();
		return s + endStr;
	}
	public static <T> String toString(Collection<T> a) {
		return toString(a, "", ",", "");
	}
}
