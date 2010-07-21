/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import chord.doms.DomM;
import chord.project.Project;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-oblivious, abbreviated,
 * context-sensitive call graph of the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "throbl-abbr-cscg-java",
	consumedNames = { "thrOblAbbrCICM", "thrOblAbbrCMCM",
		"thrOblAbbrRootCM", "thrOblAbbrReachableCM" }
)
public class ThrOblAbbrCSCGAnalysis extends CSCGAnalysis {
	public void run() {
		domM = (DomM) Project.getTrgt("M");
		relCICM = (ProgramRel) Project.getTrgt("thrOblAbbrCICM");
		relCMCM = (ProgramRel) Project.getTrgt("thrOblAbbrCMCM");
		relRootCM = (ProgramRel) Project.getTrgt("thrOblAbbrRootCM");
		relReachableCM = (ProgramRel)
			Project.getTrgt("thrOblAbbrReachableCM");
	}
}
