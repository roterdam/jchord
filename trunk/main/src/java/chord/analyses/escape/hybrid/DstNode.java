/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import java.util.Arrays;
import java.util.Set;

import chord.util.ArraySet;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DstNode {
	IntArraySet[] env;
	Set<IntTrio> heap;
	IntArraySet esc;
	public void check() {
		assert (!esc.contains(ThreadEscapeFullAnalysis.ESC_VAL));
		for (IntTrio t : heap) {
			assert(!esc.contains(t.idx0));
			assert(!esc.contains(t.idx2));
		}
		int n = env.length;
		for (int i = 0; i < n; i++) {
			assert(!esc.overlaps(env[i]));
		}
	}
	public DstNode(IntArraySet[] e, Set<IntTrio> h, IntArraySet e2) {
		env = e;
		heap = h;
		esc = e2;
	}
	public boolean mergeWith(DstNode dstNode2) {
		boolean changed = false;
        IntArraySet esc2 = dstNode2.esc;
        if (!esc.equals(esc2)) {
            esc = new IntArraySet(esc);
            esc.addAll(esc2);
            changed = true;
        }
		IntArraySet[] env2 = dstNode2.env;
		int n = env.length;
		assert (n == env2.length);
		IntArraySet[] env3 = null;
		for (int i = 0; i < n; i++) {
			IntArraySet pts = env[i];
			IntArraySet pts2 = env2[i];
			if (!pts.equals(pts2)) {
				IntArraySet pts3 = new IntArraySet(pts);
				pts3.addAll(pts2);
				if (env3 == null) {
					env3 = new IntArraySet[n];
					for (int j = 0; j < i; j++)
						env3[j] = env[j];
				}
				env3[i] = pts3;
			} else if (env3 != null)
				env3[i] = pts;
		}
		if (env3 != null) {
			env = env3;
			changed = true;
		}
        Set<IntTrio> heap2 = dstNode2.heap;
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
