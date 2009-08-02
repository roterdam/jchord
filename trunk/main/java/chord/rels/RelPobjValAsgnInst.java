/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomH;
import chord.doms.DomP;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (p,v,h) such that the statement
 * at program point p is an object allocation statement h which
 * assigns to local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PobjValAsgnInst",
	sign = "P0,V0,H0:P0_V0_H0"
)
public class RelPobjValAsgnInst extends ProgramRel {
	public void fill() {
		DomP domP = (DomP) doms[0];
		DomV domV = (DomV) doms[1];
		DomH domH = (DomH) doms[2];
		int numH = domH.size();
		for (int hIdx = 0; hIdx < numH; hIdx++) {
			Quad h = domH.get(hIdx);
			int pIdx = domP.indexOf(h);
			Operator op = h.getOperator();
			RegisterOperand vo;
			if (op instanceof New)
				vo = New.getDest(h);
			else
				vo = NewArray.getDest(h);
			Register v = vo.getRegister();
			int vIdx = domV.indexOf(v);
			add(pIdx, vIdx, hIdx);
		}
	}
}
