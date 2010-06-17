/*
 * Copyright (c) 2008-2010, Intel Corporation.
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

import chord.program.Program;
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
	consumedNames = { "M" }
)
public class DomR extends ProgramDom<Inst> implements IRelLockInstVisitor {
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
			BasicBlock tail = cfg.exit();
			getOrAdd(tail);
		}
	}
	public void visitRelLockInst(Quad q) {
		getOrAdd(q);
	}
	public int getOrAdd(Inst i) {
		assert (ctnrMethod != null);
		Program.getProgram().mapInstToMethod(i, ctnrMethod);
		return super.getOrAdd(i);
	}
	public String toUniqueString(Inst o) {
		return Program.getProgram().toBytePosStr(o);
	}
	public String toXMLAttrsString(Inst o) {
		jq_Method m = Program.getProgram().getMethod(o);
		String file = Program.getSourceFileName(m.getDeclaringClass());
		int line = Program.getLineNumber(o, m);
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"";
	}
}
