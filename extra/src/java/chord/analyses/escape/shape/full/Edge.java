/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.shape.full;

import java.util.Set;

import joeq.Class.jq_Field;
import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

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
		boolean isRetn1 = dstNode1.isRetn;
		boolean isRetn2 = dstNode2.isRetn;
		assert (isRetn1 == isRetn2);
        boolean changed = false;
		boolean isKill1 = dstNode1.isKill;
		boolean isKill2 = dstNode2.isKill;
		if (!isKill1 && isKill2) {
			isKill1 = true;
           	changed = true;
        }
		// merge env's
        Obj[] env1 = dstNode1.env;
        Obj[] env2 = dstNode2.env;
        int n = env1.length;
        assert (n == env2.length);
        Obj[] env3 = null;
        for (int i = 0; i < n; i++) {
			Obj pts1 = env1[i];
            Obj pts2 = env2[i];
            if (pts1 == pts2 || pts1 == Obj.BOTH || pts2 == Obj.EMTY) {
				if (env3 != null)
					env3[i] = pts1;
				continue;
			}
			// if reached here then need to allocate env3 if it hasn't
			// already been allocated
			if (pts1 == Obj.EMTY)
				pts1 = pts2;
			else {
				// at this point there are only two cases:
				// pts1=LOC and pts2=ESC|BOTH
				// pts1=ESC and pts2=LOC|BOTH
				pts1 = Obj.BOTH;
			}
			if (env3 == null) {
				env3 = new Obj[n];
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
        ArraySet<FldObj> heap1 = dstNode1.heap;
        ArraySet<FldObj> heap2 = dstNode2.heap;
        if (!heap1.equals(heap2)) {
			heap1 = mergeHeaps(heap1, heap2);
            changed = true;
        }
		if (changed) {
			this.dstNode = new DstNode(env1, heap1, isKill1, isRetn1);
			return true;
		}
        return false;
	}
	private static ArraySet<FldObj> mergeHeaps(ArraySet<FldObj> heap1,
			ArraySet<FldObj> heap2) {
		int n1 = heap1.size();
		int n2 = heap2.size();
		ArraySet<FldObj> heap3 = new ArraySet<FldObj>();
		for (int i = 0; i < n1; i++) {
			FldObj fo1 = heap1.get(i);
			jq_Field f = fo1.f;
			boolean found = false;
			for (int j = 0; j < n2; j++) {
				FldObj fo2 = heap2.get(j);
				if (fo2.f == f) {
					boolean isLoc1 = fo1.isLoc;
					boolean isEsc1 = fo1.isEsc;
					boolean isLoc2 = fo2.isLoc;
					boolean isEsc2 = fo2.isEsc;
					if (isLoc1 == isLoc2 && isEsc1 == isEsc2)
						heap3.add(fo1);
					else if (isLoc1 && isEsc1)
						heap3.add(fo1);
					else if (isLoc2 && isEsc2)
						heap3.add(fo2);
					else
						heap3.add(new FldObj(f, true, true));
					found = true;
					break;
				}
			}
			if (!found)
				heap3.add(fo1);
		}
		for (int j = 0; j < n2; j++) {
			FldObj fo2 = heap2.get(j);
			jq_Field f = fo2.f;
			boolean found = false;
			for (int i = 0; i < n1; i++) {
				FldObj fo1 = heap1.get(i);
				if (fo1.f == f) {
					found = true;
					break;
				}
			}
			if (!found)
				heap3.add(fo2);
		}
		return heap3;
	}

	public int hashCode() {
        int i = 5381;
        i = ((i << 5) + i) + srcNode.hashCode();
        i = ((i << 5) + i) + dstNode.hashCode();
		return i;
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
