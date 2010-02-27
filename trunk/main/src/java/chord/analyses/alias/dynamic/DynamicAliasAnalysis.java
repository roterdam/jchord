/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias.dynamic;

import gnu.trove.TIntProcedure;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import chord.instr.InstrScheme;
import chord.project.Chord;

/**
 * Concrete dynamic alias analysis.
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dynamic-alias-java",
    consumedNames = { "startingRacePair" },
    producedNames = { "aliasingRacePair" },
    namesOfSigns = { "aliasingRacePair" },
    signs = { "E0,E1:E0xE1" }
)
public class DynamicAliasAnalysis extends AbstractDynamicAliasAnalysis {
	private InstrScheme instrScheme;
	// map from statements in domain E to points-to set of concrete objects
    private TIntObjectHashMap<TIntHashSet> eIdx2pts =
		new TIntObjectHashMap<TIntHashSet>();
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setGetfieldReferenceEvent(true, false, true, false, false);
    	instrScheme.setGetfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setPutfieldReferenceEvent(true, false, true, false, false);
    	instrScheme.setPutfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setAloadReferenceEvent(true, false, true, false, false);
    	instrScheme.setAloadPrimitiveEvent(true, false, true, false);
    	instrScheme.setAstoreReferenceEvent(true, false, true, false, false);
    	instrScheme.setAstorePrimitiveEvent(true, false, true, false);
    	return instrScheme;
    }

	public boolean aliases(int e1, int e2) {
		final TIntHashSet pts1 = eIdx2pts.get(e1);
		if (pts1 == null)
			return false;
		final TIntHashSet pts2 = eIdx2pts.get(e2);
		if (pts2 == null)
			return false;
		final TIntProcedure proc = new TIntProcedure() {
			public boolean execute(int value) {
				if (pts2.contains(value))
					return false;
				return true;
			}
		};
		if (!pts1.forEach(proc))
			return true;
		return false;
	}

	public void donePass() {
		super.donePass();
		eIdx2pts.clear();
	}

	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		updatePointsTo(e, b);
	}
	
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updatePointsTo(e, b);
	}

	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updatePointsTo(e, b);
	}
	
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updatePointsTo(e, b);
	}
	
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updatePointsTo(e, b);
	}

	public void processAstorePrimitive(int e, int t, int b, int i) {
		updatePointsTo(e, b);
	}
	
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updatePointsTo(e, b);
	}
	
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updatePointsTo(e, b);
	}
	
	private void updatePointsTo(int e, int b) {
		if (e >= 0 && b != 0) {
			TIntHashSet pts = eIdx2pts.get(e);
			if (pts == null) {
				pts = new TIntHashSet();
				eIdx2pts.put(e, pts);
			}
			pts.add(b);
		}
	}
}
