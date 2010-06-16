/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Type;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.doms.DomH;
import chord.doms.DomT;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.project.Messages;

/**
 * Relation containing each tuple (h,t) such that object allocation
 * statement h allocates objects of non-array type t.
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
		int numA = domH.getLastRealHidx() + 1;
		int numH = domH.size();
		for (int hIdx = 1; hIdx < numA; hIdx++) {
			Quad h = (Quad) domH.get(hIdx);
			Operator op = h.getOperator();
			jq_Type t;
			// Note: do NOT merge handling of New and NewArray
			if (op instanceof New)
				t = New.getType(h).getType();
			else {
				assert (op instanceof NewArray);
				t = NewArray.getType(h).getType();
			}
			int tIdx = domT.indexOf(t);
			assert (tIdx >= 0);
			add(hIdx, tIdx);
		}
		for (int hIdx = numA; hIdx < numH; hIdx++) {
			jq_Reference c = (jq_Reference) domH.get(hIdx);
			int tIdx = domT.indexOf(c);
			assert (tIdx >= 0);
			add(hIdx, tIdx);
		}
	}
}
