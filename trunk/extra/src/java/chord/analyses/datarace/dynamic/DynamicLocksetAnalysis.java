/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.bddbddb.Rel.IntPairIterable;
import chord.doms.DomE;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
@Chord(	name="dynamic-lockset-java",
		consumes = { "aliasEE" },
		produces = { "noLckSyncEE" },
		namesOfSigns = { "noLckSyncEE" },
		signs = { "E0,E1:E0xE1" })
public class DynamicLocksetAnalysis extends DynamicAnalysis {
	
	private InstrScheme instrScheme;
	private TIntObjectHashMap<TIntHashSet> t2lcks;
	private TIntObjectHashMap<TIntHashSet> e2lckset;
	private Set<IntPair> noLckSync;
	private ProgramRel relAlias;
	private ProgramRel relNoLckSync;
	
	@Override
	public void initAllPasses() {
		t2lcks = new TIntObjectHashMap<TIntHashSet>();
		e2lckset = new TIntObjectHashMap<TIntHashSet>();
		relAlias = (ProgramRel) Project.getTrgt("aliasEE");
		relNoLckSync = (ProgramRel) Project.getTrgt("noLckSyncEE");
		noLckSync = new HashSet<IntPair>();
	}
	
	@Override
	public void initPass() {
		t2lcks.clear();
		e2lckset.clear();
		noLckSync.clear();
	}
	
	@Override
	public void doneAllPasses() {
		relNoLckSync.zero();
		for (IntPair pair : noLckSync) {
			relNoLckSync.add(pair.idx0, pair.idx1);
		}
		relNoLckSync.save();
		
		DomE domE = (DomE) Project.getTrgt("E");
		PrintWriter writer =
			 OutDirUtils.newPrintWriter("dynamic_noLckSync.txt");
		for (IntPair pair : noLckSync) {
			Quad q1 = (Quad) domE.get(pair.idx0);
			String s1 = q1.toVerboseStr();
			Quad q2 = (Quad) domE.get(pair.idx1);
			String s2 = q2.toVerboseStr();
			writer.println(s1);
			writer.println("\t" + s2);
		}
		writer.close();
	}
	
	@Override
	public void donePass() {
		relAlias.load();
		IntPairIterable racePairs = relAlias.getAry2IntTuples();
		L: for (IntPair tmpPair : racePairs) {
			final IntPair pair = tmpPair; 
			final TIntHashSet L1 = e2lckset.get(pair.idx0);
			final TIntHashSet L2 = e2lckset.get(pair.idx1);
			noLckSync.add(pair);
			if (L1 == null || L2 == null) {
				continue L;
			}
			L1.forEach(new TIntProcedure() {
//				@Override
				public boolean execute(int arg0) {
					if (L2.contains(arg0)) {
						noLckSync.remove(pair);
						return false;
					} else {
						return true;
					}
				}
			});
		}
	}
	
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setAcquireLockEvent(true, true, true);
		instrScheme.setReleaseLockEvent(true, true, true);
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
	public void processAcquireLock(int l, int t, int o) {
		if (l >= 0 && t >= 0 && o != 0) {
			TIntHashSet L = t2lcks.get(t);
			if (L == null) t2lcks.put(t, L = new TIntHashSet());
			L.add(o);
		}
	}
	
	@Override
	public void processReleaseLock(int r, int t, int o) {
		if (r >= 0 && t >= 0 && o != 0) {
			TIntHashSet L = t2lcks.get(t);
			if (!L.contains(o)) {
				Messages.log("WARN: [DynamicLocksetAnalysis.processReleaseLock] Cannot find lock to remove: t=" + t + ",o=" + o);
			} else {
				L.remove(o);
			}
		}
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updateLockset(e, t);
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updateLockset(e, t);
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		updateLockset(e, t);
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updateLockset(e, t);
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updateLockset(e, t);
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updateLockset(e, t);
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updateLockset(e, t);
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		updateLockset(e, t);
	}

	private void updateLockset(int e, int t) {
		if (e >= 0 && t >= 0) {
			TIntHashSet L = t2lcks.get(t);
			if (L == null) return;
			TIntHashSet tmpLckset = e2lckset.get(e);
			if (tmpLckset == null) e2lckset.put(e, tmpLckset = new TIntHashSet());
			final TIntHashSet lckset = tmpLckset;
			L.forEach(new TIntProcedure() {
//				@Override
				public boolean execute(int arg0) {
					lckset.add(arg0);
					return true;
				}
			});
		}
	}
}
