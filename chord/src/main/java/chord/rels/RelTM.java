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
 * Relation containing each tuple (t,m) such that method m is
 * declared in type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "TM",
	sign = "T0,M0:M0_T0"
)
public class RelTM extends ProgramRel implements IMethodVisitor {
	private jq_Class ctnrClass;
	public void visit(jq_Class c) {
		ctnrClass = c;
	}
	public void visit(jq_Method m) {
		add(ctnrClass, m);
	}
}
