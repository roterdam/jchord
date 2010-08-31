/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;

import chord.doms.DomM;
import chord.doms.DomI;
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
	name = "MeasyNewInstanceInvkInst",
	sign = "M0,I0:M0xI0"
)
public class RelMeasyNewInstanceInvkInst extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomI domI = (DomI) doms[1];
		Program program = Program.g();
		Set<Pair<Quad, Set<jq_Reference>>> resolvedNewInstSites =
			program.getReflectInfo().getResolvedNewInstSites();
		for (Pair<Quad, Set<jq_Reference>> p : resolvedNewInstSites) {
			Quad q = p.val0;
			jq_Method m = q.getMethod();
			int mIdx = domM.indexOf(m);
			assert (mIdx >= 0);
			int iIdx = domI.indexOf(q);
			assert (iIdx >= 0);
			add(mIdx, iIdx);
		}
	}
}
