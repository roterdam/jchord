/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import chord.doms.DomM;
import chord.project.Project;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Call graph analysis producing a thread-oblivious, abbreviated,
 * context-sensitive call graph of the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "throbl-abbr-cicg-java",
	consumedNames = { "thrOblAbbrIM", "thrOblAbbrMM",
		"thrOblAbbrRootM", "thrOblAbbrReachableM" }
)
public class ThrOblAbbrCICGAnalysis extends CICGAnalysis {
	public void run() {
		domM = (DomM) Project.getTrgt("M");
		relIM = (ProgramRel) Project.getTrgt("thrOblAbbrIM");
		relMM = (ProgramRel) Project.getTrgt("thrOblAbbrMM");
		relRootM = (ProgramRel) Project.getTrgt("thrOblAbbrRootM");
		relReachableM = (ProgramRel)
			Project.getTrgt("thrOblAbbrReachableM");
	}
}
