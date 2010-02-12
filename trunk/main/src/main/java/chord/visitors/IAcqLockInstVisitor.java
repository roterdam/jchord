/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.visitors;

import joeq.Compiler.Quad.Quad;

/**
 * Visitor over all monitorenter statements in all methods
 * in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IAcqLockInstVisitor extends IMethodVisitor {
	/**
	 * Visits all monitorenter statements in all methods
	 * in the program.
	 * 
	 * @param	q	A monitorenter statement.
	 */
	public void visitAcqLockInst(Quad q);
}
