/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Util.Templates.ListIterator;

import chord.project.Program;
import chord.project.ProgramDom;
import chord.project.Project;
import chord.project.Chord;

/**
 * Domain of simple statements.
 * <p>
 * The 0th element in this domain is the statement at the unique
 * entry point of the main method of the program.
 * <p>
 * The statements of each method in the program are assigned
 * contiguous indices in this domain, with the statements at the
 * unique entry and exit points of each method being assigned
 * the smallest and largest indices, respectively, of all
 * indices assigned to statements in that method.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "P",
	consumedNames = { "M" }
)
public class DomP extends ProgramDom<Inst> {
	public void fill() {
		DomM domM = (DomM) Project.getTrgt("M");
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
					it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				int n = bb.size();
				if (n == 0) {
					Program.v().mapInstToMethod(bb, m);
					getOrAdd(bb);
					continue;
				}
				for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
					Quad q = it2.nextQuad();
					Program.v().mapInstToMethod(q, m);
					getOrAdd(q);
				}
			}
		}
	}
	public String toString(Inst q) {
		return q instanceof Quad ? ((Quad) q).toString_short() : q.toString();
	}
}
