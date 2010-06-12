/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import chord.project.analyses.rhs.IEdge;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Edge implements IEdge {
	// may be null if this is a summary edge and srcExpr
	// represents return variable
	final Expr srcExpr;
	final Expr dstExpr;
	public Edge(Expr e1, Expr e2) {
		srcExpr = e1;
		dstExpr = e2;
	}
	@Override
	public boolean matchesSrcNodeOf(IEdge edge) {
		throw new RuntimeException();
	}
	@Override
	public boolean mergeWith(IEdge edge) {
		throw new RuntimeException();
	}
	@Override
	public int hashCode() {
		return srcExpr.hashCode() + dstExpr.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return srcExpr.equals(that.srcExpr) &&
			dstExpr.equals(that.dstExpr);
	}
	@Override
	public String toString() {
		return "[" + srcExpr.toString() + "," + dstExpr.toString() + "]";
	}
}
