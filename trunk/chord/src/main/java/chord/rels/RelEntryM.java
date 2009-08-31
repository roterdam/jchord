/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.project.Program;
import chord.util.IndexSet;

/**
 * Relation containing all entry methods (typically only the main method)
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "entryM",
	sign = "M0"
)
public class RelEntryM extends ProgramRel {
	public void fill() {
		jq_Method mainMethod = Program.v().getMainMethod();
		IndexSet<jq_Method> rootMethods = Program.v().getRootMethods();
		assert (mainMethod != null ^ rootMethods != null);
		if (mainMethod != null)
			add(mainMethod);
		else {
			for (jq_Method m : rootMethods)
				add(m);
		}
	}
}
