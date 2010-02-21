/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.program.visitors.INewInstVisitor;
import chord.project.Chord;

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
	public void visitNewInst(Quad q) {
		getOrAdd(q);
	}
	public String toXMLAttrsString(Inst i) {
		Quad q = (Quad) i;
		TypeOperand to = (q.getOperator() instanceof New) ?
			New.getType(q) : NewArray.getType(q);
		String type = to.getType().getName();
		return super.toXMLAttrsString(q) + " type=\"" +
			type + "\"";
	}
}
