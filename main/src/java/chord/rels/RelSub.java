/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Type;

import chord.doms.DomT;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (s,t) such that type s is a
 * subtype of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "sub",
	sign = "T1,T0:T0_T1"
)
public class RelSub extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		int numT = domT.size();
		for (int t1Idx = 0; t1Idx < numT; t1Idx++) {
			jq_Type t1 = domT.get(t1Idx);
			if (!t1.isPrepared())
				continue;
			for (int t2Idx = 0; t2Idx < numT; t2Idx++) {
				jq_Type t2 = domT.get(t2Idx);
				if (!t2.isPrepared())
					continue;
				if (t1.isSubtypeOf(t2)) {
					add(t1Idx, t2Idx);
				}
			}
		}
	}
}
