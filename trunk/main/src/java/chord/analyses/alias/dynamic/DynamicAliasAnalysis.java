/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.io.PrintWriter;

import joeq.Compiler.Quad.Quad;
import chord.bddbddb.Rel.IntPairIterable;
import chord.doms.DomE;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * Concrete dynamic alias analysis.
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dynamic-alias-java",
    producedNames = { "normalizedAliasingRacePair" },
    namesOfSigns = { "normalizedAliasingRacePair" },
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
	
	public void doneAllPasses() {
        ProgramRel relRawAliasingRacePair =
			(ProgramRel) Project.getTrgt("rawAliasingRacePair");
		relRawAliasingRacePair.zero();
		for (IntPair p : aliasingRacePairSet)
			relRawAliasingRacePair.add(p.idx0, p.idx1);
		relRawAliasingRacePair.save();
		Project.runTask("nonalloc-filter");
        DomE domE = instrumentor.getDomE();
		Program program = Program.v();
		ProgramRel relNormalizedAliasingRacePair =
			(ProgramRel) Project.getTrgt("normalizedAliasingRacePair");
		relNormalizedAliasingRacePair.load();
		PrintWriter writer =
			OutDirUtils.newPrintWriter("aliasing_race_pair-dyn.txt");
		IntPairIterable ary2IntTuples = relNormalizedAliasingRacePair.getAry2IntTuples();
		for (IntPair p : ary2IntTuples) {
			Quad q1 = (Quad) domE.get(p.idx0);
			Quad q2 = (Quad) domE.get(p.idx1);
			String s1 = program.toVerboseStr(q1);
			String s2 = program.toVerboseStr(q2);
			writer.println("e1=<" + s1 + ">, e2=<" + s2 + ">");
		}
		writer.close();
	}
}
