/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.monosite;

import chord.project.Chord;
import chord.project.Project;
import chord.project.JavaAnalysis;
import chord.project.ProgramRel;
import chord.analyses.alias.CtxtsAnalysis;

/**
 * Static monomorphic call site analysis.
 * <p>
 * Outputs relations <tt>monoSite</tt> and <tt>polySite</tt>
 * containing dynamically dispatching method invocation statements
 * (of kind <tt>INVK_VIRTUAL</tt> or <tt>INVK_INTERFACE</tt>)
 * that, as deemed by this analysis, have either at most a single
 * target method or possibly multiple target methods,
 * respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 * <li>All system properties recognized by abstract contexts analysis
 * (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="monosite-java"
)
public class MonositeAnalysis extends JavaAnalysis {
	public void run() {
		Project.runTask(CtxtsAnalysis.getCspaKind());
		Project.runTask("monosite-dlog");
	}
}
