/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;

import chord.doms.DomT;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (s,t) such that class type s
 * implements interface type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "implsTT",
	sign = "T1,T0:T0_T1"
)
public class RelImplsTT extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		for (jq_Class c : Program.getPreparedClasses()) {
			if (!c.isInterface()) {
				int t1Idx = domT.get(c);
				for (jq_Class d : c.getInterfaces()) {
					int t2Idx = domT.get(d);
					if (t2Idx != -1)
						add(t1Idx, t2Idx);
				}
			}
		}
	}
}
