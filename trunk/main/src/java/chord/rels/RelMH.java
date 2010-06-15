/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomH;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,h) such that method m contains
 * object allocation statement h.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MH",
	sign = "M0,H0:M0_H0"
)
public class RelMH extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomH domH = (DomH) doms[1];
		int numH = domH.size();
		int numA = domH.getLastRealHidx();
		Program program = Program.getProgram();
		for (int hIdx = 1; hIdx < numA; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			jq_Method m = program.getMethod(q);
			int mIdx = domM.indexOf(m);
			add(mIdx, hIdx);
		}
	}
}
