/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import chord.program.visitors.IFieldVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

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
