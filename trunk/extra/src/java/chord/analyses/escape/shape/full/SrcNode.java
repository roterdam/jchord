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
public class SrcNode {
	final Obj[] env;
	final ArraySet<FldObj> heap;
	public SrcNode(Obj[] env, ArraySet<FldObj> heap) {
		this.env = env;
		this.heap = heap;
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
		if (!(o instanceof SrcNode))
			return false;
		SrcNode that = (SrcNode) o;
		int n = env.length;
		for (int i = 0; i < n; i++) {
			if (env[i] != that.env[i])
				return false;
		}
		return heap.equals(that.heap);
	}
	public String toString() {
		return "v@s=" + ThreadEscapeFullAnalysis.toString(env) +
			 "; h@s=" + ThreadEscapeFullAnalysis.toString(heap);
	}
}
