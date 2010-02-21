/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each interface type.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "interfaceT",
	sign = "T0"
)
public class RelInterfaceT extends ProgramRel
		implements IClassVisitor {
	public void visit(jq_Class c) {
		if (c.isInterface())
			add(c);
	}
}
