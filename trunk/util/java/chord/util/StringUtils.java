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
	public static String trimNumSuffix(String s) {
		int i = s.length() - 1;
		while (Character.isDigit(s.charAt(i)))
			i--;
		return s.substring(0, i + 1);
	}
	public static String[] concat(String[] a, String[] b) {
		String[] c = new String[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}
