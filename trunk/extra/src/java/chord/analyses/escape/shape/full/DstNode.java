/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.shape.full;

import chord.util.ArraySet;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DstNode {
	final Obj[] env;
	final ArraySet<FldObj> heap;
	final boolean isKill;
	final boolean isRetn;
	public DstNode(Obj[] env, ArraySet<FldObj> heap, boolean isKill, boolean isRetn) {
		this.env = env;
		this.heap = heap;
		this.isKill = isKill;
		this.isRetn = isRetn;
	}
	public int hashCode() {
        int i = 5381;
        for (Obj pts : env) {
            i = ((i << 5) + i) + pts.hashCode();
        }
        return i;
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof DstNode))
			return false;
		DstNode that = (DstNode) o;
		int n = env.length;
		for (int i = 0; i < n; i++) {
			if (env[i] != that.env[i])
				return false;
		}
		return heap.equals(that.heap) && isKill == that.isKill && isRetn == that.isRetn;
	}
	public String toString() {
		return "v@d=" + ThreadEscapeFullAnalysis.toString(env) +
			 "; h@d=" + ThreadEscapeFullAnalysis.toString(heap) +
			 "; k@d=" + isKill + "; r@d: " + isRetn;
	}
}

