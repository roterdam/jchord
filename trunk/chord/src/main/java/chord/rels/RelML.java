/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import chord.doms.DomL;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (m,l) such that method m contains
 * synchronized statement l.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "ML",
	sign = "M0,L0:M0_L0"
)
public class RelML extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomL domL = (DomL) doms[1];
		int numL = domL.size();
		for (int lIdx = 0; lIdx < numL; lIdx++) {
			Inst i = domL.get(lIdx);
			jq_Method m = Program.v().getMethod(i);
			int mIdx = domM.indexOf(m);
			add(mIdx, lIdx);
		}
	}
}
