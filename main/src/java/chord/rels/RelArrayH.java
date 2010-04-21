/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.doms.DomH;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each array object allocation statement h.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "arrayH",
	sign = "H0:H0"
)
public class RelArrayH extends ProgramRel {
	public void fill() {
		DomH domH = (DomH) doms[0];
		int numH = domH.size();
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			if (q.getOperator() instanceof NewArray)
				add(q);
		}
	}
}
