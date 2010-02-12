/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomE;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing all statements that write to an instance
 * field, static field, or array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "writeE",
	sign = "E0"
)
public class RelWriteE extends ProgramRel {
	public void fill() {
		DomE domE = (DomE) doms[0];
		int numE = domE.size();
		for (int eIdx = 0; eIdx < numE; eIdx++) {
			Quad e = (Quad) domE.get(eIdx);
			Operator op = e.getOperator();
			if (Program.isWrHeapInst(op))
				add(eIdx);
		}
	}
}
