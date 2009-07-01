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
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;

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
		for (int hIdx = 0; hIdx < numH; hIdx++) {
			Quad q = domH.get(hIdx);
			jq_Method m = Program.getMethod(q);
			int mIdx = domM.get(m);
			add(mIdx, hIdx);
		}
	}
}
