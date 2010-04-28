/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (i,m) such that m is the resolved
 * method of method invocation statement i of kind
 * <tt>INVK_VIRTUAL</tt> or <tt>INVK_INTERFACE</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "virtIM",
	sign = "I0,M0:I0xM0"
)
public class RelVirtIM extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomM domM = (DomM) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad i = (Quad) domI.get(iIdx);
			Operator op = i.getOperator();
			if (op instanceof InvokeVirtual || op instanceof InvokeInterface) {
				jq_Method m = Invoke.getMethod(i).getMethod();
				int mIdx = domM.indexOf(m);
				if (mIdx != -1)
					add(iIdx, mIdx);
			}
		}
	}
}
