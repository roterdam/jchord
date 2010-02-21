/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import chord.project.ISummaryEdge;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class SummaryEdge implements ISummaryEdge {
	final SrcNode srcNode;
	final RetNode retNode;
	public SummaryEdge(SrcNode s, RetNode r) {
		srcNode = s;
		retNode = r;
	}
	public boolean matchesSrcNodeOf(ISummaryEdge se2) {
		SrcNode srcNode2 = ((SummaryEdge) se2).srcNode;
		return srcNode.equals(srcNode2);
	}
	public boolean mergeWith(ISummaryEdge se2) {
		return retNode.mergeWith(((SummaryEdge) se2).retNode);
	}
	public int hashCode() {
		return 0; // srcNode.hashCode() + retNode.hashCode(); 
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SummaryEdge))
			return false;
		SummaryEdge that = (SummaryEdge) o;
		return srcNode.equals(that.srcNode) &&
			retNode.equals(that.retNode);
	}
	public String toString() {
		return srcNode + ";" + retNode;
	}
}
