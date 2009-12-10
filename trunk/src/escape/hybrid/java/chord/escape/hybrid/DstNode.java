/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.escape.hybrid;

import java.util.Arrays;
import java.util.Set;

import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DstNode {
	final IntArraySet[] env;
	final Set<IntTrio> heap;
	final IntArraySet esc;
	public DstNode(IntArraySet[] e, Set<IntTrio> h, IntArraySet e2) {
		env = e;
		heap = h;
		esc = e2;
	}
	public int hashCode() {
		return heap.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof DstNode))
			return false;
		DstNode that = (DstNode) o;
		return Arrays.equals(env, that.env) &&
			esc.equals(that.esc) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "env@d=" + ThreadEscapeFullAnalysis.toString(env) +
			"; heap@d=" + ThreadEscapeFullAnalysis.toString(heap) +
			"; esc@d=" + ThreadEscapeFullAnalysis.toString(esc);
	}
}
