/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-sensitive, abbreviated,
 * context-sensitive call graph of the program.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thrsen-abbr-cscg-java",
	consumedNames = { "thrSenAbbrCICM", "thrSenAbbrCMCM",
		"thrSenAbbrRootCM", "thrSenAbbrReachableCM" }
)
public class ThrSenAbbrCSCGAnalysis extends CSCGAnalysis {
	public void run() {
		domM = (DomM) Project.getTrgt("M");
		relCICM = (ProgramRel) Project.getTrgt("thrSenAbbrCICM");
		relCMCM = (ProgramRel) Project.getTrgt("thrSenAbbrCMCM");
		relRootCM = (ProgramRel) Project.getTrgt("thrSenAbbrRootCM");
		relReachableCM = (ProgramRel)
			Project.getTrgt("thrSenAbbrReachableCM");
	}
}
