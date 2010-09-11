/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace.dynamic;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

import chord.bddbddb.Rel.IntPairIterable;
import chord.instr.InstrScheme;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomL;
import chord.doms.DomR;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.Config;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.SetUtils;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Dynamic datarace analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="dynamic-datarace-java",
	consumes = { "startingRacePairs" },
	produces = { "aliasingRacePairs", "parallelRacePairs", "escE" }
)
public class DynamicDataraceAnalysis extends DynamicAnalysis {
	private boolean verbose = true;

	// visitedAcc[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] visitedAcc;

	private IntArraySet activeThrObjs = new IntArraySet();

	private IntArraySet[] accToObjs;

	// map from each object to a list of each non-null instance field
	// of reference type along with its value
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;
	// set of IDs of currently escaping concrete/abstract objects
	private TIntHashSet escObjs;
	// escAcc[e] == true iff:
	// 1. kind is flowSen and instance field/array deref site having
	//	index e in domain E is flow-sen. thread-escaping
	// 2. kind is flowIns and instance field/array deref site having
	//	index e in domain E is flow-ins. thread-escaping
	private boolean[] escAcc;

	private TIntObjectHashMap<TIntArrayList> thrToLocksMap;
	private ArraySet/*<IntxIntSet>*/[] accToObjLocks;

	private ArraySet/*<IntxIntSet>*/[] accToMHP;

	private InstrScheme instrScheme;

	private AliasingCheckKind aliasingCheckKind;
	private EscapingCheckKind escapingCheckKind;
	private boolean checkMHP, checkLocks;

