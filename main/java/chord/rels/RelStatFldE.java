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
 * a static field (as opposed to an instance field or array element).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "statFldE",
	sign = "E0"
)
public class RelStatFldE extends ProgramRel {
	public void fill() {
		DomE domE = (DomE) doms[0];
		int numE = domE.size();
		for (int eIdx = 0; eIdx < numE; eIdx++) {
			Quad e = domE.get(eIdx);
			Operator op = e.getOperator();
			if (op instanceof Getstatic || op instanceof Putstatic) {
				add(eIdx);
			}
		}
	}
}
