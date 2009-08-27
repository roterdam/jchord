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
		IndexSet<jq_Method> roots = Program.v().getRootMethods();
		jq_Method main = Program.v().getMainMethod();
		assert (roots != null ^ main != null);
		if (roots != null) {
			for (jq_Method m : roots)
				add(m);
		} else
			add(main);
	}
}
