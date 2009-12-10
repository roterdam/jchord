/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Field;
import joeq.Class.jq_Class;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IFieldVisitor;

/**
 * Relation containing each tuple (t,f) such that f is a
 * static field defined in type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "staticTF",
	sign = "T0,F0:F0_T0"
)
public class RelStaticTF extends ProgramRel
		implements IFieldVisitor {
	private jq_Class ctnrClass;
	public void visit(jq_Class c) {
		ctnrClass = c;
	}
	public void visit(jq_Field f) {
		if (f.isStatic()) {
			add(ctnrClass, f);
        }
	}
}
