/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * Array related utilities.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ArrayUtils {
	/**
	 * Determines whether a given array contains a given value.
	 * 
	 * @param	<T>	The type of the array elements and the value
	 * 			to be checked for containment in the array.
	 * @param	a	An array.
	 * @param	s	A value to be checked for containment in the
	 * 			given array.
	 * 
	 * @return	true iff the given array contains the given value.
	 */
	public static <T> boolean contains(T[] a, T s) {
		for (T t : a) {
			if (t == null) {
				if (s == null)
					return true;
			} else if (s != null && t.equals(s))
				return true;
		}
		return false;
	}
	/**
	 * Determines whether a given array contains duplicate values.
	 * 
	 * @param	<T>	The type of the array elements.
	 * @param	a	An array.
	 * 
	 * @return	true iff the given array contains duplicate values.
	 */
	public static <T> boolean hasDuplicates(T[] a) {
		for (int i = 0; i < a.length - 1; i++) {
			T elem = a[i];
			for (int j = i + 1; j < a.length; j++) {
				T elem2 = a[j];
				if (elem.equals(elem2))
					return true;
			}
		}
		return false;
	}
	public static <T> String toString(T[] a,
			String begStr, String sepStr, String endStr) {
		int n = a.length;
		if (n == 0)
			return begStr + endStr;
		String s = begStr + a[0];
		for (int i = 1; i < n; i++) {
			s += sepStr + a[i];
		}
		return s + endStr;
	}
	public static <T> String toString(T[] a) {
		return toString(a, "", ",", "");
	}
}
