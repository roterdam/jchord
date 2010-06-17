/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class LocalVar implements Expr {
	public final Register v;
	public LocalVar(Register v) {
		this.v = v;
	}
	@Override
	public int hashCode() {
		return v.getNumber();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof LocalVar) {
			LocalVar e = (LocalVar) o;
			return e.v == this.v;
		}
		return false;
	}
	@Override
	public String toString() {
		return v.toString();
	}
}
