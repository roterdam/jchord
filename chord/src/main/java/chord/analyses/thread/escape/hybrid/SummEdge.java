/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.thread.escape.hybrid;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class SummEdge {
	final SrcNode srcNode;
	final RetNode retNode;
	public SummEdge(SrcNode s, RetNode r) {
		srcNode = s;
		retNode = r;
	}
	public int hashCode() {
		return srcNode.hashCode() + retNode.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SummEdge))
			return false;
		SummEdge that = (SummEdge) o;
		return srcNode.equals(that.srcNode) &&
			retNode.equals(that.retNode);
	}
	public String toString() {
		return srcNode + ";" + retNode;
	}
}
