/*
 * Copyright (c) 2006-07, The Trustees of Stanford University.  All
 * rights reserved.
 * Licensed under the terms of the GNU GPL; see COPYING for details.
 */

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class T extends java.lang.Thread {
	static T g;
	T f;
	public static void main(String[] a) {
		T t1 = new T();
		T t2 = new T();
		t1.f = t2;
		g = t2;
		T t3 = new T();
		t2.f = t3;
	}
}

