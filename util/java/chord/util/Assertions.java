/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

/**
 * Error handling utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Assertions {
	public static void Assert(boolean cond) {
		if (!cond) {
			throw new RuntimeException("");
		}
	}
	public static void Assert(boolean cond, String mesg) {
		if (!cond) {
			throw new RuntimeException(mesg);
		}
	}
}
