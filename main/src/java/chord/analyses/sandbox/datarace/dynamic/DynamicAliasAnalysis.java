/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.sandbox.datarace.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.util.HashSet;
import java.util.Set;

import chord.bddbddb.Rel.IntPairIterable;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
@Chord(	name="dynamic-alias-java",
		consumedNames = { "startingRaces", "filteredStartingRaces" },
		producedNames = { "aliasEE" }, 
		namesOfSigns = { "aliasEE" },
		signs = { "E0,E1:E0xE1" })
public class DynamicAliasAnalysis extends DynamicAnalysis {

	private final static boolean useThresc = Boolean.parseBoolean(System.getProperty("chord.datarace.wthThresc", "false"));
	
	private Set<IntPair> aliases;
	private InstrScheme instrScheme;
	private TIntObjectHashMap<TIntHashSet> pts;
	private ProgramRel relAlias;
	private ProgramRel relInitial;
	
	@Override
	public void initAllPasses() {
		aliases = new HashSet<IntPair>();
		pts = new TIntObjectHashMap<TIntHashSet>();
		relAlias = (ProgramRel) Project.getTrgt("aliasEE");
		relInitial = (ProgramRel) Project.getTrgt(useThresc ? "filteredStartingRaces" : "startingRaces");
	}
	
	@Override
	public void initPass() {
		pts.clear();
		aliases.clear();
	}
	
	@Override
	public void donePass() {
		relInitial.load();
		IntPairIterable racePairs = relInitial.getAry2IntTuples();
		L: for (IntPair tmpPair : racePairs) {
			final IntPair pair = tmpPair; 
			final TIntHashSet A = pts.get(pair.idx0);
			final TIntHashSet B = pts.get(pair.idx1);
			if (A == null || B == null) {
				continue L;
			}
			A.forEach(new TIntProcedure() {
//				@Override
				public boolean execute(int arg0) {
					if (B.contains(arg0)) {
						aliases.add(pair);
						return false;
					} else {
						return true;
					}
				}
			});
		}
	}
	
	@Override
	public void doneAllPasses() {
		relAlias.zero();
		for (IntPair pair : aliases) {
			relAlias.add(pair.idx0, pair.idx1);
		}
		relAlias.save();
	}
		
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setAloadPrimitiveEvent(true, true, true, false);
		instrScheme.setAloadReferenceEvent(true, true, true, true, false);
		instrScheme.setAstorePrimitiveEvent(true, true, true, false);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, false);
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, false);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, false);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, false);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, false);
		return instrScheme;
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updatePts(e, b);
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updatePts(e, b);
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		updatePts(e, b);
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updatePts(e, b);
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updatePts(e, b);
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updatePts(e, b);
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updatePts(e, b);
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		updatePts(e, b);
	}

	private void updatePts(int e, int b) {
		if (e < 0 || b == 0) return;
		TIntHashSet S = pts.get(e);
		if (S == null) pts.put(e, S = new TIntHashSet(1));
		S.add(b);
	}
}
