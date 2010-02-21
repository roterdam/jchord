/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Util.Templates.ListIterator;
import chord.doms.DomP;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p1,p2) such that program point p2
 * is an immediate successor of program point p1.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PP",
	sign = "P0,P1:P0xP1"
)
public class RelPP extends ProgramRel implements IMethodVisitor {
	private DomP domP;
	public void init() {
		domP = (DomP) doms[0];
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
				it.hasNext();) {
			BasicBlock bq = it.nextBasicBlock();
			int n = bq.size();
			Inst y = (n == 0) ? bq : bq.getQuad(0);
			int yIdx = domP.indexOf(y);
			if (n != 0) {
				int pIdx = yIdx;
				for (int i = 1; i < n; i++) {
					Quad q = bq.getQuad(i);
					int qIdx = domP.indexOf(q);
					add(pIdx, qIdx);
					pIdx = qIdx;
				}
			}
			for (Object bo : bq.getPredecessors()) {
				BasicBlock bp = (BasicBlock) bo;
				int l = bp.size();
				Inst x = (l == 0) ? bp : bp.getQuad(l - 1);
				int xIdx = domP.indexOf(x);
				add(xIdx, yIdx);
			}
		}
	}
}
