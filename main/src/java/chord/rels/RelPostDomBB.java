/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import chord.util.ArraySet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

import joeq.Class.jq_Method;
import joeq.Class.jq_Class;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.program.visitors.IMethodVisitor;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Util.Templates.ListIterator;

/**
 * Relation containing each pair of basic blocks (b1,b2) in each method
 * such that b1 is immediate postdominator of b2.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "postDomBB",
	sign = "B0,B1:B0xB1"
)
public class RelPostDomBB extends ProgramRel implements IMethodVisitor {
	private final Map<BasicBlock, Set<BasicBlock>> pdomMap =
		new HashMap<BasicBlock, Set<BasicBlock>>();
    public void visit(jq_Class c) { }
    public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		// System.out.println("VISITING: " + m);
		pdomMap.clear();
		ControlFlowGraph cfg = m.getCFG();
		boolean changed;
		while (true) {
			changed = false;
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				Set<BasicBlock> oldPdom = pdomMap.get(bb);
				Set<BasicBlock> newPdom = null;
				List<BasicBlock> succs = bb.getSuccessors();
				int n = succs.size();
				if (n >= 1) {
					Set<BasicBlock> fstPdom = pdomMap.get(succs.get(0));
					if (fstPdom != null) {
						newPdom = new ArraySet<BasicBlock>(fstPdom);
						for (int i = 1; i < n; i++) {
							Set<BasicBlock> nxtPdom = pdomMap.get(succs.get(i));
							if (nxtPdom == null) {
								newPdom.clear();
								break;
							}
							newPdom.retainAll(nxtPdom);
						}
					}
				}
				if (newPdom == null)
					newPdom = new ArraySet<BasicBlock>(1);
				newPdom.add(bb);
				if (oldPdom == null || !oldPdom.equals(newPdom)) {
					changed = true;
					pdomMap.put(bb, newPdom);
				}
			}
			if (!changed)
				break;
		}
		for (BasicBlock bb : pdomMap.keySet()) {
			// System.out.print("postdominators of " + bb + ":");
			for (BasicBlock bb2 : pdomMap.get(bb)) {
				// System.out.print(" " + bb2);
				add(bb2, bb);
			}
			System.out.println();
		}
	}
}
