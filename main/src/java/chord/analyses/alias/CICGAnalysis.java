/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * Context-insensitive call graph analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "cicg-java",
	consumedNames = { "IM", "MM", "rootM", "reachableM" }
)
public class CICGAnalysis extends JavaAnalysis {
	protected DomM domM;
	protected ProgramRel relRootM;
	protected ProgramRel relReachableM;
	protected ProgramRel relIM;
	protected ProgramRel relMM;
	protected CICG callGraph;
	public void run() {
		domM = (DomM) Project.getTrgt("M");
		relRootM = (ProgramRel) Project.getTrgt("rootM");
		relReachableM = (ProgramRel) Project.getTrgt("reachableM");
		relIM = (ProgramRel) Project.getTrgt("IM");
		relMM = (ProgramRel) Project.getTrgt("MM");
	}
	/**
	 * Provides the program's context-insensitive call graph.
	 * 
	 * @return	The program's context-insensitive call graph.
	 */
	public ICICG getCallGraph() {
		if (callGraph == null) {
			callGraph = new CICG(domM, relRootM, relReachableM,
				relIM, relMM);
		}
		return callGraph;
	}
	/**
	 * Frees relations used by this program analysis if they are in
	 * memory.
	 * <p>
	 * This method must be called after clients are done exercising
	 * the interface of this analysis.
	 */
	public void free() {
		if (callGraph != null)
			callGraph.free();
	}
}

