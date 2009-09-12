/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * Object comparison utilities.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CompareUtils {
	public static boolean areEqual(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}
}
