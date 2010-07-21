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
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * Context-sensitive call graph analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "cscg-java",
	consumedNames = { "CICM", "CMCM", "rootCM", "reachableM" }
)
public class CSCGAnalysis extends JavaAnalysis {
    protected DomM domM;
    protected ProgramRel relCICM;
	protected ProgramRel relCMCM;
	protected ProgramRel relRootCM;
    protected ProgramRel relReachableCM;
    protected CSCG callGraph;
    public void run() {
    	domM = (DomM) Project.getTrgt("M");
    	relRootCM = (ProgramRel) Project.getTrgt("rootCM");
    	relReachableCM = (ProgramRel) Project.getTrgt("reachableCM");
    	relCICM = (ProgramRel) Project.getTrgt("CICM");
    	relCMCM = (ProgramRel) Project.getTrgt("CMCM");
    }
    /**
     * Provides the program's context-sensitive call graph.
     * 
     * @return	The program's context-sensitive call graph.
     */
    public ICSCG getCallGraph() {
    	if (callGraph == null) {
    		callGraph = new CSCG(domM, relRootCM, relReachableCM,
				relCICM, relCMCM);
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
   
