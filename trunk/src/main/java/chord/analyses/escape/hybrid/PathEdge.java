/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class PathEdge {
	final Quad q;	// q == null => empty basic block
	final int qIdx; // q == null => qIdx == -1
	final int qId;
	final BasicBlock bb;	// basic block of quad
	final SD sd;
	public PathEdge(Quad q, int qIdx, BasicBlock bb, SD sd) {
		this.q = q;
		this.qIdx = qIdx;
		this.bb = bb;
		this.sd = sd;
		this.qId = q == null ? -1 : q.getID();
	}
	public int hashCode() {
		return qId;
	}
	public boolean equals(Object o) {
		// System.out.println("PE EQ");
		if (o == this)
			return true;
		if (!(o instanceof PathEdge))
			return false;
		PathEdge that = (PathEdge) o;
		return q == that.q && bb == that.bb &&
			sd.equals(that.sd);
	}
	public String toString() {
		return q + ";" + sd;
	}
}
