/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class InstField implements Expr {
	public final Quad q;
	public final jq_Field f;
	public InstField(Quad q, jq_Field f) {
		this.q = q;
		this.f = f;
	}
	@Override
	public int hashCode() {
		return q.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof InstField) {
			InstField e = (InstField) o;
			return e.q == this.q && e.f == this.f;
		}
		return false;
	}
	@Override
	public String toString() {
		return "<" + f.toString() + "," + q.toString() + ">";
	}
}
