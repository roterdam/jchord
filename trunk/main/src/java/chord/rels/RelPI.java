/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Quad;
import chord.doms.DomI;
import chord.doms.DomP;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p,i) such that the statement
 * at program point p is method invocation statement i.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PI",
	sign = "P0,I0:I0xP0"
)
public class RelPI extends ProgramRel {
	public void fill() {
		DomP domP = (DomP) doms[0];
		DomI domI = (DomI) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad i = (Quad) domI.get(iIdx);
			int pIdx = domP.indexOf(i);
			add(pIdx, iIdx);
		}
	}
}
