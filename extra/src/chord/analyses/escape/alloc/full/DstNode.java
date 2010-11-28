/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.alloc.full;

import java.util.Arrays;
import java.util.Set;

import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DstNode {
	final boolean isRet;
	final IntArraySet[] env;
	final Set<IntTrio> heap;
	final IntArraySet esc;
	public DstNode(IntArraySet[] env, Set<IntTrio> heap,
			IntArraySet esc, boolean isRet) {
		this.env = env;
		this.heap = heap;
		this.esc = esc;
		this.isRet = isRet;
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
		return Arrays.equals(env, that.env) && esc.equals(that.esc) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "v@d=" + ThreadEscapeFullAnalysis.toString(env) +
			 "; h@d=" + ThreadEscapeFullAnalysis.toString(heap) +
			 "; e@d=" + ThreadEscapeFullAnalysis.toString(esc);
	}
}
