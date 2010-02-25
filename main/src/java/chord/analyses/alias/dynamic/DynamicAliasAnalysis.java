/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;

/**
 * Dynamic alias analysis.
 * 
 * This analysis runs in support of race detection, and abstracts concrete objects by their allocation site.
 * Thus, for statements
 * 		u.f = y; 
 * and
 * 		z = v.f;
 * pts(u) and pts(v) are computed (in the form of allocation sites), which enables the detection of a 
 * potential race. 
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 */
@Chord(
	name = "dynamic-alias-java"
)
public class DynamicAliasAnalysis extends DynamicAnalysis {

    private InstrScheme instrScheme;
    private TIntIntHashMap object2allocationSite;
    private TIntObjectHashMap<TIntHashSet> pts;

    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setNewAndNewArrayEvent(true, false, true);

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

	public void initAllPasses() {
		object2allocationSite = new TIntIntHashMap();
		pts = new TIntObjectHashMap<TIntHashSet>();
	}

	public void initPass() {
		object2allocationSite.clear();
		pts.clear();
	}

	public void donePass() {
//		System.out.println("***** DONE PASS *****");
	}

	public void doneAllPasses() {
//		System.out.println("***** DONE ALL PASSES *****");
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0 && h >= 0) {
			// Associate the allocated object with an allocation site.
			object2allocationSite.put(o, h);
		}
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
	
	public void processGetfieldPrimitive(int e, int t, int b, int f, int o) {
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
			TIntHashSet S = pts.get(e);
			if (S == null) {
				S = new TIntHashSet();
				pts.put(e, S);
			}
			if (object2allocationSite.containsKey(b)) {
				int allocationSite = object2allocationSite.get(b);
				S.add(allocationSite);
			}
		}
	}
}