/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.project.Chord;
import chord.project.Program;
import chord.visitors.IInvokeInstVisitor;

/**
 * Domain of method invocation statements.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "I",
	consumedNames = { "M" }
)
public class DomI extends QuadDom implements IInvokeInstVisitor {
	public void visitInvokeInst(Quad q) {
		set(q);
	}
	public String toString(Quad q) {
		return Program.toStringInvokeInst(q);
	}
}
