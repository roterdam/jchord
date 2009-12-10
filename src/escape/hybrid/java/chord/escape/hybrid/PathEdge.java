/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.escape.hybrid;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class PathEdge {
	final SrcNode srcNode;
	final DstNode dstNode;
	public PathEdge(SrcNode s, DstNode d) {
		srcNode = s;
		dstNode = d;
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
