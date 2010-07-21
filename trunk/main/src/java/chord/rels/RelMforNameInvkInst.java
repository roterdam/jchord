/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import java.util.Set;

import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;

import chord.doms.DomM;
import chord.doms.DomI;
import chord.doms.DomT;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Relation containing all calls to java.lang.Class.newInstance() that
 * were resolved by the program scope construction algorithm and
 * therefore do not need to be resolved by looking at a subsequent
 * cast statement by call graph construction algorithms.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MforNameInvkInst",
	sign = "M0,I0,T0:M0xI0_T0"
)
public class RelMforNameInvkInst extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomI domI = (DomI) doms[1];
		DomT domT = (DomT) doms[2];
		Program program = Program.getProgram();
		Set<Pair<Quad, Set<jq_Reference>>> resolvedForNameSites =
			program.getReflectInfo().getResolvedForNameSites();
		for (Pair<Quad, Set<jq_Reference>> p : resolvedForNameSites) {
			Quad q = p.val0;
			int iIdx = domI.indexOf(q);
			if (iIdx != -1) {
				jq_Method m = q.getMethod();
				int mIdx = domM.indexOf(m);
				assert (mIdx >= 0);
				for (jq_Reference t : p.val1) {
					int tIdx = domT.indexOf(t);
					assert (tIdx >= 0);
					add(mIdx, iIdx, tIdx);
				}
			}
		}
	}
}
