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
 * Relation containing each tuple (s,t) such that class/interface type s
 * extends class/interface type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "extnsTT",
	sign = "T1,T0:T0_T1"
)
public class RelExtnsTT extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		for (jq_Class c : Program.getPreparedClasses()) {
			// c is concrete/abstract class or interface
			int t1Idx = domT.get(c);
			if (c.isInterface()) {
				for (jq_Class d : c.getInterfaces()) {
					int t2Idx = domT.get(d);
					add(t1Idx, t2Idx);
				}
			} else {
				jq_Class d = c.getSuperclass();
				if (d == null) {
					assert (c.getName().equals("java.lang.Object"));
				} else {
					int t2Idx = domT.get(d);
					if (t2Idx != -1)
						add(t1Idx, t2Idx);
				}
			}
		}
	}
}
