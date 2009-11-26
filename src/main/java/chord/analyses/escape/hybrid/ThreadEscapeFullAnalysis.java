/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.util.tuple.integer.IntPair;
import chord.project.ProgramRel;
import chord.bddbddb.Rel.IntPairIterable;
import chord.project.Properties;
import chord.util.ChordRuntimeException;
import chord.analyses.alias.ICICG;
import chord.analyses.alias.ThrSenAbbrCICGAnalysis;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.JavaAnalysis;
import chord.project.Program;
import chord.project.Project;
import chord.util.ArraySet;
import chord.util.CompareUtils;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.Timer;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	    name = "thresc-full-java"
	)
public class ThreadEscapeFullAnalysis extends JavaAnalysis {
    private final static boolean DEBUG = false;
	public final static Set<IntTrio> emptyHeap =
		Collections.emptySet();
	public final static IntArraySet nilPts = new IntArraySet(0);
	public final static IntArraySet escPts = new IntArraySet(0);
	private DomV domV;
	private DomF domF;
	private DomH domH;
	private DomI domI;
	private DomM domM;
	private DomE domE;
	private int varId[];
	private TObjectIntHashMap<jq_Method> methToNumVars =
		new TObjectIntHashMap<jq_Method>();
	private List<Location> workList1 = new ArrayList<Location>();
	private List<PathEdge> workList2 = new ArrayList<PathEdge>();
	private Map<Quad, Location> invkQuadToLoc =
		new HashMap<Quad, Location>();
	private Map<Inst, Set<PathEdge>> pathEdges =
		new HashMap<Inst, Set<PathEdge>>();
	private Map<jq_Method, Set<SummEdge>> summEdges =
		new HashMap<jq_Method, Set<SummEdge>>();
	private MyQuadVisitor qv = new MyQuadVisitor();
	private ICICG cicg;
	private Map<jq_Method, Set<Quad>> callersMap =
		new HashMap<jq_Method, Set<Quad>>();
	private Map<Quad, Set<jq_Method>> targetsMap =
		new HashMap<Quad, Set<jq_Method>>();

	private Set<Quad> getCallers(jq_Method m) {
		Set<Quad> callers = callersMap.get(m);
		if (callers == null) {
			callers = cicg.getCallers(m);
			callersMap.put(m, callers);
		}
		return callers;
	}

	private Set<jq_Method> getTargets(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets;
	}

