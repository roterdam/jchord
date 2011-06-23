/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace;

import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation denoting whether races involving the same abstract thread must be checked.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "excludeSameThread",
	sign = "K0:K0"
)
public class RelExcludeSameThread extends ProgramRel {
	public void fill() {
		if (System.getProperty("chord.datarace.exclude.eqth", "true").equals("true"))
			add(1);
	}
}
