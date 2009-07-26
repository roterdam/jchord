/*
 * Copyright (c) 2006-07, The Trustees of Stanford University.  All
 * rights reserved.
 * Licensed under the terms of the GNU GPL; see COPYING for details.
 */

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class T extends java.lang.Thread {
	static AAA g;
	public static void main(String[] a) {
		// bug goes if you uncomment this line T t = new T();
		AAA a1 = AAA.getNewInstance();
		a1.f = new Object();  // must be rightly deemed loc
		AAA a2 = AAA.getNewInstance();
		g = a2;
		a2.f = new Object();  // must be rightly deemed esc
		AAA a3 = AAA.getNewInstance();
		a3.f = new Object();  // must be falsely deemed esc
	}
}

class AAA {
	public static AAA getNewInstance() {
		return new AAA();
	}
	Object f;
	public AAA() {
		this.f = new Object();
	}
}


