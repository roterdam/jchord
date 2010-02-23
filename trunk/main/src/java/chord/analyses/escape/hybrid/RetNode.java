/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import java.util.Set;

import chord.util.ArraySet;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 *
 */
public class RetNode {
	final IntArraySet pts;
	final Set<IntTrio> heap;
	final IntArraySet esc;
	public RetNode(IntArraySet pts, Set<IntTrio> heap, IntArraySet esc) {
		this.pts = pts;
		this.heap = heap;
		this.esc = esc;
	}
	public int hashCode() {
		return heap.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof RetNode))
			return false;
		RetNode that = (RetNode) o;
		return pts.equals(that.pts) && esc.equals(that.esc) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "p@r=" + ThreadEscapeFullAnalysis.toString(pts) +
			  ";h@r=" + ThreadEscapeFullAnalysis.toString(heap) +
			  ";e@r=" + ThreadEscapeFullAnalysis.toString(esc);
	}
}
