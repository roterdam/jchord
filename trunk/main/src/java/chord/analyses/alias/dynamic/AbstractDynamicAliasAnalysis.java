/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias.dynamic;

import java.util.HashSet;
import java.util.Set;

import chord.bddbddb.Rel.IntPairIterable;
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * Abstract dynamic alias analysis.
 * 
 * This analysis runs in support of race detection.
 * Thus, for statements
 * 		u.f = y; 
 * and
 * 		z = v.f;
 * pts(u) and pts(v) are computed by subclasses of this analysis
 * which enables the detection of a potential race. 
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class AbstractDynamicAliasAnalysis extends DynamicAnalysis {
	protected Set<IntPair> startingRacePairSet = new HashSet<IntPair>();
	protected Set<IntPair> aliasingRacePairSet = new HashSet<IntPair>();

	public abstract boolean aliases(int e1, int e2);

	public void initAllPasses() {
        ProgramRel relStartingRacePair =
			(ProgramRel) Project.getTrgt("startingRacePair");
		relStartingRacePair.load();
		IntPairIterable tuples = relStartingRacePair.getAry2IntTuples();
		for (IntPair p : tuples) 
			startingRacePairSet.add(p);
		relStartingRacePair.close();
	}

	public void donePass() {
		for (IntPair p : startingRacePairSet) {
			if (!aliasingRacePairSet.contains(p)) {
				int e1 = p.idx0;
				int e2 = p.idx1;
				if (aliases(e1, e2))
					aliasingRacePairSet.add(p);
			}
		}
	}
}
