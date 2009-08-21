/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import gnu.trove.TIntArrayList;

import java.util.HashSet;
import java.util.Set;


import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.Monitor.MONITORENTER;
import chord.doms.DomI;
import chord.doms.DomL;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IMethodVisitor;

/**
 * Relation containing each tuple (e,l) such that statement e
 * that accesses (reads or writes) an instance field, a
 * static field, or an array element is lexically enclosed in
 * a synchronized block that acquires a lock at monitorenter
 * statement l.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "LI",
	sign = "L0,I0:L0_I0"
)
public class RelLI extends ProgramRel implements IMethodVisitor {
	private Set<BasicBlock> visited = new HashSet<BasicBlock>();
	private DomI domI;
	private DomL domL;
	public void init() {
		domL = (DomL) doms[0];
		domI = (DomI) doms[1];
	}
	public void visit(jq_Class c) {	}
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		BasicBlock entry = cfg.entry();
		TIntArrayList locks = new TIntArrayList();
		if (m.isSynchronized()) {
			int lIdx = domL.indexOf(entry);
			locks.add(lIdx);
		}
		process(entry, locks);
		visited.clear();
	}
	private void process(BasicBlock bb, TIntArrayList locks) {
		int n = bb.size();
		int k = locks.size();
		for (int i = 0; i < n; i++) {
			Quad q = bb.getQuad(i);
			Operator op = q.getOperator();
			if (op instanceof Monitor) {
				if (op instanceof MONITORENTER) {
					TIntArrayList locks2 = new TIntArrayList(k + 1);
					for (int j = 0; j < k; j++)
						locks2.add(locks.get(j));
					int lIdx = domL.indexOf(q);
					locks2.add(lIdx);
					locks = locks2;
					k++;
				} else {
					k--;
					TIntArrayList locks2 = new TIntArrayList(k);
					for (int j = 0; j < k; j++)
						locks2.add(locks.get(j));
					locks = locks2;
				}
			} else if (op instanceof Invoke && k > 0) {
				int iIdx = domI.indexOf(q);
				add(locks.get(k - 1), iIdx);
			}
		}
		for (Object o : bb.getSuccessors()) {
			BasicBlock bb2 = (BasicBlock) o;
			if (!visited.contains(bb2)) {
				visited.add(bb2);
				process(bb2, locks);
			}
		}
	}
}
