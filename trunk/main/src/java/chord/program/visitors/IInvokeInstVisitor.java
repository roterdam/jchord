/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program.visitors;

import joeq.Compiler.Quad.Quad;

/**
 * Visitor over all method invocation statements in all methods
 * in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IInvokeInstVisitor extends IMethodVisitor {
	/**
	 * Visits all method invocation statements in all methods
	 * in the program.
	 * 
	 * @param	q	A method call statement.
	 */
	public void visitInvokeInst(Quad q);
}
