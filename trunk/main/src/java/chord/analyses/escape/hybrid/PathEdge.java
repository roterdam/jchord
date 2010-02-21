/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import chord.project.analyses.rhs.IPathEdge;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class PathEdge implements IPathEdge {
	final SrcNode srcNode;
	final DstNode dstNode;
	public PathEdge(SrcNode s, DstNode d) {
		srcNode = s;
		dstNode = d;
	}
	public boolean matchesSrcNodeOf(IPathEdge pe2) {
		SrcNode srcNode2 = ((PathEdge) pe2).srcNode;
		return srcNode.equals(srcNode2);
	}
	public boolean mergeWith(IPathEdge pe2) {
		return dstNode.mergeWith(((PathEdge) pe2).dstNode);
	}
	public int hashCode() {
		return srcNode.hashCode() + dstNode.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof PathEdge))
			return false;
		PathEdge that = (PathEdge) o;
		return srcNode.equals(that.srcNode) &&
			dstNode.equals(that.dstNode);
	}
	public String toString() {
		return srcNode + ";" + dstNode;
	}
}
