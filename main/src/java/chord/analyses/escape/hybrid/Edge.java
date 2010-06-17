/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import java.util.Set;

import chord.util.ArraySet;
import chord.util.IntArraySet;
import chord.project.analyses.rhs.IEdge;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Edge implements IEdge {
	final SrcNode srcNode;
	// dstNode is intentionally not final: it is updated when this edge
	// is merged with another edge with matching srcNode; see mergeWith
	DstNode dstNode;
	public Edge(SrcNode s, DstNode d) {
		srcNode = s;
		dstNode = d;
	}
	public boolean matchesSrcNodeOf(IEdge pe2) {
		SrcNode srcNode2 = ((Edge) pe2).srcNode;
		return srcNode.equals(srcNode2);
	}
	public boolean mergeWith(IEdge pe2) {
		DstNode dstNode1 = this.dstNode;
		DstNode dstNode2 = ((Edge) pe2).dstNode;
		boolean isRet1 = dstNode1.isRet;
		boolean isRet2 = dstNode2.isRet;
		assert (isRet1 == isRet2);
        boolean changed = false;
		// merge esc's
		IntArraySet esc1 = dstNode1.esc;
        IntArraySet esc2 = dstNode2.esc;
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
		// merge env's
        IntArraySet[] env1 = dstNode1.env;
        IntArraySet[] env2 = dstNode2.env;
        int n = env1.length;
        assert (n == env2.length);
        IntArraySet[] env3 = null;
        for (int i = 0; i < n; i++) {
            IntArraySet pts1 = env1[i];
            IntArraySet pts2 = env2[i];
            if (pts1.equals(pts2)) {
				if (env3 != null)
					env3[i] = pts1;
				continue;
			}
			if (pts2 == ThreadEscapeFullAnalysis.nilPts) {
				if (env3 != null)
					env3[i] = pts1;
				continue;
			}
			if (pts1 == ThreadEscapeFullAnalysis.nilPts)
				pts1 = pts2;
			else {
				pts1 = new IntArraySet(pts1);
				pts1.addAll(pts2);
			}
			if (env3 == null) {
				env3 = new IntArraySet[n];
				for (int j = 0; j < i; j++)
					env3[j] = env1[j];
			}
			env3[i] = pts1;
        }
        if (env3 != null) {
            env1 = env3;
            changed = true;
        }
		// merge heaps
        Set<IntTrio> heap1 = dstNode1.heap;
        Set<IntTrio> heap2 = dstNode2.heap;
        if (!heap1.equals(heap2)) {
            heap1 = new ArraySet<IntTrio>(heap1);
            heap1.addAll(heap2);
            changed = true;
        }
		if (changed) {
			this.dstNode = new DstNode(env1, heap1, esc1, isRet1);
			return true;
		}
        return false;
	}
	public int hashCode() {
		return srcNode.hashCode() + dstNode.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Edge))
			return false;
		Edge that = (Edge) o;
		return srcNode.equals(that.srcNode) && dstNode.equals(that.dstNode);
	}
	public String toString() {
		return srcNode + ";" + dstNode;
	}
}
