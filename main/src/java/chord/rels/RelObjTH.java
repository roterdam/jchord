/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;

import chord.doms.DomT;
import chord.doms.DomH;
import chord.program.Program;
import chord.program.PhantomObjVal;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,h) such that h is the hypothetical
 * site at which objects of class t are reflectively allocated.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "objTH",
	sign = "T0,H0:H0_T0"
)
public class RelObjTH extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		DomH domH = (DomH) doms[1];
		for (jq_Reference r : Program.g().getReflectInfo().getReflectClasses()) {
			int tIdx = domT.indexOf(r);
			assert (tIdx >= 0);
			int hIdx = domH.indexOf(new PhantomObjVal(r));
			assert (hIdx >= 0);
			add(tIdx, hIdx);
		}
	}
}
