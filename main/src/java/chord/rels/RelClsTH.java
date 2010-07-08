/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;

import chord.doms.DomT;
import chord.doms.DomH;
import chord.program.PhantomClsVal;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,h) such that h is the hypothetical
 * site at which class t is reflectively created.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "clsTH",
	sign = "T0,H0:H0_T0"
)
public class RelClsTH extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		DomH domH = (DomH) doms[1];
		System.out.println("SIZE: " + domH.size());
		for (jq_Reference r : Program.getProgram().getClasses()) {
			int tIdx = domT.indexOf(r);
			assert (tIdx >= 0);
			int hIdx = domH.indexOf(new PhantomClsVal(r));
			assert (hIdx >= 0);
			add(tIdx, hIdx);
		}
	}
}
