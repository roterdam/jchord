/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program.visitors;

import joeq.Compiler.Quad.Quad;

/**
 * Visitor over all phi statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IPhiInstVisitor extends IMethodVisitor {
	/**
	 * Visits all phi statements in all methods in the program.
	 * 
	 * @param	q	A copy assignment statement.
	 */
	public void visitPhiInst(Quad q);
}
