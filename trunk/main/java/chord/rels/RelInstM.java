/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IMethodVisitor;

/**
 * Relation containing all instance (as opposed to static) methods.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "instM",
	sign = "M0"
)
public class RelInstM extends ProgramRel implements IMethodVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (!m.isStatic())
			add(m);
	}
}
