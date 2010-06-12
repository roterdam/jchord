/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import chord.program.Program;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramDom;

/**
 * Domain of method invocation statements.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "I",
	consumedNames = { "M" }
)
public class DomI extends ProgramDom<Quad> implements IInvokeInstVisitor {
	protected DomM domM;
	protected jq_Method ctnrMethod;
	@Override
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	@Override
	public void visit(jq_Class c) { }
	@Override
	public void visit(jq_Method m) {
		if (!m.isAbstract())
			ctnrMethod = m;
	}
	@Override
	public void visitInvokeInst(Quad q) {
		getOrAdd(q);
	}
	@Override
	public int getOrAdd(Quad q) {
		assert (ctnrMethod != null);
		Program.v().mapInstToMethod(q, ctnrMethod);
		return super.getOrAdd(q);
	}
	@Override
	public String toUniqueString(Quad q) {
		return Program.v().toBytePosStr(q);
	}
	@Override
	public String toXMLAttrsString(Quad q) {
		Operator op = q.getOperator();
		jq_Method m = Program.v().getMethod(q);
		String file = Program.getSourceFileName(m.getDeclaringClass());
		int line = Program.getLineNumber(q, m);
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"" +
			" rdwr=\"" + (Program.isWrHeapInst(op) ? "Wr" : "Rd") + "\"";
	}
}
