/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.doms.DomH;
import chord.doms.DomT;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (h,t) such that object allocation
 * statement h allocates objects of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "HT",
	sign = "H0,T1:T1_H0"
)
public class RelHT extends ProgramRel {
	public void fill() {
		DomH domH = (DomH) doms[0];
		DomT domT = (DomT) doms[1];
		int numH = domH.size();
		for (int hIdx = 0; hIdx < numH; hIdx++) {
			Quad h = (Quad) domH.get(hIdx);
			Operator op = h.getOperator();
			jq_Type t;
			if (op instanceof New)
				t = New.getType(h).getType();
			else
				t = NewArray.getType(h).getType();
			int tIdx = domT.indexOf(t);
			if (tIdx == -1) {
				System.out.println("WARNING: HT: " + h);
				continue;
			}
			add(hIdx, tIdx);
		}
	}
}
