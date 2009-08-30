/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.project.Chord;
import chord.project.Program;
import chord.visitors.INewInstVisitor;

/**
 * Domain of object allocation statements.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "H",
	consumedNames = { "M" }
)
public class DomH extends QuadDom implements INewInstVisitor {
	public void init() {
		super.init();
		getOrAdd(null);
	}
	public void visitNewInst(Quad q) {
		getOrAdd(q);
	}
	public String toXMLAttrsString(Quad q) {
		// XXX
		if (q == null)
			return "";
		TypeOperand to = (q.getOperator() instanceof New) ?
			New.getType(q) : NewArray.getType(q);
		String type = to.getType().getName();
		return super.toXMLAttrsString(q) + " type=\"" +
			type + "\"";
	}
}
