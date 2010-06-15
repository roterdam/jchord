/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;

/**
 * Relation containing each tuple (s,t) such that type s is a
 * subtype of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "sub",
	sign = "T1,T0:T0_T1"
)
public class RelSub extends ProgramRel {
	public void fill() {
		Program program = Program.getProgram();
		IndexSet<jq_Class> classes = program.getClasses();
		for (jq_Class t1 : classes) {
			for (jq_Class t2 : classes) {
				if (t1.isSubtypeOf(t2)) {
					add(t1, t2);
				}
			}
		}
	}
}
