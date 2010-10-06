/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;

import chord.doms.DomI;
import chord.doms.DomH;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Relation containing each tuple (m,i,t) such that method m contains
 * call site i calling instance method java.lang.Class.newInstance()
 * with its this argument possibly evaluating to type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "aryNewInstIH",
	sign = "I0,H0:I0_H0"
)
public class RelAryNewInstIH extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomH domH = (DomH) doms[1];
        List<Pair<Quad, List<jq_Reference>>> l =
            Program.g().getReflect().getResolvedAryNewInstSites();
		for (Pair<Quad, List<jq_Reference>> p : l) {
			Quad q = p.val0;
			int iIdx = domI.indexOf(q);
			assert (iIdx >= 0);
			int hIdx = domH.indexOf(q);
			assert (hIdx >= 0);
			add(iIdx, hIdx);
		}
	}
}

