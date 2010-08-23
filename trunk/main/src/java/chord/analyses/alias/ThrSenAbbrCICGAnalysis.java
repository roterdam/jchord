/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-sensitive, abbreviated,
 * context-insensitive call graph of the program.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thrsen-abbr-cicg-java",
	consumes = { "thrSenAbbrIM", "thrSenAbbrMM",
		"thrSenAbbrRootM", "thrSenAbbrReachableM" }
)
public class ThrSenAbbrCICGAnalysis extends CICGAnalysis {
	public void run() {
		domM = (DomM) Project.getTrgt("M");
		relIM = (ProgramRel) Project.getTrgt("thrSenAbbrIM");
		relMM = (ProgramRel) Project.getTrgt("thrSenAbbrMM");
		relRootM = (ProgramRel) Project.getTrgt("thrSenAbbrRootM");
		relReachableM = (ProgramRel)
			Project.getTrgt("thrSenAbbrReachableM");
	}
}
