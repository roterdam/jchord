/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

import chord.program.visitors.IRelLockInstVisitor;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramDom;

/**
 * Domain of all lock release points, including monitorexit
 * statements and exit basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "R",
	consumes = { "M" }
)
public class DomR extends ProgramDom<Inst> implements IRelLockInstVisitor {
	protected DomM domM;
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		if (m.isSynchronized()) {
			ControlFlowGraph cfg = m.getCFG();
			EntryOrExitBasicBlock tail = cfg.exit();
			add(tail);
		}
	}
	public void visitRelLockInst(Quad q) {
		add(q);
	}
	public String toUniqueString(Inst o) {
		return o.toByteLocStr();
	}
	public String toXMLAttrsString(Inst o) {
		jq_Method m = o.getMethod();
		String file = m.getDeclaringClass().getSourceFileName();
		int line = o.getLineNumber();
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"";
	}
}
