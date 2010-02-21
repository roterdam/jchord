/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IVarVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (v,t) such that local variable v
 * of reference type has type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "VT",
	sign = "V0,T0:T0_V0"
)
public class RelVT extends ProgramRel implements IVarVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visit(Register v) {
		jq_Type t = v.getType();
		if (t.isReferenceType()) {
			try {
				add(v, t);  // TODO: is t accurate???
			} catch (RuntimeException ex) {
				// TODO
			}
		}
	}
}
