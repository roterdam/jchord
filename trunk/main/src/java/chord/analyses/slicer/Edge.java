/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import chord.project.analyses.rhs.IEdge;
import chord.util.CompareUtils;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Edge implements IEdge {
	final Expr srcExpr;
	final Expr dstExpr;
	final boolean affectedSlice;
	public Edge(Expr e1, Expr e2, boolean _affectedSlice) {
		assert (e1 != null);
		srcExpr = e1;
		if (e2 == null)
			assert(_affectedSlice);
		dstExpr = e2;
		this.affectedSlice = _affectedSlice;
	}
	public boolean matchesSrcNodeOf(IEdge edge) {
		throw new RuntimeException();
	}
	public boolean mergeWith(IEdge edge) {
		throw new RuntimeException();
	}
	public int hashCode() {
		return srcExpr.hashCode() + (dstExpr == null ? 0 : dstExpr.hashCode());
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return srcExpr.equals(that.srcExpr) &&
			   CompareUtils.areEqual(dstExpr, that.dstExpr) &&
			   affectedSlice == that.affectedSlice;
	}
	public String toString() {
		return "[" + srcExpr.toString() + "," +
			(dstExpr == null ? "null" : dstExpr.toString()) + "," +
			affectedSlice + "]";
	}
}
