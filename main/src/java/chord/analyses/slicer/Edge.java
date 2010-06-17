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
	// srcExpr/dstExpr may be null if expression represents
	// return variable
	final Expr srcExpr;
	final Expr dstExpr;
	public Edge(Expr e1, Expr e2) {
		srcExpr = e1;
		dstExpr = e2;
	}
	public boolean matchesSrcNodeOf(IEdge edge) {
		throw new RuntimeException();
	}
	public boolean mergeWith(IEdge edge) {
		throw new RuntimeException();
	}
	public int hashCode() {
		return
			(srcExpr == null ? 0 : srcExpr.hashCode()) +
			(dstExpr == null ? 0 : dstExpr.hashCode());
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return CompareUtils.areEqual(srcExpr, that.srcExpr) &&
			   CompareUtils.areEqual(dstExpr, that.dstExpr);
	}
	public String toString() {
		return "[" +
			(srcExpr == null ? "null" : srcExpr.toString()) + "," +
			(dstExpr == null ? "null" : dstExpr.toString()) + "]";
	}
}
