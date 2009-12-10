/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IFieldVisitor;

/**
 * Relation containing all static (as opposed to instance) fields.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "statF",
	sign = "F0"
)
public class RelStatF extends ProgramRel implements IFieldVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Field f) {
		if (f.isStatic())
			add(f);
	}
}
