/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Monitor;

import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramDom;
import chord.project.Project;
import chord.visitors.ILockInstVisitor;

/**
 * Domain of all monitorenter statements.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "L",
	consumedNames = { "M" }
)
public class DomL extends ProgramDom<Inst> implements ILockInstVisitor {
	protected DomM domM;
	protected jq_Method ctnrMethod;
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
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
	public void visitLockInst(Quad q) {
		Program.v().mapInstToMethod(q, ctnrMethod);
		getOrAdd(q);
	}
	public String toXMLAttrsString(Inst i) {
		jq_Method m = Program.v().getMethod(i);
		String fileName = Program.getSourceFileName(m.getDeclaringClass());
		int lineNumber = Program.getLineNumber(i, m);
		return "file=\"" + fileName + "\" " + "line=\"" + lineNumber + "\" " +
			"Mid=\"M" + domM.indexOf(m) + "\"";
	}
	public String toString(Inst q) {
		return Program.v().toStringLockInst(q);
	}
}
