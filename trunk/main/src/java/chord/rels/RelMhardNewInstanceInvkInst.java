/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import java.util.Set;
import java.util.HashSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;

import chord.doms.DomM;
import chord.doms.DomI;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.ChordProperties;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MhardNewInstanceInvkInst",
    sign = "M0,I0:M0xI0"
)
public class RelMhardNewInstanceInvkInst extends ProgramRel
		implements IInvokeInstVisitor {
	private DomM domM;
	private DomI domI;
	private jq_Method ctnrMethod;
	private Set<Quad> resolvedNewInstSites;
	public void init() {
		domM = (DomM) doms[0];
		domI = (DomI) doms[1];
        resolvedNewInstSites = new HashSet<Quad>();
		for (Pair<Quad, Set<jq_Reference>> p : Program.getProgram().
				getReflectInfo().getResolvedNewInstSites()) {
			resolvedNewInstSites.add(p.val0);
		}
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitInvokeInst(Quad q) {
		if (!ChordProperties.handleNewInstReflection ||
				resolvedNewInstSites.contains(q))
			return;
		jq_Method meth = Invoke.getMethod(q).getMethod();
		if (meth.getName().toString().equals("newInstance") &&
			meth.getDesc().toString().equals("()Ljava/lang/Object;") &&
			meth.getDeclaringClass().getName().equals("java.lang.Class")) {
			int mIdx = domM.indexOf(ctnrMethod);
			assert (mIdx >= 0);
			int iIdx = domI.indexOf(q);
			assert (iIdx >= 0);
			add(mIdx, iIdx);
		}
	}
}
