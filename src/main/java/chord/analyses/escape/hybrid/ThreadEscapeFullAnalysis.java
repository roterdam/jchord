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

import chord.project.Properties;
import chord.project.ChordRuntimeException;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
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
	private TObjectIntHashMap<jq_Method> methToVar0Idx =
		new TObjectIntHashMap<jq_Method>();
	private TObjectIntHashMap<jq_Method> methToNumVars =
		new TObjectIntHashMap<jq_Method>();
	private List<Pair<Pair<Ctxt, jq_Method>, PathEdge>> workList =
		new ArrayList<Pair<Pair<Ctxt, jq_Method>, PathEdge>>();
	private Map<Pair<Ctxt, jq_Method>, Set<PathEdge>> pathEdges =
		new HashMap<Pair<Ctxt, jq_Method>, Set<PathEdge>>();
	private Map<Pair<Ctxt, jq_Method>, Set<SummEdge>> summEdges =
		new HashMap<Pair<Ctxt, jq_Method>, Set<SummEdge>>();
	private MyQuadVisitor qv = new MyQuadVisitor();
	private ICSCG cscg;
	// TODO: E -> CE in each of 3 below sets?
	// set of heap insts deemed possibly escaping by
	// whole-program analysis
	private Set<Quad> escHeapInsts = new HashSet<Quad>();
	// set of heap insts proven thread local by whole-program analysis
	private Set<Quad> locHeapInsts = new HashSet<Quad>();
	private Quad currHeapInst;
	private Set<Quad> currAllocs;
	private jq_Method threadStartMethod;
	private jq_Method mainMethod;
	
	public void run() {
		ThreadEscapePathAnalysis analysis =
			(ThreadEscapePathAnalysis) Project.getTrgt("thresc-path-java");
		Project.runTask(analysis);
		Map<Quad, Set<Quad>> heapInstToAllocs = analysis.getHeapInstToAllocsMap();
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
		int numV = domV.size();
		int vIdx = 0;
		while (vIdx < numV) {
			Register v = domV.get(vIdx);
			jq_Method m = domV.getMethod(v);
			assert (!methToNumVars.containsKey(m));
			int n = m.getNumVarsOfRefType();
			methToNumVars.put(m, n);
			methToVar0Idx.put(m, vIdx);
			if (DEBUG)
				System.out.println(m + " numVars: " + n + " var0Idx: " + vIdx);
			vIdx += n;
		}

		ThrSenAbbrCSCGAnalysis cscgAnalysis =
			(ThrSenAbbrCSCGAnalysis) Project.getTrgt(
				"thrsen-abbr-cscg-java");
		Project.runTask(cscgAnalysis);
		cscg = cscgAnalysis.getCallGraph();
		Set<Pair<Ctxt, jq_Method>> roots = cscg.getRoots();

		for (Map.Entry<Quad, Set<Quad>> e : heapInstToAllocs.entrySet()) {
			currHeapInst = e.getKey();
			currAllocs = e.getValue();
			jq_Method m = Program.v().getMethod(currHeapInst);
			String c = m.getDeclaringClass().getName();
			if (c.startsWith("java.") || c.startsWith("sun.") ||
				c.startsWith("com.") || c.startsWith("org."))
				continue;
			System.out.println("currHeapInst: " +
				Program.v().toVerboseStr(currHeapInst));
			for (Quad h : currAllocs)
				System.out.println("\t" + Program.v().toVerboseStr(h));
			Timer timer = new Timer("hybrid-thresc-timer");
			timer.init();
			invkTimer = new Timer("invk-timer");
			invkTimer.init();
			invkTimer.pause();
			try {
				for (Pair<Ctxt, jq_Method> root : roots) {
					processThread(root);
				}
				System.out.println("XXX LOC");
				locHeapInsts.add(currHeapInst);
			} catch (ThrEscException ex) {
				System.out.println("XXX ESC");
				escHeapInsts.add(currHeapInst);
			}
			invkTimer.resume();
			invkTimer.done();
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
			System.out.println(invkTimer.getExclusiveTimeStr());
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
	Timer invkTimer;

	private void processThread(Pair<Ctxt, jq_Method> root) {
		System.out.println("PROCESSING THREAD: " + root);
		init(root);
		while (!workList.isEmpty()) {
			Pair<Pair<Ctxt, jq_Method>, PathEdge> pair =
				workList.remove(workList.size() - 1);
			Pair<Ctxt, jq_Method> cm = pair.val0;
			PathEdge pe = pair.val1;
			Quad q = pe.q;
			if (DEBUG)
				System.out.println("Processing path edge: cm: " + cm + " pe: " + pe);
			if (q == null) {
				BasicBlock bb = pe.bb;
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
						PathEdge edge2 = new PathEdge(q2, q2Idx, bb2, pe.sd);
						addPathEdge(cm, edge2);
					}
				} else {
					assert (bb.isExit()); 
					processReturn(cm, pe);
				}
			} else {
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					processInvoke(cm, pe);
				} else if (op instanceof Return) {
					processReturn(cm, pe);
				} else {
					SD sd = pe.sd;
					DstNode dstNode = sd.dstNode;
					qv.iDstNode = dstNode;
					qv.m = cm.val1;
					qv.oDstNode = dstNode;
					q.accept(qv);
					DstNode dstNode2 = qv.oDstNode;
					SD sd2 = (dstNode2 == dstNode) ? sd :
						new SD(sd.srcNode, dstNode2);
					propagateToSucc(cm, pe.qIdx, pe.bb, sd2);
				}
			}
		}
	}
	private void propagateToSucc(Pair<Ctxt, jq_Method> cm,
			int qIdx, BasicBlock bb, SD sd) {
		if (qIdx != bb.size() - 1) {
			int q2Idx = qIdx + 1;
			Quad q2 = bb.getQuad(q2Idx);
			PathEdge edge2 = new PathEdge(q2, q2Idx, bb, sd);
			addPathEdge(cm, edge2);
			return;
		}
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
			PathEdge edge2 = new PathEdge(q2, q2Idx, bb2, sd);
			addPathEdge(cm, edge2);
		}
	}
	private void addPathEdge(Pair<Ctxt, jq_Method> cm, PathEdge pe) {
		if (DEBUG)
			System.out.println("\tAdding path edge: cm: " + cm + " pe: " + pe);
		Set<PathEdge> peSet = pathEdges.get(cm);
		if (peSet == null) {
			peSet = new HashSet<PathEdge>();
			pathEdges.put(cm, peSet);
		}
		if (peSet.add(pe)) {
			Pair<Pair<Ctxt, jq_Method>, PathEdge> pair =
				new Pair<Pair<Ctxt, jq_Method>, PathEdge>(cm, pe);
			workList.add(pair);
		}
	}
	private void init(Pair<Ctxt, jq_Method> root) {
		pathEdges.clear();
		summEdges.clear();
		// clear worklist: may not be empty due to thrown exception
		workList.clear();
		jq_Method meth = root.val1;
		int n = methToNumVars.get(meth);
		IntArraySet[] env = new IntArraySet[n];
		for (int i = 0; i < n; i++)
			env[i] = nilPts;
		if (meth == threadStartMethod) {
			// arg of start method of java.lang.Thread escapes
			env[0] = escPts;
		}
		BasicBlock bb = meth.getCFG().entry();
		SrcNode srcNode = new SrcNode(env, emptyHeap);
		DstNode dstNode = new DstNode(env, emptyHeap, nilPts);
		SD sd = new SD(srcNode, dstNode);
		PathEdge edge = new PathEdge(null, -1, bb, sd);
		addPathEdge(root, edge);
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
	private void processInvoke(Pair<Ctxt, jq_Method> cm, PathEdge pe) {
		invkTimer.resume();
		Quad q = pe.q;
		MethodOperand mo = Invoke.getMethod(q);
		mo.resolve();
		jq_Method tgt = mo.getMethod();
		if (tgt == threadStartMethod) {
			if (DEBUG) System.out.println("Target is thread start method");
			SD sd = pe.sd;
			DstNode dstNode = sd.dstNode;
			IntArraySet[] dstEnv = dstNode.env;
			RegisterOperand ao = Invoke.getParam(q, 0);
			int aIdx = getIdx(ao, cm.val1);
			IntArraySet aPts = dstEnv[aIdx];
			DstNode dstNode2 = (aPts == escPts || aPts == nilPts) ? dstNode :
				propagateEsc(aPts, dstNode);
			SD sd2 = (dstNode == dstNode2) ? sd :
				new SD(sd.srcNode, dstNode2);
			propagateToSucc(cm, pe.qIdx, pe.bb, sd2);
			invkTimer.pause();
			return;
		}
		SD sd = pe.sd;
		DstNode dstNode = sd.dstNode;
		IntArraySet[] dstEnv = dstNode.env;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        jq_Method m = cm.val1;
		for (Pair<Ctxt, jq_Method> cm2 : cscg.getTargets(cm.val0, q)) {
			if (DEBUG) System.out.println("Target: " + cm2);
			Set<SummEdge> seSet = summEdges.get(cm2);
			boolean found = false;
			if (seSet != null) {
				for (SummEdge se : seSet) {
					if (DEBUG) System.out.println("Testing summary edge: " + se);
					if (propagateSEtoPE(pe, cm, se)) {
						if (DEBUG) System.out.println("Match found");
						found = true;
					}
				}
			}
			if (!found) {
				if (DEBUG) System.out.println("No match found");
				jq_Method m2 = cm2.val1;
		        int numVars = methToNumVars.get(m2);
		        IntArraySet[] env = new IntArraySet[numVars];
		        int mIdx = 0;
		        for (int i = 0; i < numArgs; i++) {
		            RegisterOperand ao = args.get(i);
		            if (ao.getType().isReferenceType()) {
		                int aIdx = getIdx(ao, m);
		                IntArraySet pts = dstEnv[aIdx];
		                env[mIdx++] = pts;
					}
		        }
		        while (mIdx < numVars)
		        	env[mIdx++] = nilPts;
				SrcNode srcNode2 = new SrcNode(env, dstNode.heap);
				DstNode dstNode2 = new DstNode(env, dstNode.heap, nilPts);
				SD sd2 = new SD(srcNode2, dstNode2);
				BasicBlock bb2 = m2.getCFG().entry();
				PathEdge pe2 = new PathEdge(null, -1, bb2, sd2);
				addPathEdge(cm2, pe2);
			}
		}
		invkTimer.pause();
	}
	private void processReturn(Pair<Ctxt, jq_Method> cm, PathEdge pe) {
		SD sd = pe.sd;
		DstNode dstNode = sd.dstNode;
		IntArraySet rPts = nilPts;
		Quad q = pe.q;
		// q may be null in which case pe.bb is exit basic block
		if (q != null) {
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				if (ro.getType().isReferenceType()) {
					int rIdx = getIdx(ro, cm.val1);
					rPts = dstNode.env[rIdx];
				}
			}
		}
		RetNode retNode = new RetNode(rPts, dstNode.heap, dstNode.esc);
		SummEdge se = new SummEdge(sd.srcNode, retNode);
		Set<SummEdge> seSet = summEdges.get(cm);
		if (DEBUG) System.out.println("Testing summary edge: " + se);
		if (seSet == null) {
			seSet = new HashSet<SummEdge>();
			summEdges.put(cm, seSet);
		}
		if (!seSet.add(se)) {
			// summary edge se already exists
			if (DEBUG) System.out.println("Already exists");
			return;
		}
		boolean flag = false;
		if (DEBUG) System.out.println("Not found; adding");
		for (Pair<Ctxt, Quad> ci : cscg.getCallers(cm.val0, cm.val1)) {
			if (DEBUG) System.out.println("Caller: " + ci);
			Ctxt c2 = ci.val0;
			Quad q2 = ci.val1;
			jq_Method m2 = Program.v().getMethod(q2);
			Pair<Ctxt, jq_Method> cm2 = new Pair<Ctxt, jq_Method>(c2, m2);
			Set<PathEdge> peSet = pathEdges.get(cm2);
			if (peSet == null)
				continue;
			peSet = new ArraySet<PathEdge>(peSet);
			for (PathEdge pe2 : peSet) {
				if (DEBUG) System.out.println("Testing path edge: " + pe2);
				boolean match = false;
				if (pe2.q == q2)
					match = propagateSEtoPE(pe2, cm2, se);
				if (match) {
					flag = true;
					if (DEBUG) System.out.println("Match found");
				} else
					if (DEBUG) System.out.println("No match found");
			}
		}
		jq_Method m = cm.val1;
		if (m != mainMethod && m != threadStartMethod)
			assert flag;
	}
	private boolean propagateSEtoPE(PathEdge clrPE,
			Pair<Ctxt, jq_Method> clrCM, SummEdge tgtSE) {
		SD sd = clrPE.sd;
		DstNode clrDstNode = sd.dstNode;
		SrcNode tgtSrcNode = tgtSE.srcNode;
		if (!clrDstNode.heap.equals(tgtSrcNode.heap))
			return false;
		IntArraySet[] clrDstEnv = clrDstNode.env;
		IntArraySet[] tgtSrcEnv = tgtSrcNode.env;
		Quad q = clrPE.q;
		int qIdx = clrPE.qIdx;
		jq_Method clr = clrCM.val1;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        for (int i = 0, fIdx = 0; i < numArgs; i++) {
            RegisterOperand ao = args.get(i);
            if (ao.getType().isReferenceType()) {
                int aIdx = getIdx(ao, clr);
                IntArraySet aPts = clrDstEnv[aIdx];
                IntArraySet fPts = tgtSrcEnv[fIdx];
                if (!CompareUtils.areEqual(aPts, fPts))
                	return false;
                fIdx++;
			}
		}
		RetNode tgtRetNode = tgtSE.retNode;
        BasicBlock bb = clrPE.bb;
        int n = clrDstEnv.length;
        IntArraySet[] clrDstEnv2 = new IntArraySet[n];
        RegisterOperand ro = Invoke.getDest(q);
        int rIdx = -1;
        if (ro != null && ro.getType().isReferenceType()) {
        	rIdx = getIdx(ro, clr);
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
			// MAYUR System.out.println("BBB esc size: " + clrDstEsc2.size());
		}
        DstNode clrDstNode2 = new DstNode(clrDstEnv2,
        	tgtRetNode.heap, clrDstEsc2);
        SD sd2 = new SD(sd.srcNode, clrDstNode2);
        propagateToSucc(clrCM, qIdx, bb, sd2);
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
		jq_Method m;
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}
		public void visitMove(Quad q) {
	        RegisterOperand lo = Move.getDest(q);
			jq_Type t = lo.getType();
	        if (!t.isReferenceType())
	        	return;
	        IntArraySet[] dstEnv = iDstNode.env;
			int lIdx = getIdx(lo, m);
			IntArraySet lPts = dstEnv[lIdx];
			Operand rx = Move.getSrc(q);
			IntArraySet lPts2;
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				int rIdx = getIdx(ro, m);
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
			assert (t != null);
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
					int rIdx = getIdx(ro, m);
					IntArraySet rPts = dstEnv[rIdx];
					if (rPts == escPts) {
						pPts = escPts;
						break;
					}
					pPts.addAll(rPts);
				}
			}
			int lIdx = getIdx(lo, m);
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
			if (q == currHeapInst)
				check(q, ALoad.getBase(q));
			Operator op = q.getOperator();
			if (!((ALoad) op).getType().isReferenceType())
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			Set<IntTrio> dstHeap = iDstNode.heap;
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			int bIdx = getIdx(bo, m);
			IntArraySet bPts = dstEnv[bIdx];
			RegisterOperand lo = ALoad.getDest(q);
			int lIdx = getIdx(lo, m);
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
			if (q == currHeapInst)
				check(q, Getfield.getBase(q));
			FieldOperand fo = Getfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType())
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			Set<IntTrio> dstHeap = iDstNode.heap;
			RegisterOperand lo = Getfield.getDest(q);
			int lIdx = getIdx(lo, m);
			IntArraySet lPts = dstEnv[lIdx];
			Operand bx = Getfield.getBase(q);
			IntArraySet lPts2;
			if (bx instanceof RegisterOperand) {
				RegisterOperand bo = (RegisterOperand) bx;
				int bIdx = getIdx(bo, m);
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
			if (q == currHeapInst)
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
			int rIdx = getIdx(ro, m);
			IntArraySet rPts = dstEnv[rIdx];
			if (rPts == nilPts)
				return;
			int bIdx = getIdx(bo, m);
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
				// MAYUR System.out.println("YYY heap size: " + dstHeap2.size());
				oDstNode = new DstNode(dstEnv, dstHeap2, iDstNode.esc);
				return;
			}
			if (rPts == escPts)
				return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitPutfield(Quad q) {
			if (q == currHeapInst)
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
			int rIdx = getIdx(ro, m);
			IntArraySet rPts = dstEnv[rIdx];
			if (rPts == nilPts)
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo, m);
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
				// MAYUR System.out.println("ZZZ heap size: " + dstHeap2.size());
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
	        int rIdx = getIdx(ro, m);
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
	        int lIdx = getIdx(lo, m);
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
			int vIdx = getIdx(vo, m);
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
			int bIdx = getIdx(bo, m);
			IntArraySet pts = iDstNode.env[bIdx];
			if (pts == escPts)
				throw new ThrEscException();
		}
	}

	/*****************************************************************
	 * Frequently used functions
     *****************************************************************/

	private int getIdx(RegisterOperand ro, jq_Method m) {
		Register r = ro.getRegister();
		int vIdx = domV.indexOf(r);
		return vIdx - methToVar0Idx.get(m);
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
			String x = toString(e);
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
