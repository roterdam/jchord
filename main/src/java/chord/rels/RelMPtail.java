/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,p) such that statement p is
 * the unique exit basic block of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MPtail",
	sign = "M0,P0:M0xP0"
)
public class RelMPtail extends ProgramRel
		implements IMethodVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		BasicBlock bx = cfg.exit();
		add(m, bx);
	}
}
