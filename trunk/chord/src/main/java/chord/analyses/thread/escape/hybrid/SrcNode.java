/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.thread.escape.hybrid;

import java.util.Arrays;
import java.util.Set;

import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class SrcNode {
	final IntArraySet[] env;  // may contain null (i.e. escPts) elems
	final Set<IntTrio> heap;
	public SrcNode(IntArraySet[] e, Set<IntTrio> h) {
		env = e;
		heap = h;
	}
	public int hashCode() {
		return heap.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SrcNode))
			return false;
		SrcNode that = (SrcNode) o;
		return Arrays.equals(env, that.env) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "env@s=" + HybridThreadEscapeAnalysis.toString(env) +
			";heap@s=" + HybridThreadEscapeAnalysis.toString(heap);
	}
}
