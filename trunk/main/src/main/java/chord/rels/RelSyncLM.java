/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomL;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (l,m) such that monitorenter
 * statement l is synchronized on method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "syncLM",
	sign = "L0,M0:L0_M0"
)
public class RelSyncLM extends ProgramRel {
	public void fill() {
		DomL domL = (DomL) doms[0];
		DomM domM = (DomM) doms[1];
		int numL = domL.size();
		for (int lIdx = 0; lIdx < numL; lIdx++) {
			Inst i = domL.get(lIdx);
			if (!(i instanceof Quad)) {
				jq_Method m = Program.v().getMethod(i);
				int mIdx = domM.indexOf(m);
				add(lIdx, mIdx);
			}
		}
	}
}
