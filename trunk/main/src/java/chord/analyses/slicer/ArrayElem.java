/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import joeq.Compiler.Quad.Quad;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ArrayElem implements Expr {
	public final Quad q;
	public ArrayElem(Quad q) {
		this.q = q;
	}
	@Override
	public int hashCode() {
		return q.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof ArrayElem) {
			ArrayElem e = (ArrayElem) o;
			return e.q == this.q;
		}
		return false;
	}
	@Override
	public String toString() {
		return q.toString();
	}
}
