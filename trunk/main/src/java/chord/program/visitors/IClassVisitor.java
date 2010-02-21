/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program.visitors;

import joeq.Class.jq_Class;

/**
 * Visitor over all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IClassVisitor {
	/**
	 * Visits all classes in the program.
	 *
	 * @param	c	A class.
	 */
	public void visit(jq_Class c);
}
