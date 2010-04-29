/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import java.util.Set;

import joeq.Compiler.Quad.Quad;

import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Edge implements IEdge {
	final Expr srcExpr;
    // dstExprs and dstQuads are intentionally not final: they are
	// updated when this edge is merged with another edge with
	// matching srcNode; see mergeWith
	// dstExprs and dstQuads are unmodifiable sets, i.e., their
	// contents must never be changed, since they may be shared by
	// different edges.
	Set<Expr> dstExprs;
	Set<Quad> dstQuads;
	public Edge(Expr expr, Set<Expr> exprs, Set<Quad> quads) {
		srcExpr = expr;
		dstExprs = exprs;
		dstQuads = quads;
	}
	public boolean matchesSrcNodeOf(IEdge edge) {
		return srcExpr.equals(((Edge) edge).srcExpr);
	}
	public boolean mergeWith(IEdge edge) {
        boolean changed = false;
		Edge that = (Edge) edge;
		Set<Quad> dstQuads1 = this.dstQuads;
		Set<Quad> dstQuads2 = that.dstQuads;
		if (!dstQuads1.equals(dstQuads2)) {
			dstQuads1 = new ArraySet<Quad>(dstQuads1);
			dstQuads1.addAll(dstQuads2);
			changed = true;
		}
		// todo: can return in the 'else' case above
		// as dstExprs is a function of dstQuads
        Set<Expr> dstExprs1 = this.dstExprs;
        Set<Expr> dstExprs2 = that.dstExprs;
        if (!dstExprs1.equals(dstExprs2)) {
			dstExprs1 = new ArraySet<Expr>(dstExprs1);
			dstExprs1.addAll(dstExprs2);
           	changed = true;
        }
		if (changed) {
			this.dstExprs = dstExprs1;
			this.dstQuads = dstQuads1;
			return true;
		}
		return false;
	}
	public int hashCode() {
		return 0;
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return srcExpr.equals(that.srcExpr) &&
			dstQuads.equals(that.dstQuads) &&
  		// todo: perhaps can remove below line since
		// dstExprs is a function of dstQuads
			dstExprs.equals(that.dstExprs);
	}
	public String toString() {
		String s = srcExpr.toString() + ",[";
		for (Expr e : dstExprs)
			s += e.toString();
		s += "],[";
		for (Quad q : dstQuads)
			s += q.toString();
		return s + "]";
	}
}
