/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;
import chord.visitors.IMethodVisitor;

/**
 * Relation containing each tuple (m,s) such that method m has
 * signature s.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MS",
	sign = "M0,S0:S0_M0"
)
public class RelMS extends ProgramRel implements IMethodVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		String s = Program.getSign(m);
		add(m, s);
	}
}