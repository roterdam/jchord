/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Inst;
import chord.doms.DomL;
import chord.doms.DomP;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (p,e) such that the statement
 * at program point p is a heap access statement e that
 * accesses (reads or writes) an instance field, a static field,
 * or an array element.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PL",
	sign = "P0,L0:P0xL0"
)
public class RelPL extends ProgramRel {
	public void fill() {
		DomP domP = (DomP) doms[0];
		DomL domL = (DomL) doms[1];
		int numL = domL.size();
		for (int lIdx = 0; lIdx < numL; lIdx++) {
			Inst i = domL.get(lIdx);
			int pIdx = domP.get(i);
			add(pIdx, lIdx);
		}
	}
}
