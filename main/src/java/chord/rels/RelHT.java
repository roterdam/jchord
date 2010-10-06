/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import java.util.List;

import joeq.Class.jq_Type;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import chord.program.Reflect;
import chord.doms.DomH;
import chord.doms.DomT;
import chord.program.Program;
import chord.program.PhantomObjVal;
import chord.program.PhantomClsVal;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.project.Messages;
import chord.util.tuple.object.Pair;

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
	private DomH domH;
	private DomT domT;
	public void fill() {
		domH = (DomH) doms[0];
		domT = (DomT) doms[1];
		int numH = domH.size();
		int mark1 = domH.getLastRealIdx() + 1;
		// int mark2 = domH.getLastPhantomObjIdx() + 1;
		for (int hIdx = 1; hIdx < mark1; hIdx++) {
			Quad h = (Quad) domH.get(hIdx);
			Operator op = h.getOperator();
			jq_Type t;
			// do NOT merge handling of New and NewArray
			if (op instanceof New)
				t = New.getType(h).getType();
			else if (op instanceof NewArray) {
				t = NewArray.getType(h).getType();
			} else {
				assert (op instanceof MultiNewArray);
				t = MultiNewArray.getType(h).getType();
			}
			int tIdx = domT.indexOf(t);
			assert (tIdx >= 0);
			add(hIdx, tIdx);
		}
		Reflect reflect = Program.g().getReflect();
		processResolvedNewInstSites(reflect.getResolvedObjNewInstSites());
		processResolvedNewInstSites(reflect.getResolvedConNewInstSites());
		processResolvedNewInstSites(reflect.getResolvedAryNewInstSites());
/*
		for (int hIdx = mark1; hIdx < mark2; hIdx++) {
			PhantomObjVal h = (PhantomObjVal) domH.get(hIdx);
			jq_Reference r = h.r;
			int tIdx = domT.indexOf(r);
			assert (tIdx >= 0);
			add(hIdx, tIdx);
		}
		jq_Reference cls = Program.g().getClass("java.lang.Class");
		if (cls != null) {
			int tIdx = domT.indexOf(cls);
			assert (tIdx >= 0);
			for (int hIdx = mark2; hIdx < numH; hIdx++)
				add(hIdx, tIdx);
		}
*/
	}
	private void processResolvedNewInstSites(List<Pair<Quad, List<jq_Reference>>> l) {
        for (Pair<Quad, List<jq_Reference>> p : l) {
			Quad q = p.val0;
			int hIdx = domH.indexOf(q);
			for (jq_Reference t : p.val1) {
				int tIdx = domT.indexOf(t);
            	add(hIdx, tIdx);
			}
		}
	}
}
