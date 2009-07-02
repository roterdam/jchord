/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import chord.project.Chord;
import chord.project.Program;
import chord.visitors.IHeapInstVisitor;

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
public class DomE extends QuadDom implements IHeapInstVisitor {
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
		set(q);
	}
	public String toXMLAttrsString(Quad q) {
		Operator op = q.getOperator();
		return super.toXMLAttrsString(q) +
			" rdwr=\"" + (Program.isWrHeapInst(op) ? "Wr" : "Rd") + "\"";
	}
	public String toString(Quad q) {
		return Program.toStringHeapInst(q);
	}
}
