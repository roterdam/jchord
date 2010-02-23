/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import java.util.Set;

import chord.util.ArraySet;
import chord.util.IntArraySet;
import chord.project.analyses.rhs.ISummaryEdge;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class SummaryEdge implements ISummaryEdge {
	final SrcNode srcNode;
    // retNode is intentionally not final: it is updated when this smry edge
	// is merged with another smry edge with matching srcNode; see mergeWith
	RetNode retNode;
	public SummaryEdge(SrcNode s, RetNode r) {
		srcNode = s;
		retNode = r;
	}
	public boolean matchesSrcNodeOf(ISummaryEdge se2) {
		SrcNode srcNode2 = ((SummaryEdge) se2).srcNode;
		return srcNode.equals(srcNode2);
	}
	public boolean mergeWith(ISummaryEdge se2) {
		RetNode retNode1 = this.retNode;
		RetNode retNode2 = ((SummaryEdge) se2).retNode;
        boolean changed = false;
		// merge pts's
		IntArraySet pts1 = retNode1.pts;
        IntArraySet pts2 = retNode2.pts;
        if (!pts1.equals(pts2)) {
			if (pts2 != ThreadEscapeFullAnalysis.nilPts) {
				if (pts1 == ThreadEscapeFullAnalysis.nilPts)
					pts1 = pts2;
				else {
					pts1 = new IntArraySet(pts1);
					pts1.addAll(pts2);
				}
            	changed = true;
			}
        }
		// merge esc's
        IntArraySet esc1 = retNode1.esc;
        IntArraySet esc2 = retNode2.esc;
        if (!esc1.equals(esc2)) {
			if (esc2 != ThreadEscapeFullAnalysis.nilPts) {
				if (esc1 == ThreadEscapeFullAnalysis.nilPts)
					esc1 = esc2;
				else {
					esc1 = new IntArraySet(esc1);
					esc1.addAll(esc2);
				}
            	changed = true;
			}
        }
		// merge heaps
        Set<IntTrio> heap1 = retNode1.heap;
        Set<IntTrio> heap2 = retNode2.heap;
        if (!heap1.equals(heap2)) {
            heap1 = new ArraySet<IntTrio>(heap1);
            heap1.addAll(heap2);
            changed = true;
        }
		if (changed) {
			this.retNode = new RetNode(pts1, heap1, esc1);
			return true;
		}
		return false;
	}
	public int hashCode() {
		// return srcNode.hashCode() + retNode.hashCode(); 
		return 0;
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SummaryEdge))
			return false;
		SummaryEdge that = (SummaryEdge) o;
		return srcNode.equals(that.srcNode) && retNode.equals(that.retNode);
	}
	public String toString() {
		return srcNode + ";" + retNode;
	}
}
