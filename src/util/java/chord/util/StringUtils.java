/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * String related utilities.
 *  
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class StringUtils {
	/**
	 * Trim the numerical suffix of the given string (e.g. convert
	 * "abc123xyz456 to "abc123xyz").
	 *
	 * @param	s	The string whose numerical suffix is to be trimmed.
	 * @return	A copy of the given string without any numerical
	 * 			suffix.
	 */
	public static String trimNumSuffix(String s) {
		int i = s.length() - 1;
		while (Character.isDigit(s.charAt(i)))
			i--;
		return s.substring(0, i + 1);
	}
	/**
	 * Create an array of strings by concatenating two given arrays
	 * of strings.
	 * 
	 * @param	a	An array of strings.
	 * @param	b	An array of strings.
	 * @return	A new array of strings containing those in <tt>a</tt>
	 * 			followed by those in <tt>b</tt>.
	 */
	public static String[] concat(String[] a, String[] b) {
		String[] c = new String[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}
