/*
 * Copyright (c) 2008-2010, Intel Corporation.
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
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitInvokeInst(Quad q) {
		add(q);
	}
	public String toUniqueString(Quad q) {
		return q.toByteLocStr();
	}
	public String toXMLAttrsString(Quad q) {
		Operator op = q.getOperator();
		jq_Method m = q.getMethod();
		String file = m.getDeclaringClass().getSourceFileName();
		int line = q.getLineNumber();
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"" +
			" rdwr=\"" + (op.isWrHeapInst() ? "Wr" : "Rd") + "\"";
	}
}
