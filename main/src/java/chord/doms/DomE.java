/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import chord.program.Program;
import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramDom;

/**
 * Domain of statements that access (read or write) an
 * instance field, a static field, or an array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "E",
	consumedNames = { "M" }
)
public class DomE extends ProgramDom<Quad> implements IHeapInstVisitor {
	protected DomM domM;
	protected jq_Method ctnrMethod;
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (!m.isAbstract())
			ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Getfield) {
			if (!(Getfield.getBase(q) instanceof RegisterOperand))
				return;
		}
		if (op instanceof Putfield) {
			if (!(Putfield.getBase(q) instanceof RegisterOperand))
				return;
		}
		getOrAdd(q);
	}
	public int getOrAdd(Quad q) {
		assert (ctnrMethod != null);
		Program.getProgram().mapInstToMethod(q, ctnrMethod);
		return super.getOrAdd(q);
	}
	public String toUniqueString(Quad q) {
		return Program.getProgram().toBytePosStr(q);
	}
	public String toXMLAttrsString(Quad q) {
		Operator op = q.getOperator();
		jq_Method m = Program.getProgram().getMethod(q);
		String file = Program.getSourceFileName(m.getDeclaringClass());
		int line = Program.getLineNumber(q, m);
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"" +
			" rdwr=\"" + (Program.isWrHeapInst(op) ? "Wr" : "Rd") + "\"";
	}
}
