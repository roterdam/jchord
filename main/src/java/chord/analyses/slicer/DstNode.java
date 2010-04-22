/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

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
	public DstNode(IntArraySet[] env, Set<IntTrio> heap, IntArraySet esc) {
		this.env = env;
		this.heap = heap;
		this.esc = esc;
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
		return "v@d=" + Slicer.toString(env) +
			 "; h@d=" + Slicer.toString(heap) +
			 "; e@d=" + Slicer.toString(esc);
	}
}
