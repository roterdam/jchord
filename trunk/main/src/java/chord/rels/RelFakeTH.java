/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import chord.doms.DomH;
import chord.doms.DomT;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;
import joeq.Class.jq_Class;

/**
 * Relation containing each tuple (t,h) such that the fake allocation site of type t is h.
 *
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 */
@Chord(
	name = "fakeTH",
	sign = "T0,H0:T0_H0"
)
public class RelFakeTH extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		DomH domH = (DomH) doms[1];
		IndexSet<jq_Class> reflectAllocTypes = domH.getReflectAllocTypes();
		if (reflectAllocTypes == null)
			return;
		int hIdx = domH.getLastRealHidx() + 1;
		for (jq_Class c : reflectAllocTypes) {
			int tIdx = domT.indexOf(c);
			assert (tIdx != -1);
			add(tIdx, hIdx);
			hIdx++;
		}
	}
}
