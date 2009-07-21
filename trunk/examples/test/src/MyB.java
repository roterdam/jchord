/*
 * Copyright (c) 2006-07, The Trustees of Stanford University.  All
 * rights reserved.
 * Licensed under the terms of the GNU GPL; see COPYING for details.
 */

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class MyB {
	private MyA bf;
	public MyB() {
		MyA a = new MyA();
		this.bf = a;
	}
	public int get() {
		MyA a = this.bf;
		return a.get();
	}
	public void set(int i) {
		MyA a = this.bf;
		a.set(i);
	}
}
