/*
 * Copyright (c) 2008-2010, Intel Corporation.
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
	name = "reflectHT",
	sign = "H0,T0:T0_H0"
)
public class RelReflectHT extends ProgramRel {
	public void fill() {
		DomH domH = (DomH) doms[0];
		DomT domT = (DomT) doms[1];
		int numH = domH.size();
		int fstT = domH.getLastRealHidx() + 1;
		for (int hIdx = fstT; hIdx < numH; hIdx++) {
			jq_Class c = (jq_Class) domH.get(hIdx);
			int tIdx = domT.indexOf(c);
			assert (tIdx != -1);
			add(hIdx, tIdx);
		}
	}
}
