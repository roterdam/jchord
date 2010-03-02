/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.io.PrintWriter;

import joeq.Compiler.Quad.Quad;
import chord.doms.DomE;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * Dynamic alias analysis using object alloc site-based abstraction.
 * 
 * This analysis runs in support of race detection and abstracts concrete
 * objects by their alloc site.
 * Thus, for statements
 * 		u.f = y; 
 * and
 * 		z = v.f;
 * pts(u) and pts(v) are computed (in the form of alloc sites),
 * which enables the detection of a potential race. 
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dynamic-alias-alloc-java",
    consumedNames = { "startingRacePair" },
    producedNames = { "aliasingRacePair", "bannedE" },
    namesOfSigns = { "aliasingRacePair", "bannedE" },
    signs = { "E0,E1:E0xE1", "E0:E0" }
)
public class DynamicAliasAnalysisUsingAlloc extends AbstractDynamicAliasAnalysis {
    private InstrScheme instrScheme;
    private final TIntIntHashMap obj2hIdx = new TIntIntHashMap();
    private final TIntObjectHashMap<TIntHashSet> eIdx2pts =
		new TIntObjectHashMap<TIntHashSet>();
	private final TIntHashSet banned = new TIntHashSet();
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
		obj2hIdx.clear();
		eIdx2pts.clear();
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0 && h >= 0)
			obj2hIdx.put(o, h);
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
			if (obj2hIdx.containsKey(b)) {
				int h = obj2hIdx.get(b);
				pts.add(h);
			} else {
				banned.add(e);
			}
		}
	}
	
	public void doneAllPasses() {
		ProgramRel relAliasingRacePair =
			(ProgramRel) Project.getTrgt("aliasingRacePair");
		relAliasingRacePair.zero();
		for (IntPair p : aliasingRacePairSet)
			relAliasingRacePair.add(p.idx0, p.idx1);
		relAliasingRacePair.save();

        DomE domE = instrumentor.getDomE();
		Program program = Program.v();
		PrintWriter writer =
			OutDirUtils.newPrintWriter("aliasing_race_pair-dynalloc.txt");
		for (IntPair p : aliasingRacePairSet) {
			Quad q1 = (Quad) domE.get(p.idx0);
			Quad q2 = (Quad) domE.get(p.idx1);
			String s1 = program.toVerboseStr(q1);
			String s2 = program.toVerboseStr(q2);
			writer.println("e1=<" + s1 + ">, e2=<" + s2 + ">");
		}
		
		writer.close();
        ProgramRel relBannedE =
			(ProgramRel) Project.getTrgt("bannedE");
		relBannedE.zero();
		TIntIterator it = banned.iterator();
		while (it.hasNext())
			relBannedE.add(it.next());			
		relBannedE.save();
		writer = OutDirUtils.newPrintWriter("bannedStatements.txt");
		it = banned.iterator();
		while (it.hasNext()) {
			Quad q = (Quad) domE.get(it.next());
			String s = program.toVerboseStr(q);
			writer.println("e=<" + s + ">");
		}
		writer.close();
	}
}
