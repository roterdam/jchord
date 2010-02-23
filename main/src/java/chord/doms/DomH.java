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
* <p>		
 * The 0th element of this domain (null) is a distinguished		
 * hypothetical object allocation statement that may be used		
 * for various purposes.
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
	public String toXMLAttrsString(Inst i) {
		if (i == null)
			return "";
		Quad q = (Quad) i;
		TypeOperand to = (q.getOperator() instanceof New) ?
			New.getType(q) : NewArray.getType(q);
		String type = to.getType().getName();
		return super.toXMLAttrsString(q) + " type=\"" +
			type + "\"";
	}
}
