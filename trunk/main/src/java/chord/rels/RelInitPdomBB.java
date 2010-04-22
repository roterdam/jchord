/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Class.jq_Class;
import joeq.Compiler.Quad.Dominators;
import joeq.Compiler.Quad.Dominators.DominatorNode;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.program.visitors.IMethodVisitor;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each pair of basic blocks (b1,b2) in each method
 * such that b1 is immediate postdominator of b2.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "initPdomBB",
	sign = "B0,B1:B0xB1"
)
public class RelInitPdomBB extends ProgramRel implements IMethodVisitor {
	private Dominators doms = new Dominators();
    public void visit(jq_Class c) { }
    public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		System.out.println("VISITING: " + m);
		ControlFlowGraph cfg = m.getCFG();
		doms.visitCFG(cfg);
		DominatorNode exit = doms.computeTree();
		process(exit);
	}
	private void process(DominatorNode n) {
		BasicBlock bb = n.bb;
		System.out.println("PROCESSING: " + bb);
		for (Object o : n.children) {
			DominatorNode n2 = (DominatorNode) o;
			BasicBlock bb2 = n2.bb;
			System.out.println("ADDING: " + bb + " -> " + bb2);
			add(bb, bb2);
			process(n2);
		}
	}
}
