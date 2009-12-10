/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Putstatic;
import chord.doms.DomE;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each statement that accesses (reads or writes)
 * an instance field (as opposed to a static field or array element).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "instFldE",
	sign = "E0"
)
public class RelInstFldE extends ProgramRel {
	public void fill() {
		DomE domE = (DomE) doms[0];
		int numE = domE.size();
		for (int eIdx = 0; eIdx < numE; eIdx++) {
			Quad e = (Quad) domE.get(eIdx);
			Operator op = e.getOperator();
			if (!(op instanceof Getstatic) && !(op instanceof Putstatic))
				add(eIdx);
		}
	}
}
