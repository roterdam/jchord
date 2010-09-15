/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace.dynamic;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import chord.bddbddb.Rel.IntPairIterable;
import chord.instr.InstrScheme;
import chord.doms.DomL;
import chord.doms.DomR;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.runtime.CoreEventHandler;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntPair;

/**
 * Dynamic datarace analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="dynamic-datarace-java",
	consumes = { "startingRacePairs" },
	produces = { "aliasingRacePairs", "parallelRacePairs", "guardedRacePairs",
		"combinedRacePairs", "escE" }
)
public class DynamicDataraceAnalysis extends DynamicAnalysis {
	private boolean verbose = true;
	private final IntArraySet emptySet = new IntArraySet(0);
	// accessedE == true iff heap-accessing statement with index e in
	// domain E is reached at least once during the execution
	private boolean[] accessedE;
	// Map from heap-accessing statement with index e in domain E to
	// info needed to determine if it is involved in a race
	private RaceInfo[] raceInfoE;

	// Map from each thread to its parent thread.
	// Entries are only added and never removed from this map.
	private TIntIntHashMap threadToParentMap;
	// Map from each thread t1 to the set containing each thread t2 that
	// is (directly) started by t1 but hasn't yet been joined.
	// A mapping from t1 to t2 is added to this map whenever t1 starts t2,
	// and the mapping from t1 to t2 is removed whenever t1 joins t2
	// (i.e., t1 waits for t2 to finish).
	// This map is used to populate MHP information: whenever thread t1
	// accesses heap-accessing statement with index e in domain E, each
	// thread t2 to which t1 is mapped in this map is added to the
	// appropriate element i in raceInfo[e][i].ts
	private TIntObjectHashMap<IntArraySet> threadToLiveChildrenMap;
	private TIntObjectHashMap<IntArraySet> threadToDescendantsMap;
	
	// Set of pairs of threads (t1,t2) such that t2 may happen in parallel
	// with all actions of t1.
	// This set is computed after the instrumented program terminates.
	// It uses threadToParentThreadMap:
	// (t1,t2) in halfParallelThreads if t2 in ancestors(t1)
	private Set<IntPair> halfParallelThreads;
	
	// Set of pairs of threads (t1,t2) such that t2 may happen in parallel
	// with all actions of t1, and vice versa.
	// The invariant t1 <= t2 holds for each pair of threads in this set.
	// This set is computed in two phases: partly during the execution of
	// the instrumented program and partly after it terminates.
	// In the first phase, (t1,t2) is added to fullParallelThreads whenever
	// t1 spawns t2.
	// In the second phase, it uses threadToParentMap:
	// (t1,t2) is added to fullParallelThreads if either
	// (t3,t2) in fullParallelThreads and t1 in descendants(t3) or
	// (t1,t3) is fullParallelThreads and t2 in descendants(t3).
	private Set<IntPair> fullParallelThreads;

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
	private boolean[] escE;

	private TIntObjectHashMap<TIntArrayList> threadToLocksMap;

	private AliasingCheckKind aliasingCheckKind;
	private EscapingCheckKind escapingCheckKind;
	private boolean checkMHP, checkLocks, checkCombined;
	private boolean modelThreads, modelLocks;
	
	private InstrScheme instrScheme;
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
		emptySet.setReadOnly();
		aliasingCheckKind = AliasingCheckKind.WEAK_CONCRETE;
		escapingCheckKind = EscapingCheckKind.WEAK_CONCRETE;
		checkMHP = System.getProperty("chord.dynrace.mhp", "true").equals("true");
		checkLocks = System.getProperty("chord.dynrace.locks", "false").equals("true");
		checkCombined = System.getProperty("chord.dynrace.combined", "false").equals("true");

		modelThreads = checkMHP || checkCombined;
		modelLocks = checkLocks || checkCombined;
		
		// if aliasingCheckKind == NONE then aliasingRacePairs = 1
		// if escapingCheckKind == NONE then escE = 1
		// if checkMHP == false then parallelRacePairs = 1
		// if checkLocks == false then guardedRacePairs = 1
		// if checkCombined == false then aliasingParallelUnguardedRacePairs = 1

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

		accessedE = new boolean[numE];
		raceInfoE = new RaceInfo[numE];
		for (int e = 0; e < numE; e++) {
			accessedE[e] = false;
			raceInfoE[e] = new RaceInfo();
		}
		
		if (modelThreads) {
			threadToParentMap = new TIntIntHashMap();
			threadToDescendantsMap = new TIntObjectHashMap<IntArraySet>();
			threadToLiveChildrenMap = new TIntObjectHashMap<IntArraySet>();
			halfParallelThreads = new HashSet<IntPair>();
			fullParallelThreads = new HashSet<IntPair>();
		}

		if (escapingCheckKind != EscapingCheckKind.NONE) {
			escObjs = new TIntHashSet();
			objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
			escE = new boolean[numE];
		}

		if (modelLocks)
			threadToLocksMap = new TIntObjectHashMap<TIntArrayList>();
	}

	private static interface IComparator {
		public boolean check(RaceElem re1, RaceElem re2);
	}
	
	private boolean check(int e1, int e2, IComparator comp) {
		List<RaceElem> l1 = raceInfoE[e1].raceElems;
		if (l1 == null) return false;
		List<RaceElem> l2 = raceInfoE[e2].raceElems;
		if (l2 == null) return false;
		int n1 = l1.size();
		int n2 = l2.size();
		for (int i = 0; i < n1; i++) {
			RaceElem re1 = l1.get(i);
			for (int j = 0; j < n2; j++) {
				RaceElem re2 = l2.get(j);
				if (comp.check(re1, re2))
					return true;
			}
		}
		return false;
	}
	
	@Override
	public void donePass() {
		if (modelThreads) {
			// build threadToDescendantsMap
			for (int c : threadToParentMap.keys()) {
				int p = threadToParentMap.get(c);
				while (p != 0) {
					IntArraySet s = threadToDescendantsMap.get(p);
					if (s == null) {
						s = new IntArraySet();
						threadToDescendantsMap.put(p, s);
					}
					s.add(c);
					p = threadToParentMap.get(p);
				}
			}
			// use threadToDescendantsMap to populate fullParallelThreads
			Set<IntPair> s = new HashSet<IntPair>();
			for (IntPair p : fullParallelThreads) {
				int t1 = p.idx0;
				int t2 = p.idx1;
				IntArraySet d1 = threadToDescendantsMap.get(t1);
				if (d1 == null) continue;
				IntArraySet d2 = threadToDescendantsMap.get(t2);
				if (d2 == null) continue;
				int n1 = d1.size(), n2 = d2.size();
				for (int i = 0; i < n1; i++) {
					int t3 = d1.get(i);
					for (int j = 0; j < n2; j++) {
						int t4 = d2.get(i);
						if (t3 < t4)
							fullParallelThreads.add(new IntPair(t3, t4));
						else
							fullParallelThreads.add(new IntPair(t4, t3));
					}
				}
			}
			fullParallelThreads.addAll(s);
		}

		ProgramRel relEscE = (ProgramRel) ClassicProject.g().getTrgt("escE");
		if (escapingCheckKind == EscapingCheckKind.NONE)
			relEscE.one();
		else {
			relEscE.zero();
			for (int e = 0; e < domE.size(); e++) {
				if (escE[e])
					relEscE.add(e);
			}
		}
		relEscE.save();

		ProgramRel relAliasingRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("aliasingRacePairs");
		if (aliasingCheckKind == AliasingCheckKind.NONE)
			relAliasingRacePairs.one();
		else
			relAliasingRacePairs.zero();

		ProgramRel relParallelRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("parallelRacePairs");
		if (!checkMHP)
			relParallelRacePairs.one();
		else
			relParallelRacePairs.zero();

		// guardedRacePairs contains those pairs in startingRacePairs that are
		// guarded by a common lock
		// Formally: (e1,e2) is in guardedRacePairs iff for each o such that
		// e1->o and e2->o: there exists an o' such that a lock is held on o'
		// by each thread while it accesses o at e1 or e2
		ProgramRel relGuardedRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("guardedRacePairs");
		if (!checkLocks)
			relGuardedRacePairs.one();
		else
			relGuardedRacePairs.zero();

		// combinedRacePairs contains those pairs in startingRacePairs that
		// *simultaneously* satisfy aliasing, parallel, and unguarded checks
		ProgramRel relCombinedRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("combinedRacePairs");
		if (!checkCombined)
			relCombinedRacePairs.one();
		else
			relCombinedRacePairs.zero();

		ProgramRel relStartingRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("startingRacePairs");
		relStartingRacePairs.load();
		IntPairIterable startingRacePairs = relStartingRacePairs.getAry2IntTuples();
		for (IntPair p : startingRacePairs) {
			int e1 = p.idx0;
			int e2 = p.idx1;
			if (accessedE[e1] && accessedE[e2]) {
				if (checkCombined) {
					if (check(e1, e2, combinedComparator))
						relCombinedRacePairs.add(e1, e2);
				} else {
					// intentionally allow the following checks to operate
					// on different RaceElem's for the same (e1,e2) since we
					// want to measure how much precision loss there is in
					// not doing a combined check
					if (aliasingCheckKind != AliasingCheckKind.NONE &&
							check(e1, e2, aliasComparator)) {
						relAliasingRacePairs.add(e1, e2);
						System.out.println("ALIASING: " + eStr(e1) + "," + eStr(e2)); 
					}
					if (checkMHP && check(e1, e2, mhpComparator)) {
						System.out.println("PARALLEL: " + eStr(e1) + "," + eStr(e2)); 
						relParallelRacePairs.add(e1, e2);
					}
					if (checkLocks && check(e1, e2, locksComparator)) {
						System.out.println("GUARDED: " + eStr(e1) + "," + eStr(e2)); 
						relGuardedRacePairs.add(e1, e2);
					}
				}
			}
       	}
		relAliasingRacePairs.save();
		relParallelRacePairs.save();
		relGuardedRacePairs.save();
		relCombinedRacePairs.save();
	}

	private final IComparator aliasComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			return re1.o == re2.o;
		}
	};

	private final IComparator mhpComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			int t1 = re1.t;
			int t2 = re2.t;
			if (t1 == t2) return false;
			if (isFullParallel(t1, t2))
				return true;
			if (!isLiveParallel(t1, re2.ts)) {
				// when t2 executes anything (including e2),
				// is t1 running in parallel?
				tmpPair.idx0 = t2;
				tmpPair.idx1 = t1;
				if (!halfParallelThreads.contains(tmpPair))
					return false;
			}
			if (!re1.ts.contains(t2)) {
				// when t1 executes anything (including e1),
				// is t2 running in parallel?
				tmpPair.idx0 = t1;
				tmpPair.idx1 = t2;
				if (!halfParallelThreads.contains(tmpPair))
					return false;
			}
			return true;
		}
	};

	private final IComparator locksComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			return re1.ls.overlaps(re2.ls);
		}
	};

	private final IComparator combinedComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			return re1.o == re2.o && mhpComparator.check(re1, re2) &&
				!re1.ls.overlaps(re2.ls);
		}
	};

	private IntPair tmpPair = new IntPair(0, 0);

	// Note: t1 and t2 can be in any order
	private boolean isFullParallel(int t1, int t2) {
		if (t1 < t2) {
			tmpPair.idx0 = t1;
			tmpPair.idx1 = t2;
		} else {
			tmpPair.idx0 = t2;
			tmpPair.idx1 = t1;
		}
		return fullParallelThreads.contains(tmpPair);
	}

	// can t run in parallel while another thread executes a
	// statement when set of "live children threads" is ts?
	private boolean isLiveParallel(int t, IntArraySet ts) {
		int n = ts.size();
		for (int i = 0; i < n; i++) {
			int t2 = ts.get(i);
			if (t2 == t)
				return true;
			IntArraySet ts2 = threadToDescendantsMap.get(t2);
			if (ts2.contains(t))
				return true;
		}
		return true;
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

	@Override
	public void processThreadStart(int p, int t, int o) {
		if (verbose) System.out.println("START: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (modelThreads) {
			int t2 = threadToParentMap.get(o);
			assert (t2 == 0);
			threadToParentMap.put(o, t);
			IntArraySet s = threadToLiveChildrenMap.get(t);
			if (s == null) {
				s = new IntArraySet();
				threadToLiveChildrenMap.put(t, s);
			} else
				assert (!s.contains(o));
			s.add(o);
		}
		if (escapingCheckKind != EscapingCheckKind.NONE)
			markAndPropEsc(o);
	}

	@Override
	public void processThreadJoin(int p, int t, int o) {
		if (verbose) System.out.println("JOIN: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (modelThreads) {
			IntArraySet s = threadToLiveChildrenMap.get(t);
			if (s != null && s.contains(o)) {
				int n = s.size();
				IntArraySet s2 = new IntArraySet(n - 1);
				for (int i = 0; i < n; i++) {
					int o2 = s.get(i);
					if (o2 != o)
						s2.add(o2);
				}
				threadToLiveChildrenMap.put(t, s2);
			}
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
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processGetstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETSTATIC: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutstaticPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " " + t + " " + b + " " + f);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapRd(e, t, b, -1);
		if (escapingCheckKind != EscapingCheckKind.NONE) {
	  		if (o != 0)
				markAndPropEsc(o);
		}
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " " + t + " " + b + " " + f);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " " + t + " " + b + " " + f);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " " + t + " " + b + " " + f + " " + o);
		processHeapRd(e, t, b, -1);
		processHeapWr(e, t, b, f, o);
	}

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " " + t + " " + b + " " + i);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " " + t + " " + b + " " + i + " " + o);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " " + t + " " + b + " " + i);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " " + t + " " + b + " " + i + " " + o);
		processHeapRd(e, t, b, i);
		processHeapWr(e, t, b, i, o);
	}

	private void processHeapRd(int e, int t, int b, int i) {
		if (e < 0) return;
		accessedE[e] = true;
		if (b == 0) return;
		RaceElem re = new RaceElem();
		if (modelThreads) {
			re.t = t;
			IntArraySet ts = threadToLiveChildrenMap.get(t);
			if (ts == null)
				ts = emptySet;
			re.ts = ts;
		} else {
			re.t = 0;
			re.ts = emptySet;
		}
		if (checkLocks) {
			re.ls = emptySet; // TODO
		} else
			re.ls = emptySet;
		switch (aliasingCheckKind) {
		case NONE:
			break;
		case WEAK_CONCRETE:
			// ignore array element index for WEAK_CONCRETE
			// i.e., do not distinguish between different elements of same array
			re.o = b;
			break;
		case CONCRETE:
			re.o = (i == -1) ? b : CoreEventHandler.getPrimitiveId(b, i);
			break;
		default:
			Messages.fatal("Unknown aliasing check kind: " + aliasingCheckKind);
		}
		if (escapingCheckKind != EscapingCheckKind.NONE) {
			if (!escE[e] && escObjs.contains(b))
				escE[e] = true;
		}
		raceInfoE[e].addRaceElem(re);
	}

	private void processHeapWr(int e, int t, int b, int fIdx, int r) {
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

class RaceElem {
	int t;	// ID of accessing thread
	long o;	// ID of object accessed:
					// 0 if static field access, o if instance field access,
					// o+i if array element access
	IntArraySet ls;	// set of locks held by this thread during this access
	IntArraySet ts;	// set of threads that may run in parallel when this
							// thread executes this access
	public RaceElem() { }
	public RaceElem(int t, long o, IntArraySet ls, IntArraySet ts) {
		this.t = t;
		this.o = o;
		this.ls = ls;
		this.ts = ts;
	}
	public int hashCode() {
		return (int) o;
	}
	public boolean equals(Object o) {
		if (o instanceof RaceElem)
			return equals((RaceElem) o);
		return false;
	}
	public boolean equals(RaceElem that) {
		return t == that.t && o == that.o && ls.equals(that.ls) &&
			ts.equals(that.ts);
	}
	public String toString() {
		String str = "t=" + t + ", o=" + o + ", ls=";
		if (ls == null)
			str += "null,";
		else {
			int n = ls.size();
			for (int i = 0; i < n; i++)
				str += ls.get(i) + ","; 
		}
		str += " ts=";
		if (ts == null)
			str += "null";
		else {
			int n = ts.size();
			for (int i = 0; i < n; i++)
				str += ts.get(i) + ","; 
		}
		return str;
	}
}

class RaceInfo {
	List<RaceElem> raceElems;  // allocated lazily
	public void addRaceElem(RaceElem re) {
		if (raceElems == null) {
			raceElems = new ArrayList<RaceElem>();
			raceElems.add(re);
		} else if (!raceElems.contains(re))
			raceElems.add(re);
	}
}