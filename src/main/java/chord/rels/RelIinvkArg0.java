/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.doms.DomI;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (i,v) such that local variable v
 * is the 0th argument variable of method invocation statement i,
 * if i has >= 0 arguments and if the target method of i is not a
 * synthesized method.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "IinvkArg0",
	sign = "I0,V1:I0_V1"
)
public class RelIinvkArg0 extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomV domV = (DomV) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			ParamListOperand l = Invoke.getParamList(q);
			if (l.length() > 0) {
				RegisterOperand vo = l.get(0);
				Register v = vo.getRegister();
				if (v.getType().isReferenceType()) {
					int vIdx = domV.indexOf(v);
					add(iIdx, vIdx);
				}
			}
		}
	}
}
