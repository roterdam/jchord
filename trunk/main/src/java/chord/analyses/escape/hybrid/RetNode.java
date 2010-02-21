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
	IntArraySet pts;
	Set<IntTrio> heap;
	IntArraySet esc;
	public RetNode(IntArraySet p, Set<IntTrio> h, IntArraySet e) {
		pts = p;
		heap = h;
		esc = e;
	}
	public boolean mergeWith(RetNode retNode2) {
		boolean changed = false;
		IntArraySet pts2 = retNode2.pts;
		if (!pts.equals(pts2)) {
			pts = new IntArraySet(pts);
			pts.addAll(pts2);
			changed = true;
		}
		IntArraySet esc2 = retNode2.esc;
		if (!esc.equals(esc2)) {
			esc = new IntArraySet(esc);
			esc.addAll(esc2);
			changed = true;
		}
		Set<IntTrio> heap2 = retNode2.heap;
		if (!heap.equals(heap2)) {
			heap = new ArraySet<IntTrio>(heap);
			heap.addAll(heap2);
			changed = true;
		}
		return changed;
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
		return pts.equals(that.pts) &&
			esc.equals(that.esc) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "pts@r=" + ThreadEscapeFullAnalysis.toString(pts) +
			";heap@r=" + ThreadEscapeFullAnalysis.toString(heap) +
			";esc@r=" + ThreadEscapeFullAnalysis.toString(esc);
	}
}
