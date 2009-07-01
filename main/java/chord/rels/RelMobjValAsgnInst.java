/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomH;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (m,v,h) such that method m contains
 * object allocation statement h which assigns to local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MobjValAsgnInst",
	sign = "M0,V0,H0:M0_V0_H0"
)
public class RelMobjValAsgnInst extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomV domV = (DomV) doms[1];
		DomH domH = (DomH) doms[2];
		int numH = domH.size();
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad q = domH.get(hIdx);
			jq_Method m = Program.getMethod(q);
			int mIdx = domM.get(m);
			Operator op = q.getOperator();
			RegisterOperand vo;
			if (op instanceof New)
				vo = New.getDest(q);
			else
				vo = NewArray.getDest(q);
			Register v = vo.getRegister();
			int vIdx = domV.get(v);
			add(mIdx, vIdx, hIdx);
		}
	}
}
