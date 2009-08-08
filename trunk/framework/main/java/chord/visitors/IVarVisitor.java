/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.visitors;

import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Visitor over all local variables of all methods in the program.
 * 
 * @author Mayur Naik (mayur.naik@intel.com)
 */
public interface IVarVisitor extends IMethodVisitor {
	/**
	 * Visits all local variables of all methods in the program.
	 * 
	 * @param	v	A local variable.
	 */
	public void visit(Register v);
}
