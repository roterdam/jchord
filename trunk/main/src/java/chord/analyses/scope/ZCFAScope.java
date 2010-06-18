/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Method;

import chord.util.IndexSet;

import chord.program.Program;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * Context-insensitive may alias analysis-based scope construction.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "0cfa-scope-java",
	consumedNames = { "reachableM" }
)
public class ZCFAScope extends JavaAnalysis {
	public void run() {
		ProgramRel relReachableM = (ProgramRel) Project.getTrgt("reachableM");
		IndexSet<jq_Method> methods = new IndexSet<jq_Method>();
		relReachableM.load();
		final Iterable<jq_Method> tuples = relReachableM.getAry1ValTuples();
		for (jq_Method m : tuples)
			methods.add(m);
		relReachableM.close();
		Program.write(methods);
	}
}
