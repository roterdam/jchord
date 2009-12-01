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
import chord.visitors.IAcqLockInstVisitor;

/**
 * Domain of all lock acquire points, including monitorenter
 * statements and entry basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "L",
	consumedNames = { "M" }
)
public class DomL extends QuadDom implements IAcqLockInstVisitor {
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ctnrMethod = m;
		if (m.isSynchronized()) {
			ControlFlowGraph cfg = m.getCFG();
			BasicBlock head = cfg.entry();
			Program.v().mapInstToMethod(head, m);
			getOrAdd(head);
		}
	}
	public void visitAcqLockInst(Quad q) {
		getOrAdd(q);
	}
}