	// TODO: E -> CE in each of 3 below sets?
	// set of heap insts deemed possibly escaping by
	// whole-program analysis
	private Set<Quad> escHeapInsts = new HashSet<Quad>();
	// set of heap insts proven thread local by whole-program analysis
	private Set<Quad> locHeapInsts = new HashSet<Quad>();
	private Set<Quad> currAllocs;
	private Set<Quad> currLocHeapInsts = new HashSet<Quad>();
	private Set<Quad> currEscHeapInsts = new HashSet<Quad>();
	private jq_Method threadStartMethod;
	private jq_Method mainMethod;
	private volatile boolean timedOut = false;
	private volatile boolean doneAll = false;
	private boolean done = false;
	private final Object lock = new Object();
	private Thread timerThread = new Thread() {
		public void run() {
			try {
				while (true) {
					if (doneAll)
						break;
					timedOut = false;
					synchronized (lock) {
						done = false;
						lock.notify();
						lock.wait(600000);
					}
					timedOut = true;
					synchronized (lock) {
						while (!done)
							lock.wait();
					}
				}
			} catch (InterruptedException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
	};
	public void run() {
		// ThreadEscapePathAnalysis analysis =
		//	(ThreadEscapePathAnalysis) Project.getTrgt("thresc-path-java");
		// Project.runTask(analysis);
		// Map<Set<Quad>, Set<Quad>> allocInstsToHeapInsts =
		//	analysis.getAllocInstsToHeapInstsMap();

		threadStartMethod = Program.v().getThreadStartMethod();
		mainMethod = Program.v().getMainMethod();
		domV = (DomV) Project.getTrgt("V");
		Project.runTask(domV);
		domF = (DomF) Project.getTrgt("F");
		Project.runTask(domF);
		domH = (DomH) Project.getTrgt("H");
		Project.runTask(domH);
		domI = (DomI) Project.getTrgt("I");
		Project.runTask(domI);
		domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);
		domE = (DomE) Project.getTrgt("E");
		Project.runTask(domE);

		ProgramRel rel = (ProgramRel) Project.getTrgt("EH");
		rel.load();
		IntPairIterable tuples = rel.getAry2IntTuples();
        Map<Quad, Set<Quad>> heapInstToAllocInsts =
			new HashMap<Quad, Set<Quad>>();
		for (IntPair tuple : tuples) {
			int eIdx = tuple.idx0;
			int hIdx = tuple.idx1;
			Quad e = (Quad) domE.get(eIdx);
			Quad h = (Quad) domH.get(hIdx);
            Set<Quad> allocs = heapInstToAllocInsts.get(e);
            if (allocs == null) {
                allocs = new ArraySet<Quad>();
                heapInstToAllocInsts.put(e, allocs);
            }
            allocs.add(h);
		}
		Map<Set<Quad>, Set<Quad>> allocInstsToHeapInsts =
        	new HashMap<Set<Quad>, Set<Quad>>();
        for (Map.Entry<Quad, Set<Quad>> e :
                heapInstToAllocInsts.entrySet()) {
            Quad heapInst = e.getKey();
            Set<Quad> allocInsts = e.getValue();
            Set<Quad> heapInsts = allocInstsToHeapInsts.get(allocInsts);
            if (heapInsts == null) {
                heapInsts = new ArraySet<Quad>();
                allocInstsToHeapInsts.put(allocInsts, heapInsts);
            }
            heapInsts.add(heapInst);
        }

		int numV = domV.size();
		varId = new int[numV];
		for (int vIdx = 0; vIdx < numV;) {
			Register v = domV.get(vIdx);
			jq_Method m = domV.getMethod(v);
			int n = m.getNumVarsOfRefType();
			methToNumVars.put(m, n);
			for (int i = 0; i < n; i++)
				varId[vIdx + i] = i;
			vIdx += n;
		}

		ThrSenAbbrCICGAnalysis cicgAnalysis =
			(ThrSenAbbrCICGAnalysis) Project.getTrgt(
				"thrsen-abbr-cicg-java");
		Project.runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();
		Set<jq_Method> roots = cicg.getRoots();

		timerThread.start();

		int n = allocInstsToHeapInsts.size();
		int i = 0;
		for (Map.Entry<Set<Quad>, Set<Quad>> e :
				allocInstsToHeapInsts.entrySet()) {
			//////////////////
			currLocHeapInsts = e.getValue();
			boolean consider = false;
			for (Quad q : currLocHeapInsts) {
				if (domE.indexOf(q) == 12572) {
					consider = true;
					break;
				}
			}
			if (!consider)
				continue;
			//////////////////
			try {
				synchronized (lock) {
					while (done)
						lock.wait();
				}
			} catch (InterruptedException ex) {
				throw new ChordRuntimeException(ex);
			}
			currAllocs = e.getKey();
			currLocHeapInsts = e.getValue();
			currEscHeapInsts.clear();
			System.out.println("**************");
			System.out.println("currHeapInsts:");
			for (Quad q : currLocHeapInsts)
				System.out.println("\t" + Program.v().toVerboseStr(q) + " " + domE.indexOf(q));
			System.out.println("currAllocInsts:");
			for (Quad q : currAllocs)
				System.out.println("\t" + Program.v().toVerboseStr(q));
			Timer timer = new Timer("hybrid-thresc-timer");
			timer.init();
			try {
				for (jq_Method root : roots) 
					processThread(root);
			} catch (ThrEscException ex) {
				// do nothing
			}
			for (Quad q : currLocHeapInsts)
				System.out.println("LOC: " + Program.v().toVerboseStr(q));
			for (Quad q : currEscHeapInsts)
				System.out.println("ESC: " + Program.v().toVerboseStr(q));
			locHeapInsts.addAll(currLocHeapInsts);
			escHeapInsts.addAll(currEscHeapInsts);
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
			i++;
			if (i == n)
				doneAll = true;
			synchronized (lock) {
				done = true;
				lock.notify();
			}
		}
		try {
			String outDirName = Properties.outDirName;
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullEscE.txt")));
				for (Quad e : escHeapInsts)
					writer.println(Program.v().toPosStr(e));
				writer.close();
			}
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullLocE.txt")));
				for (Quad e : locHeapInsts)
					writer.println(Program.v().toPosStr(e));
				writer.close();
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	private void processThread(jq_Method root) {
		System.out.println("PROCESSING THREAD: " + root);
		init(root);
		while (!workList1.isEmpty()) {
			if (timedOut) {
				System.out.println("TIMED OUT");
				for (Quad q : currLocHeapInsts) {
					currEscHeapInsts.add(q);
				}
				currLocHeapInsts.clear();
				throw new ThrEscException();
			}
			int last = workList1.size() - 1;
			Location loc = workList1.remove(last);
			PathEdge pe  = workList2.remove(last);
			Quad q = loc.q;
			if (DEBUG)
				System.out.println("Processing path edge: loc: " + loc + " pe: " + pe);
			if (q == null) {
				BasicBlock bb = loc.bb;
				if (bb.isEntry()) {
					for (Object o : bb.getSuccessorsList()) {
						BasicBlock bb2 = (BasicBlock) o;
						Quad q2;
						int q2Idx;
						if (bb2.size() == 0) {
							q2 = null;
							q2Idx = -1;
						} else {
							q2 = bb2.getQuad(0);
							q2Idx = 0;
						}
						Location loc2 = new Location(loc.m, bb2, q2Idx, q2);
						addPathEdge(loc2, pe);
					}
				} else {
					assert (bb.isExit()); 
					processReturn(loc.m, q, pe);
				}
			} else {
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					processInvoke(loc, pe);
				} else if (op instanceof Return) {
					processReturn(loc.m, q, pe);
				} else {
					DstNode dstNode = pe.dstNode;
					qv.iDstNode = dstNode;
					qv.oDstNode = dstNode;
					q.accept(qv);
					DstNode dstNode2 = qv.oDstNode;
					PathEdge pe2 = (dstNode2 == dstNode) ? pe :
						new PathEdge(pe.srcNode, dstNode2);
					propagateToSucc(loc.m, loc.bb, loc.qIdx, pe2);
				}
			}
		}
	}
	private void propagateToSucc(jq_Method m, BasicBlock bb, int qIdx, PathEdge pe) {
		if (qIdx != bb.size() - 1) {
			int q2Idx = qIdx + 1;
			Quad q2 = bb.getQuad(q2Idx);
			Location loc2 = new Location(m, bb, q2Idx, q2);
			addPathEdge(loc2, pe);
			return;
		}
		for (Object o : bb.getSuccessorsList()) {
			BasicBlock bb2 = (BasicBlock) o;
			int q2Idx;
			Quad q2;
			if (bb2.size() == 0) {
				q2Idx = -1;
				q2 = null;
			} else {
				q2Idx = 0;
				q2 = bb2.getQuad(0);
			}
			Location loc2 = new Location(m, bb2, q2Idx, q2);
			addPathEdge(loc2, pe);
		}
	}
	private void addPathEdge(Location loc, PathEdge pe) {
		if (DEBUG)
			System.out.println("\tAdding path edge: loc " + loc + " pe: " + pe);
		Quad q = loc.q;
		Inst i = (q != null) ? q : loc.bb;
		Set<PathEdge> peSet = pathEdges.get(i);
		if (peSet == null) {
			peSet = new HashSet<PathEdge>();
			pathEdges.put(i, peSet);
			if (q != null && (q.getOperator() instanceof Invoke))
				invkQuadToLoc.put(q, loc);
		}
		if (peSet.add(pe)) {
			workList1.add(loc);
			workList2.add(pe);
		}
	}
	private void init(jq_Method root) {
		pathEdges.clear();
		summEdges.clear();
		// clear worklist: may not be empty due to thrown exception
		workList1.clear();
		workList2.clear();
		int n = methToNumVars.get(root);
		IntArraySet[] env = new IntArraySet[n];
		for (int i = 0; i < n; i++)
			env[i] = nilPts;
		if (root == threadStartMethod) {
			// arg of start method of java.lang.Thread escapes
			env[0] = escPts;
		}
		BasicBlock bb = root.getCFG().entry();
		SrcNode srcNode = new SrcNode(env, emptyHeap);
		DstNode dstNode = new DstNode(env, emptyHeap, nilPts);
		PathEdge pe = new PathEdge(srcNode, dstNode);
		Location loc = new Location(root, bb, -1, null);
		addPathEdge(loc, pe);
	}
	private IntArraySet tmpPts = new IntArraySet();
	private IntArraySet getPtsFromHeap(IntArraySet bPts, int fIdx,
			Set<IntTrio> heap, IntArraySet rPts) {
		assert (bPts != escPts);
		tmpPts.clear();
		for (IntTrio t : heap) {
			if (t.idx1 != fIdx)
				continue;
			int h1Idx = t.idx0;
			if (bPts.contains(h1Idx)) {
				int h2Idx = t.idx2;
				if (h2Idx == 0)
					return escPts;
				tmpPts.add(h2Idx);
			}
		}
		if (tmpPts.isEmpty())
			return nilPts;
		if (tmpPts.equals(rPts))
			return rPts;
		return new IntArraySet(tmpPts);
	}
	private void processInvoke(Location loc, PathEdge pe) {
		Quad q = loc.q;
		MethodOperand mo = Invoke.getMethod(q);
		mo.resolve();
		jq_Method tgt = mo.getMethod();
		if (tgt == threadStartMethod) {
			if (DEBUG) System.out.println("Target is thread start method");
			DstNode dstNode = pe.dstNode;
			IntArraySet[] dstEnv = dstNode.env;
			RegisterOperand ao = Invoke.getParam(q, 0);
			int aIdx = getIdx(ao);
			IntArraySet aPts = dstEnv[aIdx];
			DstNode dstNode2 = (aPts == escPts || aPts == nilPts) ? dstNode :
				propagateEsc(aPts, dstNode);
			PathEdge pe2 = (dstNode == dstNode2) ? pe :
				new PathEdge(pe.srcNode, dstNode2);
			propagateToSucc(loc.m, loc.bb, loc.qIdx, pe2);
			return;
		}
		DstNode dstNode = pe.dstNode;
		IntArraySet[] dstEnv = dstNode.env;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        jq_Method m = Program.v().getMethod(q);
		for (jq_Method m2 : getTargets(q)) {
			if (DEBUG) System.out.println("Target: " + m2);
			Set<SummEdge> seSet = summEdges.get(m2);
			boolean found = false;
			if (seSet != null) {
				for (SummEdge se : seSet) {
					if (DEBUG) System.out.println("Testing summary edge: " + se);
					if (propagateSEtoPE(pe, loc, m, se)) {
						if (DEBUG) System.out.println("Match found");
						found = true;
					}
				}
			}
			if (!found) {
				if (DEBUG) System.out.println("No match found");
		        int numVars = methToNumVars.get(m2);
		        IntArraySet[] env = new IntArraySet[numVars];
		        int mIdx = 0;
		        for (int i = 0; i < numArgs; i++) {
		            RegisterOperand ao = args.get(i);
		            if (ao.getType().isReferenceType()) {
		                int aIdx = getIdx(ao);
		                IntArraySet pts = dstEnv[aIdx];
		                env[mIdx++] = pts;
					}
		        }
		        while (mIdx < numVars)
		        	env[mIdx++] = nilPts;
				SrcNode srcNode2 = new SrcNode(env, dstNode.heap);
				DstNode dstNode2 = new DstNode(env, dstNode.heap, nilPts);
				PathEdge pe2 = new PathEdge(srcNode2, dstNode2);
				BasicBlock bb2 = m2.getCFG().entry();
				Location loc2 = new Location(m2, bb2, -1, null);
				addPathEdge(loc2, pe2);
			}
		}
	}
	private void processReturn(jq_Method m, Quad q, PathEdge pe) {
		DstNode dstNode = pe.dstNode;
		IntArraySet rPts = nilPts;
		// q may be null in which case pe.bb is exit basic block
		if (q != null) {
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				if (ro.getType().isReferenceType()) {
					int rIdx = getIdx(ro);
					rPts = dstNode.env[rIdx];
				}
			}
		}
		RetNode retNode = new RetNode(rPts, dstNode.heap, dstNode.esc);
		SummEdge se = new SummEdge(pe.srcNode, retNode);
		Set<SummEdge> seSet = summEdges.get(m);
		if (DEBUG) System.out.println("Testing summary edge: " + se);
		if (seSet == null) {
			seSet = new HashSet<SummEdge>();
			summEdges.put(m, seSet);
		}
		if (!seSet.add(se)) {
			// summary edge se already exists
			if (DEBUG) System.out.println("Already exists");
			return;
		}
		boolean flag = false;
		if (DEBUG) System.out.println("Not found; adding");
		for (Quad q2 : getCallers(m)) {
			if (DEBUG) System.out.println("Caller: " + q2);
			jq_Method m2 = Program.v().getMethod(q2);
			Set<PathEdge> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			peSet = new ArraySet<PathEdge>(peSet);
			Location loc2 = invkQuadToLoc.get(q2);
			for (PathEdge pe2 : peSet) {
				if (DEBUG) System.out.println("Testing path edge: " + pe2);
				boolean match = propagateSEtoPE(pe2, loc2, m2, se);
				if (match) {
					if (DEBUG) System.out.println("Match found");
					flag = true;
				} else {
					if (DEBUG) System.out.println("No match found");
				}
			}
		}
		if (m != mainMethod && m != threadStartMethod)
			assert flag;
	}
	private boolean propagateSEtoPE(PathEdge clrPE, Location loc,
			jq_Method clrM, SummEdge tgtSE) {
		DstNode clrDstNode = clrPE.dstNode;
		SrcNode tgtSrcNode = tgtSE.srcNode;
		if (!clrDstNode.heap.equals(tgtSrcNode.heap))
			return false;
		IntArraySet[] clrDstEnv = clrDstNode.env;
		IntArraySet[] tgtSrcEnv = tgtSrcNode.env;
		Quad q = loc.q;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        for (int i = 0, fIdx = 0; i < numArgs; i++) {
            RegisterOperand ao = args.get(i);
            if (ao.getType().isReferenceType()) {
                int aIdx = getIdx(ao);
                IntArraySet aPts = clrDstEnv[aIdx];
                IntArraySet fPts = tgtSrcEnv[fIdx];
                if (!CompareUtils.areEqual(aPts, fPts))
                	return false;
                fIdx++;
			}
		}
		RetNode tgtRetNode = tgtSE.retNode;
        int n = clrDstEnv.length;
        IntArraySet[] clrDstEnv2 = new IntArraySet[n];
        RegisterOperand ro = Invoke.getDest(q);
        int rIdx = -1;
        if (ro != null && ro.getType().isReferenceType()) {
        	rIdx = getIdx(ro);
        	clrDstEnv2[rIdx] = tgtRetNode.pts;
        }
		IntArraySet tgtRetEsc = tgtRetNode.esc;
        for (int i = 0; i < n; i++) {
        	if (i == rIdx)
        		continue;
        	IntArraySet pts = clrDstEnv[i];
        	if (pts != escPts && pts.overlaps(tgtRetEsc)) {
        		pts = escPts;
        	}
        	clrDstEnv2[i] = pts;
        }
        IntArraySet clrDstEsc = clrDstNode.esc;
        IntArraySet clrDstEsc2;
		if (tgtRetEsc == nilPts)
 			clrDstEsc2 = clrDstEsc;
		else {
			clrDstEsc2 = new IntArraySet(clrDstEsc);
        	clrDstEsc2.addAll(tgtRetEsc);
		}
        DstNode clrDstNode2 = new DstNode(clrDstEnv2,
        	tgtRetNode.heap, clrDstEsc2);
        PathEdge pe2 = new PathEdge(clrPE.srcNode, clrDstNode2);
        propagateToSucc(loc.m, loc.bb, loc.qIdx, pe2);
        return true;
	}
	private DstNode propagateEsc(IntArraySet pts, DstNode iDstNode) {
		assert (pts != escPts);
		assert (pts != nilPts);
		IntArraySet dstEsc = iDstNode.esc;
		IntArraySet dstEsc2 = new IntArraySet(pts);
		Set<IntTrio> dstHeap = iDstNode.heap;
		IntArraySet[] dstEnv = iDstNode.env;
		boolean changed;
		do {
			changed = false;
			for (IntTrio t : dstHeap) {
				int hIdx = t.idx0;
				assert (hIdx != 0);
				if (dstEsc2.contains(hIdx)) {
					int h2Idx = t.idx2;
					if (h2Idx != 0 && dstEsc2.add(h2Idx))
						changed = true;
				}
			}
		} while (changed);
		int n = dstEnv.length;
		IntArraySet[] dstEnv2 = new IntArraySet[n];  // TODO
		for (int i = 0; i < n; i++) {
			IntArraySet pts2 = dstEnv[i];
			if (pts2 == escPts || dstEsc2.overlaps(pts2))
				dstEnv2[i] = escPts;
			else
				dstEnv2[i] = pts2;
		}
		Set<IntTrio> dstHeap2 = new ArraySet<IntTrio>(dstHeap.size());  // TODO
		for (IntTrio t : dstHeap) {
			int hIdx = t.idx0;
			if (!dstEsc2.contains(hIdx)) {
				int h2Idx = t.idx2;
				if (dstEsc2.contains(h2Idx))
					dstHeap2.add(new IntTrio(hIdx, t.idx1, 0));
				else
					dstHeap2.add(t);
			}
		}
		// MAYUR System.out.println("XXX heap size: " + dstHeap2.size());
		dstEsc2.addAll(dstEsc);
		// MAYUR System.out.println("AAA esc size: " + dstEsc2.size());
		return new DstNode(dstEnv2, dstHeap2, dstEsc2);
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		DstNode iDstNode;
		DstNode oDstNode;
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}
		public void visitMove(Quad q) {
	        RegisterOperand lo = Move.getDest(q);
			jq_Type t = lo.getType();
	        if (!t.isReferenceType())
	        	return;
	        IntArraySet[] dstEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			IntArraySet lPts = dstEnv[lIdx];
			Operand rx = Move.getSrc(q);
			IntArraySet lPts2;
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				int rIdx = getIdx(ro);
				lPts2 = dstEnv[rIdx];
			} else
				lPts2 = nilPts;
			if (lPts2 == lPts)
				return;
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = lPts2;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap, iDstNode.esc);
		}
		public void visitPhi(Quad q) {
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			if (t == null)
				return;
			if (!t.isReferenceType())
				return;
	        IntArraySet[] dstEnv = iDstNode.env;
			ParamListOperand ros = Phi.getSrcs(q);
			int n = ros.length();
			tmpPts.clear();
			IntArraySet pPts = tmpPts;
			for (int i = 0; i < n; i++) {
				RegisterOperand ro = ros.get(i);
				if (ro != null) {
					int rIdx = getIdx(ro);
					IntArraySet rPts = dstEnv[rIdx];
					if (rPts == escPts) {
						pPts = escPts;
						break;
					}
					pPts.addAll(rPts);
				}
			}
			int lIdx = getIdx(lo);
			IntArraySet lPts = dstEnv[lIdx];
			IntArraySet lPts2;
			if (pPts == escPts) {
				if (lPts == escPts)
					return;
				lPts2 = escPts;
			} else if (pPts.isEmpty()) {
				if (lPts == nilPts)
					return;
				lPts2 = nilPts;
			} else {
				if (pPts.equals(lPts))
					return;
				lPts2 = new IntArraySet(pPts);
			}
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = lPts2;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap, iDstNode.esc);
		}
		public void visitALoad(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, ALoad.getBase(q));
			Operator op = q.getOperator();
			if (!((ALoad) op).getType().isReferenceType())
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			Set<IntTrio> dstHeap = iDstNode.heap;
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			int bIdx = getIdx(bo);
			IntArraySet bPts = dstEnv[bIdx];
			RegisterOperand lo = ALoad.getDest(q);
			int lIdx = getIdx(lo);
			IntArraySet lPts = dstEnv[lIdx];
			IntArraySet lPts2;
			if (bPts == escPts)
				lPts2 = escPts;
			else
				lPts2 = getPtsFromHeap(bPts, 0, dstHeap, lPts);
			if (lPts2 == lPts)
				return;
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = lPts2;
			oDstNode = new DstNode(dstEnv2, dstHeap, iDstNode.esc);
		}
		public void visitGetfield(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, Getfield.getBase(q));
			FieldOperand fo = Getfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType())
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			Set<IntTrio> dstHeap = iDstNode.heap;
			RegisterOperand lo = Getfield.getDest(q);
			int lIdx = getIdx(lo);
			IntArraySet lPts = dstEnv[lIdx];
			Operand bx = Getfield.getBase(q);
			IntArraySet lPts2;
			if (bx instanceof RegisterOperand) {
				RegisterOperand bo = (RegisterOperand) bx;
				int bIdx = getIdx(bo);
				IntArraySet bPts = dstEnv[bIdx];
				if (bPts == escPts) {
					lPts2 = escPts;
				} else {
					int fIdx = domF.indexOf(f);
					lPts2 = getPtsFromHeap(bPts, fIdx, dstHeap, lPts);
				}
			} else
				lPts2 = nilPts;
			if (lPts2 == lPts)
				return;
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = lPts2;
			oDstNode = new DstNode(dstEnv2, dstHeap, iDstNode.esc);
		}
		public void visitAStore(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, AStore.getBase(q));
			Operator op = q.getOperator();
			if (!((AStore) op).getType().isReferenceType())
				return;
			Operand rx = AStore.getValue(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
			RegisterOperand ro = (RegisterOperand) rx;
			IntArraySet[] dstEnv = iDstNode.env;
			int rIdx = getIdx(ro);
			IntArraySet rPts = dstEnv[rIdx];
			if (rPts == nilPts)
				return;
			int bIdx = getIdx(bo);
			IntArraySet bPts = dstEnv[bIdx];
			if (bPts == nilPts)
				return;
			if (bPts != escPts) {
				Set<IntTrio> dstHeap = iDstNode.heap;
				ArraySet<IntTrio> dstHeap2 = null;
				int nb = bPts.size();
				if (rPts == escPts) {
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						IntTrio trio = new IntTrio(hIdx, 0, 0);
						if (!dstHeap.contains(trio)) {
							if (dstHeap2 == null)
								dstHeap2 = new ArraySet<IntTrio>(dstHeap);
							dstHeap2.add(trio);
						}
					}
				} else {
					int nr = rPts.size();
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						for (int j = 0; j < nr; j++) {
							int hIdx2 = rPts.get(j);
							IntTrio trio = new IntTrio(hIdx, 0, hIdx2);
							if (dstHeap2 != null)
								dstHeap2.add(trio);
							else if (!dstHeap.contains(trio)) {
								dstHeap2 = new ArraySet<IntTrio>(dstHeap);
								dstHeap2.addForcibly(trio);
							}
						}
					}
				}
				if (dstHeap2 == null)
					return;
				oDstNode = new DstNode(dstEnv, dstHeap2, iDstNode.esc);
				return;
			}
			if (rPts == escPts)
				return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitPutfield(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, Putfield.getBase(q));
			FieldOperand fo = Putfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType())
				return;
			Operand rx = Putfield.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			Operand bx = Putfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			RegisterOperand ro = (RegisterOperand) rx;
			int rIdx = getIdx(ro);
			IntArraySet rPts = dstEnv[rIdx];
			if (rPts == nilPts)
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			IntArraySet bPts = dstEnv[bIdx];
			if (bPts == nilPts)
				return;
			if (bPts != escPts) {
				Set<IntTrio> dstHeap = iDstNode.heap;
				ArraySet<IntTrio> dstHeap2 = null;
				int fIdx = domF.indexOf(f);
				int nb = bPts.size();
				if (rPts == escPts) {
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						IntTrio trio = new IntTrio(hIdx, fIdx, 0);
						if (!dstHeap.contains(trio)) {
							if (dstHeap2 == null)
								dstHeap2 = new ArraySet<IntTrio>(dstHeap);
							dstHeap2.add(trio);
						}
					}
				} else {
					int nr = rPts.size();
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						for (int j = 0; j < nr; j++) {
							int hIdx2 = rPts.get(j);
							IntTrio trio = new IntTrio(hIdx, fIdx, hIdx2);
							if (dstHeap2 != null)
								dstHeap2.add(trio);
							else if (!dstHeap.contains(trio)) {
								dstHeap2 = new ArraySet<IntTrio>(dstHeap);
								dstHeap2.addForcibly(trio);
							}
						}
					}
				}
				if (dstHeap2 == null)
					return;
				oDstNode = new DstNode(dstEnv, dstHeap2, iDstNode.esc);
				return;
			}
			if (rPts == escPts)
				return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitPutstatic(Quad q) {
			FieldOperand fo = Putstatic.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
	        if (!f.getType().isReferenceType())
	        	return;
	        Operand rx = Putstatic.getSrc(q);
	        if (!(rx instanceof RegisterOperand))
	        	return;
			IntArraySet[] dstEnv = iDstNode.env;
	        RegisterOperand ro = (RegisterOperand) rx;
	        int rIdx = getIdx(ro);
	        IntArraySet rPts = dstEnv[rIdx];
	        if (rPts == escPts || rPts == nilPts)
	        	return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitGetstatic(Quad q) {
			FieldOperand fo = Getstatic.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
	        if (!f.getType().isReferenceType())
	        	return;
			IntArraySet[] dstEnv = iDstNode.env;
	        RegisterOperand lo = Getstatic.getDest(q);
	        int lIdx = getIdx(lo);
			if (dstEnv[lIdx] == escPts)
				return;
	        IntArraySet[] dstEnv2 = copy(dstEnv);
	       	dstEnv2[lIdx] = escPts;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap, iDstNode.esc);
		}
		public void visitNew(Quad q) {
			RegisterOperand vo = New.getDest(q);
			processAlloc(q, vo);
		}
		public void visitNewArray(Quad q) {
			RegisterOperand vo = NewArray.getDest(q);
			processAlloc(q, vo);
		}
		private void processAlloc(Quad q, RegisterOperand vo) {
			IntArraySet[] dstEnv = iDstNode.env;
			int vIdx = getIdx(vo);
			IntArraySet dstPts = dstEnv[vIdx];
			if (currAllocs.contains(q)) {
				int hIdx = domH.indexOf(q);
				if (dstPts.size() == 1 && dstPts.contains(hIdx))
					return;
				dstPts = new IntArraySet(1);
				dstPts.add(hIdx);
			} else {
				if (dstPts == escPts)
					return;
				dstPts = escPts;
			}
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[vIdx] = dstPts;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap, iDstNode.esc);
		}
		private void check(Quad q, Operand bx) {
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			IntArraySet pts = iDstNode.env[bIdx];
			if (pts == escPts) {
				currLocHeapInsts.remove(q);
				currEscHeapInsts.add(q);
				if (currLocHeapInsts.size() == 0)
					throw new ThrEscException();
			}
		}
	}

	/*****************************************************************
	 * Frequently used functions
     *****************************************************************/

	private int getIdx(RegisterOperand ro) {
		Register r = ro.getRegister();
		int vIdx = domV.indexOf(r);
		return varId[vIdx];
	}
	private static IntArraySet[] copy(IntArraySet[] a) {
		int n = a.length;
		IntArraySet[] b = new IntArraySet[n];
        for (int i = 0; i < n; i++)
        	b[i] = a[i];
        return b;
	}

	/*****************************************************************
	 * Printing functions
     *****************************************************************/

	public static String toString(IntArraySet[] env) {
		String s = null;
		for (IntArraySet e : env) {
			String x = (e == nilPts) ? "N" : (e == escPts) ? "E" : toString(e);
			s = (s == null) ? x : s + "," + x;
		}
		if (s == null)
			return "[]";
		return "[" + s + "]";
	}
	public static String toString(Set<IntTrio> heap) {
		String s = null;
		for (IntTrio t : heap) {
			String x = "<" + t.idx0 + " " + t.idx1 + " " + t.idx2 + ">";
			s = (s == null) ? x : s + "," + x;
		}
		if (s == null)
			return "[]";
		return "[" + s + "]";
	}
	public static String toString(IntArraySet set) {
		if (set == null)
			return "null";
		String s = null;
		int n = set.size();
		for (int i = 0; i < n; i++) {
			int e = set.get(i);
			s = (s == null) ? "" + e : s + " " + e;
		}
		if (s == null)
			return "{}";
		return "{" + s + "}";
	}
}

class Location {
    // q == null iff qIdx == -1 iff empty bb
	final jq_Method m;
	final BasicBlock bb;
    final int qIdx;
    final Quad q;
	public Location(jq_Method m, BasicBlock bb, int qIdx, Quad q) {
		this.m = m;
		this.bb = bb;
		this.qIdx = qIdx;
		this.q = q;
	}
	public int hashCode() {
		return (q != null) ? q.hashCode() : bb.hashCode();
	}
	public boolean equals(Object o) {
		if (!(o instanceof Location))
			return false;
		Location l = (Location) o;
		return (q != null) ? (q == l.q) : (bb == l.bb);
	}
	public String toString() {
		return Integer.toString(q.getID());
	}
}

