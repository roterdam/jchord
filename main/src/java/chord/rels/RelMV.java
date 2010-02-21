/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IVarVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v) such that method m
 * declares local variable v, that is, v is either an
 * argument or temporary variable of m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MV",
	sign = "M0,V0:M0_V0"
)
public class RelMV extends ProgramRel implements IVarVisitor {
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visit(Register v) {
		if (v.getType().isReferenceType())
			add(ctnrMethod, v);
	}
}