	private DomE domE;
	private DomM domM;
	private DomI domI;
	private DomL domL;
	private DomR domR;

	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null)
			return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMainMethodEvent(true, true);
        instrScheme.setNewAndNewArrayEvent(true, true, true);
		instrScheme.setThreadStartEvent(true, true, true);
		instrScheme.setThreadJoinEvent(true, true, true);
		instrScheme.setAcquireLockEvent(true, true, true);
		instrScheme.setReleaseLockEvent(true, true, true);
		instrScheme.setGetstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setGetstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setPutstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setPutstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setAloadPrimitiveEvent(true, true, true, true);
		instrScheme.setAloadReferenceEvent(true, true, true, true, true);
		instrScheme.setAstorePrimitiveEvent(true, true, true, true);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, true);
		return instrScheme;
	}

	@Override
	public void initAllPasses() { }

	@Override
	public void initPass() {
		aliasingCheckKind = AliasingCheckKind.CONCRETE;
		escapingCheckKind = EscapingCheckKind.WEAK_CONCRETE;
		checkMHP = true;
		checkLocks = false;

		domM = (DomM) ClassicProject.g().getTrgt("M");
		domE = (DomE) ClassicProject.g().getTrgt("E");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domL = (DomL) ClassicProject.g().getTrgt("L");
		domR = (DomR) ClassicProject.g().getTrgt("R");
		ClassicProject.g().runTask("M");
		ClassicProject.g().runTask("E");
		ClassicProject.g().runTask("I");
		ClassicProject.g().runTask("L");
		ClassicProject.g().runTask("R");
		int numE = domE.size();

		visitedAcc = new boolean[numE];
		for (int e = 0; e < numE; e++)
			visitedAcc[e] = false;

		accToObjs = new IntArraySet[numE];

		if (escapingCheckKind != EscapingCheckKind.NONE) {
			escObjs = new TIntHashSet();
			objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
			escAcc = new boolean[numE];
		}

		if (checkMHP) {
			accToMHP = new ArraySet[numE];
		}

		if (checkLocks) {
			thrToLocksMap = new TIntObjectHashMap<TIntArrayList>();
			accToObjLocks = new ArraySet[numE];
		}
	}

	@Override
	public void donePass() {
		System.out.println("***** STATS *****");
		int numVisitedE = 0;
		int numEscE = 0;
		ProgramRel relEscE = (ProgramRel) ClassicProject.g().getTrgt("escE");
		relEscE.zero();
		for (int e = 0; e < domE.size(); e++) {
			if (visitedAcc[e]) {
				if (escapingCheckKind == EscapingCheckKind.NONE || escAcc[e]) {
					relEscE.add(e);
				} 
			}
		}
		relEscE.save();

		ProgramRel relStartingRacePairs = (ProgramRel) ClassicProject.g().getTrgt("startingRacePairs");
		ProgramRel relAliasingRacePairs = (ProgramRel) ClassicProject.g().getTrgt("aliasingRacePairs");
		ProgramRel relParallelRacePairs = (ProgramRel) ClassicProject.g().getTrgt("parallelRacePairs");
		relStartingRacePairs.load();
		relAliasingRacePairs.zero();
		relParallelRacePairs.zero();
		IntPairIterable startingRacePairs = relStartingRacePairs.getAry2IntTuples();
		for (IntPair p : startingRacePairs) {
			int e1 = p.idx0;
			int e2 = p.idx1;
			if (visitedAcc[e1] && visitedAcc[e2]) {
				if (aliasingCheckKind == AliasingCheckKind.NONE) {
					relAliasingRacePairs.add(e1, e2);
				} else {
					IntArraySet objs1 = accToObjs[e1];
					IntArraySet objs2 = accToObjs[e2];
					if (objs1 != null && objs2 != null) {
						int n = objs1.size();
						for (int i = 0; i < n; i++) {
							int o = objs1.get(i);
							if (objs2.contains(o)) {
								relAliasingRacePairs.add(e1, e2);
								System.out.println("ALIASING: " + eStr(e1) + "," + eStr(e2)); 
								break;
							}
						}
					}
				}
				if (!checkMHP || mhp(e1, e2)) {
					System.out.println("PARALLEL: " + eStr(e1) + "," + eStr(e2)); 
					relParallelRacePairs.add(e1, e2);
					ArraySet<IntxIntSet> mhp1 = accToMHP[e1];
					for (IntxIntSet p : mhp1) System.out.println("e1=" + p);
					ArraySet<IntxIntSet> mhp2 = accToMHP[e2];
					for (IntxIntSet p : mhp2) System.out.println("e2=" + p);
				}
			}
       	}
		relAliasingRacePairs.save();
		relParallelRacePairs.save();

	}

	private boolean mhp(int e1, int e2) {
		ArraySet<IntxIntSet> mhp1 = accToMHP[e1];
		ArraySet<IntxIntSet> mhp2 = accToMHP[e2];
		if (mhp1 == null || mhp2 == null)
			return false;
		int n1 = mhp1.size();
		int n2 = mhp2.size();
		for (int i = 0; i < n1; i++) {
			IntxIntSet p1 = mhp1.get(i);
			int o1 = p1.o;
			for (int j = 0; j < n2; j++) {
				IntxIntSet p2 = mhp2.get(j);
				int o2 = p2.o;
				if (o1 != o2 && p1.s.contains(o2) && p2.s.contains(o1))
					return true;
			}
		}
		return false;
	}

	private String iStr(int i) { return (i < 0) ? "-1" : domI.get(i).toJavaLocStr(); }
	private String eStr(int e) { return (e < 0) ? "-1" : domE.get(e).toJavaLocStr(); }
	private String lStr(int l) { return (l < 0) ? "-1" : domL.get(l).toJavaLocStr(); }
	private String rStr(int r) { return (r < 0) ? "-1" : domR.get(r).toJavaLocStr(); }

	@Override
	public void doneAllPasses() { }

	@Override
	public void processEnterMainMethod(int m, int t) {
		if (verbose) System.out.println("MAIN: " + m + " " + t);
		printActiveThrObjs();
		activeThrObjs = new IntArraySet(activeThrObjs);
		activeThrObjs.add(t);
		printActiveThrObjs();
	}

	@Override
    public void processNewOrNewArray(int h, int t, int o) {
		if (verbose) System.out.println("NEW: " + h + " " + t + " " + o);
        if (o == 0)
            return;
		if (escapingCheckKind != EscapingCheckKind.NONE) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
		}
	}

	private void printActiveThrObjs() {
		String s = "";
		int n = activeThrObjs.size();
		for (int i = 0; i < n; i++) s += activeThrObjs.get(i) + ",";
		System.out.println(s);
	}

	@Override
	public void processThreadStart(int p, int t, int o) {
		if (verbose) System.out.println("START: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (checkMHP) {
			printActiveThrObjs();
			activeThrObjs = new IntArraySet(activeThrObjs);
			activeThrObjs.add(o);
			printActiveThrObjs();
		}
		if (escapingCheckKind != EscapingCheckKind.NONE)
			markAndPropEsc(o);
	}

	@Override
	public void processThreadJoin(int p, int t, int o) {
		if (verbose) System.out.println("JOIN: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (checkMHP) {
			printActiveThrObjs();
			int n = activeThrObjs.size();
			IntArraySet newThrObjs = new IntArraySet(n - 1);
			for (int i = 0; i < n; i++) {
				int t2 = activeThrObjs.get(i);
				if (t2 != o)
					newThrObjs.add(t2);
			}
			activeThrObjs = newThrObjs;
			printActiveThrObjs();
		}
	}

	@Override
	public void processAcquireLock(int p, int t, int l) {
		if (verbose) System.out.println("ACQLOCK: " + lStr(p) + " " + t + " " + l);
	}

	@Override
	public void processReleaseLock(int p, int t, int l) {
		if (verbose) System.out.println("RELLOCK: " + rStr(p) + " " + t + " " + l);
	}

	@Override
	public void processGetstaticPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("GETSTATIC: " + eStr(e) + " " + t + " " + b + " " + f);
	}

	@Override
	public void processGetstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETSTATIC: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
	}

	@Override
	public void processPutstaticPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " " + t + " " + b + " " + f);
	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		if (escapingCheckKind != EscapingCheckKind.NONE) {
	  		if (o != 0)
				markAndPropEsc(o);
		}
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " " + t + " " + b + " " + f);
		processHeapRd(e, t, b);
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapRd(e, t, b);
	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " " + t + " " + b + " " + f);
		processHeapRd(e, t, b);
	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapWr(e, t, b, f, o);
	}

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " " + t + " " + b + " " + i);
		processHeapRd(e, t, b);
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " " + t + " " + b + " " + i + " " + o);
		processHeapRd(e, t, b);
	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " " + t + " " + b + " " + i);
		processHeapRd(e, t, b);
	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " " + t + " " + b + " " + i + " " + o);
		processHeapWr(e, t, b, i, o);
	}

	private void processHeapRd(int e, int t, int b) {
		if (e < 0) return;
		visitedAcc[e] = true;
		if (b == 0) return;
		if (checkMHP) {
			ArraySet<IntxIntSet> mhp = accToMHP[e];
			boolean found = false;
			if (mhp == null) {
				mhp = new ArraySet<IntxIntSet>(1);
				accToMHP[e] = mhp;
			} else {
				for (IntxIntSet p : mhp) {
					if (p.o == t) {
 						if (activeThrObjs.subset(p.s)) {
							found = true;
							break;
						}
						if (p.s.subset(activeThrObjs)) {
							p.s = activeThrObjs;
							found = true;
							break;
						}
					}
				}
			}
			if (!found) {
				IntxIntSet tts = new IntxIntSet(t, activeThrObjs);
				mhp.add(tts);
			}
		}
		if (aliasingCheckKind == AliasingCheckKind.CONCRETE) {
			IntArraySet objs = accToObjs[e];
			if (objs == null) {
				objs = new IntArraySet();
				accToObjs[e] = objs;
				objs.add(b);
			}
		}
		if (escapingCheckKind != EscapingCheckKind.NONE) {
			if (escAcc[e])
				return;
			if (escObjs.contains(b))
				escAcc[e] = true;
		}
	}

	private void processHeapWr(int e, int t, int b, int fIdx, int r) {
		processHeapRd(e, t, b);
		if (escapingCheckKind != EscapingCheckKind.NONE) {
			if (e < 0 || b == 0 || fIdx < 0)
				return;
			List<FldObj> l = objToFldObjs.get(b);
			if (r == 0) {
				// this is a strong update; so remove field fIdx if it is there
				if (l != null) {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = l.get(i);
						if (fo.f == fIdx) {
							l.remove(i);
							break;
						}
					}
				}
				return;
			}
			boolean added = false;
			if (l == null) {
				l = new ArrayList<FldObj>();
				objToFldObjs.put(b, l);
			} else {
				for (FldObj fo : l) {
					if (fo.f == fIdx) {
						fo.o = r;
						added = true;
						break;
					}
				}
			}
			if (!added)
				l.add(new FldObj(fIdx, r));
			if (escObjs.contains(b))
				markAndPropEsc(r);
		}
	}

	private void markAndPropEsc(int o) {
		if (escObjs.add(o)) {
			List<FldObj> l = objToFldObjs.get(o);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
		}
	}
}

class IntxIntSet {
	public final int o;
	public IntArraySet s;
	public IntxIntSet(int o, IntArraySet s) {
		this.o = o;
		this.s = s;
	}
	public String toString() {
		String str = "";
		int n = s.size();
		for (int i = 0; i < n; i++)
			str += s.get(i) + ","; 
		return "o=" + o + ",s=" + str;
	}
}

