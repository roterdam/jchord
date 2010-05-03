/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import java.util.Set;

import joeq.Compiler.Quad.Quad;

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
			dstExpr.equals(that.dstExpr);
	}
	public String toString() {
		return srcExpr.toString() + "," + dstExpr.toString();
	}
}
