/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.visitors;

import joeq.Class.jq_Field;

/**
 * Visitor over all fields of all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IFieldVisitor extends IClassVisitor {
	/**
	 * Visits all fields of all classes in the program.
	 * 
	 * @param	f	A field.
	 */
	public void visit(jq_Field f);
}
