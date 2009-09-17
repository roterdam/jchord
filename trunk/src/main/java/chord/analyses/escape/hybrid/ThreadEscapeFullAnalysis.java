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

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;
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
	public final static IntArraySet emptyEsc = new IntArraySet(0);
	public final static IntArraySet escPts = null;
	public final static IntArraySet nilPts = new IntArraySet(0);
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
			// if (!m.getDeclaringClass().getName().startsWith("test"))
			//	continue;
			System.out.println("currHeapInst: " +
				Program.v().toStringHeapInst(currHeapInst) +
				" m: " + m);
			for (Quad h : currAllocs)
				System.out.println("\t" + Program.v().toStringNewInst(h));
			Timer timer = new Timer("hybrid-thresc-timer");
			timer.init();
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
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
		}
		System.out.println("Full analysis escHeapInsts:");
		for (Quad e : escHeapInsts)
			System.out.println("\t" + Program.v().toString(e));
		System.out.println("Full analysis locHeapInsts:");
		for (Quad e : locHeapInsts)
			System.out.println("\t" + Program.v().toString(e));
	}

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
				assert (bb.isEntry());
				for (Object o : bb.getSuccessors()) {
					BasicBlock bb2 = (BasicBlock) o;
					Quad q2 = (bb2.size() == 0) ? null : bb2.getQuad(0);
					PathEdge edge2 = new PathEdge(q2, bb2, pe.sd);
					addPathEdge(cm, edge2);
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
					propagateToSucc(cm, q, pe.bb, sd2);
				}
			}
		}
	}
	private void propagateToSucc(Pair<Ctxt, jq_Method> cm,
			Quad q, BasicBlock bb, SD sd) {
		if (q != bb.getLastQuad()) {
			Quad q2 = bb.getQuad(bb.getQuadIndex(q) + 1);
			PathEdge edge2 = new PathEdge(q2, bb, sd);
			addPathEdge(cm, edge2);
			return;
		}
		for (Object o : bb.getSuccessors()) {
			BasicBlock bb2 = (BasicBlock) o;
			Quad q2 = (bb2.size() == 0) ? null : bb2.getQuad(0);
			PathEdge edge2 = new PathEdge(q2, bb2, sd);
			addPathEdge(cm, edge2);
		}
	}
	private int getIdx(RegisterOperand ro, jq_Method m) {
		Register r = ro.getRegister();
		int vIdx = domV.indexOf(r);
		return vIdx - methToVar0Idx.get(m);
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
		DstNode dstNode = new DstNode(env, emptyHeap, emptyEsc);
		SD sd = new SD(srcNode, dstNode);
		PathEdge edge = new PathEdge(null, bb, sd);
		addPathEdge(root, edge);
	}
	private IntArraySet getPtsFromHeap(IntArraySet pts, int fIdx,
			Set<IntTrio> heap) {
		assert (pts != escPts);
		IntArraySet pts2 = null;
		for (IntTrio t : heap) {
			if (t.idx1 != fIdx)
				continue;
			int h1Idx = t.idx0;
			if (pts.contains(h1Idx)) {
				int h2Idx = t.idx2;
				if (h2Idx == 0)
					return escPts;
				if (pts2 == null) {
					pts2 = new IntArraySet();
					pts2.add(h2Idx);
				}
			}
		}
		if (pts2 == null)
			return nilPts;
		return pts2;
	}
	private void processInvoke(Pair<Ctxt, jq_Method> cm, PathEdge pe) {
		Quad q = pe.q;
		jq_Method tgt = Invoke.getMethod(q).getMethod();
		if (tgt == threadStartMethod) {
			if (DEBUG) System.out.println("Target is thread start method");
			SD sd = pe.sd;
			DstNode dstNode = sd.dstNode;
			IntArraySet[] dstEnv = dstNode.env;
			RegisterOperand ao = Invoke.getParam(q, 0);
			int aIdx = getIdx(ao, cm.val1);
			IntArraySet aPts = dstEnv[aIdx];
			DstNode dstNode2 = (aPts == escPts) ? dstNode :
				propagateEsc(aPts, dstNode);
			SD sd2 = (dstNode == dstNode2) ? sd :
				new SD(sd.srcNode, dstNode2);
			propagateToSucc(cm, q, pe.bb, sd2);
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
				DstNode dstNode2 = new DstNode(env, dstNode.heap, emptyEsc);
				SD sd2 = new SD(srcNode2, dstNode2);
				BasicBlock bb2 = m2.getCFG().entry();
				PathEdge pe2 = new PathEdge(null, bb2, sd2);
				addPathEdge(cm2, pe2);
			}
		}
	}
	private void processReturn(Pair<Ctxt, jq_Method> cm, PathEdge pe) {
		SD sd = pe.sd;
		DstNode dstNode = sd.dstNode;
		IntArraySet rPts = nilPts;
		Operand rx = Return.getSrc(pe.q);
		if (rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			if (ro.getType().isReferenceType()) {
				int rIdx = getIdx(ro, cm.val1);
				rPts = dstNode.env[rIdx];
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
        IntArraySet clrDstEsc2 = new IntArraySet(clrDstEsc);
        clrDstEsc2.addAll(tgtRetEsc);
        DstNode clrDstNode2 = new DstNode(clrDstEnv2,
        	tgtRetNode.heap, clrDstEsc2);
        SD sd2 = new SD(sd.srcNode, clrDstNode2);
        propagateToSucc(clrCM, q, bb, sd2);
        return true;
	}
	private DstNode propagateEsc(IntArraySet pts, DstNode iDstNode) {
		assert (pts != escPts);
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
		IntArraySet[] dstEnv2 = new IntArraySet[n];
		for (int i = 0; i < n; i++) {
			IntArraySet pts2 = dstEnv[i];
			if (pts2 == escPts || dstEsc2.overlaps(pts2))
				dstEnv2[i] = escPts;
			else
				dstEnv2[i] = pts2;
		}
		Set<IntTrio> dstHeap2 = new ArraySet<IntTrio>();
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
		dstEsc2.addAll(dstEsc);
		return new DstNode(dstEnv2, dstHeap2, dstEsc2);
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		DstNode iDstNode;
		DstNode oDstNode;
		jq_Method m;
		public void visitMove(Quad q) {
	        RegisterOperand lo = Move.getDest(q);
	        if (!lo.getType().isReferenceType())
	        	return;
	        IntArraySet[] dstEnv = iDstNode.env;
			Operand rx = Move.getSrc(q);
			IntArraySet rPts;
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				int rIdx = getIdx(ro, m);
				rPts = dstEnv[rIdx];
			} else
				rPts = nilPts;
			int lIdx = getIdx(lo, m);
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = rPts;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap,
				iDstNode.esc);
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
			IntArraySet rPts;
			if (bPts == escPts)
				rPts = escPts;
			else {
				int fIdx = 0;
				rPts = getPtsFromHeap(bPts, fIdx, dstHeap);
			}
			RegisterOperand lo = ALoad.getDest(q);
			int lIdx = getIdx(lo, m);
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = rPts;
			oDstNode = new DstNode(dstEnv2, dstHeap, iDstNode.esc);
		}
		public void visitGetfield(Quad q) {
			if (q == currHeapInst)
				check(q, Getfield.getBase(q));
			jq_Field f = Getfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			IntArraySet[] dstEnv = iDstNode.env;
			Set<IntTrio> dstHeap = iDstNode.heap;
			IntArraySet rPts;
			Operand bx = Getfield.getBase(q);
			if (bx instanceof RegisterOperand) {
				RegisterOperand bo = (RegisterOperand) bx;
				int bIdx = getIdx(bo, m);
				IntArraySet bPts = dstEnv[bIdx];
				if (bPts == escPts)
					rPts = escPts;
				else {
					int fIdx = domF.indexOf(f);
					rPts = getPtsFromHeap(bPts, fIdx, dstHeap);
				}
			} else
				rPts = nilPts;
			RegisterOperand lo = Getfield.getDest(q);
			int lIdx = getIdx(lo, m);
			IntArraySet[] dstEnv2 = copy(dstEnv);
			dstEnv2[lIdx] = rPts;
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
			int bIdx = getIdx(bo, m);
			int rIdx = getIdx(ro, m);
			IntArraySet[] dstEnv = iDstNode.env;
			IntArraySet bPts = dstEnv[bIdx];
			IntArraySet rPts = dstEnv[rIdx];
			int fIdx = 0;
			if (bPts != escPts) {
				Set<IntTrio> dstHeap = iDstNode.heap;
				Set<IntTrio> dstHeap2 = new ArraySet<IntTrio>(dstHeap);
				int nb = bPts.size();
				if (rPts == escPts) {
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						dstHeap2.add(new IntTrio(hIdx, fIdx, 0));
					}
				} else {
					int nr = rPts.size();
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						for (int j = 0; j < nr; j++) {
							int hIdx2 = rPts.get(j);
							dstHeap2.add(new IntTrio(hIdx, fIdx, hIdx2));
						}
					}
				}
				IntArraySet dstEsc = iDstNode.esc;
				oDstNode = new DstNode(dstEnv, dstHeap2, dstEsc);
				return;
			}
			if (rPts == escPts)
				return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitPutfield(Quad q) {
			if (q == currHeapInst)
				check(q, Putfield.getBase(q));
			jq_Field f = Putfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			Operand rx = Putfield.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			Operand bx = Putfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			RegisterOperand ro = (RegisterOperand) rx;
			int bIdx = getIdx(bo, m);
			int rIdx = getIdx(ro, m);
			IntArraySet[] dstEnv = iDstNode.env;
			IntArraySet bPts = dstEnv[bIdx];
			IntArraySet rPts = dstEnv[rIdx];
			int fIdx = domF.indexOf(f);
			if (bPts != escPts) {
				Set<IntTrio> dstHeap = iDstNode.heap;
				Set<IntTrio> dstHeap2 = new ArraySet<IntTrio>(dstHeap);
				int nb = bPts.size();
				if (rPts == escPts) {
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						dstHeap2.add(new IntTrio(hIdx, fIdx, 0));
					}
				} else {
					int nr = rPts.size();
					for (int i = 0; i < nb; i++) {
						int hIdx = bPts.get(i);
						for (int j = 0; j < nr; j++) {
							int hIdx2 = rPts.get(j);
							dstHeap2.add(new IntTrio(hIdx, fIdx, hIdx2));
						}
					}
				}
				oDstNode = new DstNode(dstEnv, dstHeap2, iDstNode.esc);
				return;
			}
			if (rPts == escPts)
				return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitPutstatic(Quad q) {
			jq_Field f = Putstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
	        Operand rx = Putstatic.getSrc(q);
	        if (!(rx instanceof RegisterOperand))
	        	return;
			IntArraySet[] dstEnv = iDstNode.env;
	        RegisterOperand ro = (RegisterOperand) rx;
	        int rIdx = getIdx(ro, m);
	        IntArraySet rPts = dstEnv[rIdx];
	        if (rPts == escPts)
	        	return;
			oDstNode = propagateEsc(rPts, iDstNode);
		}
		public void visitGetstatic(Quad q) {
			jq_Field f = Getstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
			IntArraySet[] dstEnv = iDstNode.env;
	        IntArraySet[] dstEnv2 = copy(dstEnv);
	        RegisterOperand lo = Getstatic.getDest(q);
	        int lIdx = getIdx(lo, m);
	        dstEnv2[lIdx] = escPts;
			oDstNode = new DstNode(dstEnv2, iDstNode.heap,
				iDstNode.esc);
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
			IntArraySet[] dstEnv2 = copy(dstEnv);
			if (currAllocs.contains(q)) {
				int hIdx = domH.indexOf(q);
				IntArraySet pts = new IntArraySet(1);
				pts.add(hIdx);
				dstEnv2[vIdx] = pts;
			} else {
				dstEnv2[vIdx] = escPts;
			}
			oDstNode = new DstNode(dstEnv2, iDstNode.heap,
				iDstNode.esc);
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
	private static IntArraySet[] copy(IntArraySet[] a) {
		int n = a.length;
		IntArraySet[] b = new IntArraySet[n];
        for (int i = 0; i < n; i++)
        	b[i] = a[i];
        return b;
	}
}
