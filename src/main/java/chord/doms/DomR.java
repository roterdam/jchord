/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;

import chord.program.Program;
import chord.project.Chord;
import chord.visitors.IRelLockInstVisitor;

/**
 * Domain of all lock release points, including monitorexit
 * statements and (unique) exit basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "R",
	consumedNames = { "M" }
)
public class DomR extends QuadDom implements IRelLockInstVisitor {
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ctnrMethod = m;
		if (m.isSynchronized()) {
			ControlFlowGraph cfg = m.getCFG();
			BasicBlock tail = cfg.exit();
			Program.v().mapInstToMethod(tail, m);
			getOrAdd(tail);
		}
	}
	public void visitRelLockInst(Quad q) {
		getOrAdd(q);
	}
}
