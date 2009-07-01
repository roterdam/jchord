/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (e,f) such that statement e
 * accesses (reads or writes) instance field, static field, or
 * array element f.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "EF",
	sign = "E0,F0:F0_E0"
)
public class RelEF extends ProgramRel {
	public void fill() {
		DomE domE = (DomE) doms[0];
		DomF domF = (DomF) doms[1];
		int numE = domE.size();
		for (int eIdx = 0; eIdx < numE; eIdx++) {
			Quad e = domE.get(eIdx);
			jq_Field f = Program.getField(e);
			int fIdx = domF.get(f);
			if (fIdx != -1)
				add(eIdx, fIdx);
		}
	}
}
