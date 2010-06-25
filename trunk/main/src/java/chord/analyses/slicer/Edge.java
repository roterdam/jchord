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
	public Edge(Expr e1, Expr e2, boolean affectedSlice) {
		assert (e1 != null);
		srcExpr = e1;
		assert (e2 != null);
		dstExpr = e2;
		this.affectedSlice = affectedSlice;
	}
	public boolean matchesSrcNodeOf(IEdge edge) {
		throw new RuntimeException();
	}
	public boolean mergeWith(IEdge edge) {
		throw new RuntimeException();
	}
	public int hashCode() {
		return srcExpr.hashCode() + dstExpr.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return srcExpr.equals(that.srcExpr) &&
			   dstExpr.equals(that.dstExpr) &&
			   affectedSlice == that.affectedSlice;
	}
	public String toString() {
		return "[" + srcExpr.toString() + "," + dstExpr.toString() + "," + affectedSlice + "]";
	}
}
