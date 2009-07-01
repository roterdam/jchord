/*
 * Copyright (c) 2006-07, The Trustees of Stanford University.  All
 * rights reserved.
 * Licensed under the terms of the GNU GPL; see COPYING for details.
 */

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class T extends java.lang.Thread {
	public static void main(String[] a) {
		MyB b1 = new MyB();
		MyB b2 = new MyB();
		for (int i = 0; i < 2; i++) {
			T s = new T(b1, b2);
			begin(s);
		}
		for (int i = 0; i < 2; i++) {
			int j = b1.get();
		}
	}
	private MyB f1, f2;
	public T(MyB b1, MyB b2) {
		this.f1 = b1;
		this.f2 = b2;
	}
	public void run() {
		MyB b1 = this.f1;
        MyB b2 = this.f2;
		System.out.println(b1);
		System.out.println(b2);
	}
	public static void begin(T t) {
		t.start();
	}
}

