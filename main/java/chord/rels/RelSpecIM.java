/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (i,m) such that m is the resolved
 * method of method invocation statement i of kind <tt>INVK_SPECIAL</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "specIM",
	sign = "I0,M0:I0xM0"
)
public class RelSpecIM extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomM domM = (DomM) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad i = domI.get(iIdx);
			Operator op = i.getOperator();
			if (op instanceof InvokeStatic) {
				jq_Method m = InvokeStatic.getMethod(i).getMethod();
				if (!m.isStatic()) {
					int mIdx = domM.get(m);
					if (mIdx != -1)
						add(iIdx, mIdx);
				}
			}
		}
	}
}
