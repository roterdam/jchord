/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Quad;
import chord.project.Chord;
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
		getOrAdd(q);
	}
}
