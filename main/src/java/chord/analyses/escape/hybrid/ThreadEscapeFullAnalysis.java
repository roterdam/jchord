/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import gnu.trove.TObjectIntHashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.alias.ICICG;
import chord.analyses.alias.ThrOblAbbrCICGAnalysis;
import chord.util.tuple.object.Pair;
import chord.util.tuple.integer.IntPair;
import chord.program.Program;
import chord.program.Location;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.ForwardRHSAnalysis;
import chord.bddbddb.Rel.IntPairIterable;
import chord.project.Properties;
import chord.util.ChordRuntimeException;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.Project;
import chord.util.ArraySet;
import chord.util.CompareUtils;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;
import chord.util.Timer;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	    name = "thresc-full-java"
	)
public class ThreadEscapeFullAnalysis extends ForwardRHSAnalysis<Edge, Edge> {
	public static final IntArraySet nilPts = new IntArraySet(0);
	private final Set<IntTrio> emptyHeap = Collections.emptySet();
	private int ESC_VAL;
	private final IntArraySet escPts = new IntArraySet(1);
	private final IntArraySet tmpPts = new IntArraySet();
	private final IntArraySet[] emptyRetEnv = new IntArraySet[] { nilPts };
	private final Set<Edge> tmpEdgeSet = new ArraySet<Edge>(1);
	private final Set<Edge> emptyEdgeSet = Collections.emptySet();
	private DomM domM;
	private DomI domI;
	private DomV domV;
	private DomF domF;
	private DomH domH;
	private DomE domE;
	private int varId[];
	private TObjectIntHashMap<jq_Method> methToNumVars =
		new TObjectIntHashMap<jq_Method>();
	private ICICG cicg;

	// TODO: E -> CE in each of 3 below sets?
	// set of heap insts deemed possibly escaping by
	// whole-program analysis
	private Set<Quad> escHeapInsts = new HashSet<Quad>();
	// set of heap insts proven thread local by whole-program analysis
	private Set<Quad> locHeapInsts = new HashSet<Quad>();
	private Set<Quad> currAllocs;
	private Set<Quad> currLocHeapInsts = new HashSet<Quad>();
	private Set<Quad> currEscHeapInsts = new HashSet<Quad>();
    private MyQuadVisitor qv = new MyQuadVisitor();
	private jq_Method mainMethod;
	private jq_Method threadStartMethod;

	@Override
	public void run() {
		Program program = Program.getProgram();
		mainMethod = program.getMainMethod();
		threadStartMethod = program.getThreadStartMethod();
        domI = (DomI) Project.getTrgt("I");
        Project.runTask(domI);
        domM = (DomM) Project.getTrgt("M");
        Project.runTask(domM);
		domV = (DomV) Project.getTrgt("V");
		Project.runTask(domV);
		domF = (DomF) Project.getTrgt("F");
		Project.runTask(domF);
		domH = (DomH) Project.getTrgt("H");
		Project.runTask(domH);
		domE = (DomE) Project.getTrgt("E");
		Project.runTask(domE);
		int numH = domH.size();
		// todo: change this to 0
		ESC_VAL = numH;
		escPts.add(ESC_VAL);
		escPts.setReadOnly();

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

		init();

		for (Map.Entry<Set<Quad>, Set<Quad>> e :
				allocInstsToHeapInsts.entrySet()) {
			currAllocs = e.getKey();
			currLocHeapInsts = e.getValue();
			currEscHeapInsts.clear();
			System.out.println("**************");
			System.out.println("currHeapInsts:");
			for (Quad q : currLocHeapInsts) {
				int x = domE.indexOf(q);
				System.out.println("\t" + program.toVerboseStr(q) + " " + x);
			}
			System.out.println("currAllocInsts:");
			for (Quad q : currAllocs)
				System.out.println("\t" + program.toVerboseStr(q));
			Timer timer = new Timer("hybrid-thresc-timer");
			timer.init();
			try {
				runPass();
			} catch (ThrEscException ex) {
				// do nothing
			}
			for (Quad q : currLocHeapInsts)
				System.out.println("LOC: " + program.toVerboseStr(q));
			for (Quad q : currEscHeapInsts)
				System.out.println("ESC: " + program.toVerboseStr(q));
			locHeapInsts.addAll(currLocHeapInsts);
			escHeapInsts.addAll(currEscHeapInsts);
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
		}
		try {
			String outDirName = Properties.outDirName;
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullEscE.txt")));
				for (Quad e : escHeapInsts)
					writer.println(program.toPosStr(e));
				writer.close();
			}
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullLocE.txt")));
				for (Quad e : locHeapInsts)
					writer.println(program.toPosStr(e));
				writer.close();
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	@Override
	public boolean doMerge() {
		return true;
	}

	@Override
	public ICICG getCallGraph() {
		if (cicg == null) {
        	ThrOblAbbrCICGAnalysis cicgAnalysis =
				(ThrOblAbbrCICGAnalysis) Project.getTrgt("throbl-abbr-cicg-java");
			Project.runTask(cicgAnalysis);
			cicg = cicgAnalysis.getCallGraph();
		}
		return cicg;
	}

	// m is either the main method or the thread root method
	private Edge getRootPathEdge(jq_Method m) {
		assert (m == mainMethod || m == threadStartMethod);
		int n = methToNumVars.get(m);
		IntArraySet[] env = new IntArraySet[n];
		for (int i = 0; i < n; i++)
			env[i] = nilPts;
		if (m == threadStartMethod) {
			// arg of start method of java.lang.Thread escapes
			env[0] = escPts;
		}
		SrcNode srcNode = new SrcNode(env, emptyHeap);
		DstNode dstNode = new DstNode(env, emptyHeap, nilPts, false);
		Edge pe = new Edge(srcNode, dstNode);
		return pe;
	}

	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		Set<Pair<Location, Edge>> initPEs =
			new ArraySet<Pair<Location, Edge>>(1);
		Edge pe = getRootPathEdge(mainMethod);
       	BasicBlock bb = mainMethod.getCFG().entry();
		Location loc = new Location(mainMethod, bb, -1, null);
		Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
		initPEs.add(pair);
		return initPEs;
	}

	@Override
	public Set<Edge> getInitPathEdges(Quad q, jq_Method m2, Edge pe) {
		Edge pe2;
		if (m2 == threadStartMethod) {
			// ignore pe
			pe2 = getRootPathEdge(m2);
		} else {
			DstNode dstNode = pe.dstNode;
			IntArraySet[] dstEnv = dstNode.env;
			ParamListOperand args = Invoke.getParamList(q);
			int numArgs = args.length();
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
			DstNode dstNode2 = new DstNode(env, dstNode.heap, nilPts, false);
			pe2 = new Edge(srcNode2, dstNode2);
		}
		tmpEdgeSet.clear();
		tmpEdgeSet.add(pe2);
		return tmpEdgeSet;
	}

	@Override
	public Set<Edge> getMiscPathEdges(Quad q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		qv.iDstNode = dstNode;
		qv.oDstNode = dstNode;
		q.accept(qv);
		DstNode dstNode2 = qv.oDstNode;
		Edge pe2 = new Edge(pe.srcNode, dstNode2);
		tmpEdgeSet.clear();
		tmpEdgeSet.add(pe2);
		return tmpEdgeSet;
	}

	@Override
	public Set<Edge> getInvkPathEdges(Quad q, Edge pe) {
		return emptyEdgeSet;
	}

	private Edge getForkPathEdge(Quad q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		IntArraySet[] iEnv = dstNode.env;
		RegisterOperand ao = Invoke.getParam(q, 0);
		int aIdx = getIdx(ao);
		IntArraySet aPts = iEnv[aIdx];
		DstNode dstNode2;
		if (aPts == escPts || aPts == nilPts)
			dstNode2 = dstNode;
		else {
			Set<IntTrio> iHeap = dstNode.heap;
			IntArraySet iEsc = dstNode.esc;
			IntArraySet oEsc = propagateEsc(aPts, iHeap, iEsc);
			if (oEsc == iEsc)
				dstNode2 = dstNode;
			else {
				IntArraySet[] oEnv = updateEnv(iEnv, oEsc);
				Set<IntTrio> oHeap = updateHeap(iHeap, oEsc);
				dstNode2 = new DstNode(oEnv, oHeap, oEsc, false);
			}
		}
		Edge pe2 = // (dstNode == dstNode2) ? pe :
			new Edge(pe.srcNode, dstNode2);
		return pe2;
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		Edge se;
		DstNode dstNode = pe.dstNode;
		if (dstNode.isRet)
			se = new Edge(pe.srcNode, dstNode);
		else {
			dstNode = new DstNode(emptyRetEnv, dstNode.heap,
				dstNode.esc, true);
			se = new Edge(pe.srcNode, dstNode);
		}
		return se;
	}

	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		if (m == threadStartMethod) {
			// ignore tgtSE
			return getForkPathEdge(q, clrPE);
		}
		DstNode clrDstNode = clrPE.dstNode;
		SrcNode tgtSrcNode = tgtSE.srcNode;
		if (!clrDstNode.heap.equals(tgtSrcNode.heap))
			return null;
		IntArraySet[] clrDstEnv = clrDstNode.env;
		IntArraySet[] tgtSrcEnv = tgtSrcNode.env;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        for (int i = 0, fIdx = 0; i < numArgs; i++) {
            RegisterOperand ao = args.get(i);
            if (ao.getType().isReferenceType()) {
                int aIdx = getIdx(ao);
                IntArraySet aPts = clrDstEnv[aIdx];
                IntArraySet fPts = tgtSrcEnv[fIdx];
                if (!CompareUtils.areEqual(aPts, fPts))
                	return null;
                fIdx++;
			}
		}
		DstNode tgtRetNode = tgtSE.dstNode;
        int n = clrDstEnv.length;
        IntArraySet[] clrDstEnv2 = new IntArraySet[n];
        RegisterOperand ro = Invoke.getDest(q);
        int rIdx = -1;
        if (ro != null && ro.getType().isReferenceType()) {
        	rIdx = getIdx(ro);
        	clrDstEnv2[rIdx] = tgtRetNode.env[0];
        }
		IntArraySet tgtRetEsc = tgtRetNode.esc;
        for (int i = 0; i < n; i++) {
        	if (i == rIdx)
        		continue;
        	IntArraySet pts = clrDstEnv[i];
        	if (pts != nilPts && pts != escPts && pts.overlaps(tgtRetEsc)) {
				tmpPts.clear();
				int k = pts.size();
				for (int j = 0; j < k; j++) {
					int x = pts.get(j);
					if (x != ESC_VAL && !tgtRetEsc.contains(x))
						tmpPts.addForcibly(x);
				}
				if (tmpPts.isEmpty())
					pts = escPts;
				else {
					tmpPts.addForcibly(ESC_VAL);
					pts = new IntArraySet(tmpPts);
				}
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
        DstNode clrDstNode2 = new DstNode(clrDstEnv2, tgtRetNode.heap,
			clrDstEsc2, false);
        Edge pe2 = new Edge(clrPE.srcNode, clrDstNode2);
		return pe2;
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		DstNode iDstNode;
		DstNode oDstNode;
		@Override
		public void visitReturn(Quad q) {
			IntArraySet[] oEnv = emptyRetEnv;
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				if (ro.getType().isReferenceType()) {
					int rIdx = getIdx(ro);
					IntArraySet rPts = iDstNode.env[rIdx];
					oEnv = new IntArraySet[1];
					oEnv[0] = rPts;
				}
			}
			Set<IntTrio> oHeap = iDstNode.heap;
			IntArraySet oEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, oHeap, oEsc, true);
		}
		@Override
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}
		@Override
		public void visitMove(Quad q) {
	        RegisterOperand lo = Move.getDest(q);
			jq_Type t = lo.getType();
	        if (!t.isReferenceType())
	        	return;
	        IntArraySet[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			IntArraySet ilPts = iEnv[lIdx];
			Operand rx = Move.getSrc(q);
			IntArraySet olPts;
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				int rIdx = getIdx(ro);
				olPts = iEnv[rIdx];
			} else
				olPts = nilPts;
			if (olPts == ilPts)
				return;
			IntArraySet[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			Set<IntTrio> oHeap = iDstNode.heap;
			IntArraySet oEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, oHeap, oEsc, false);
		}
		@Override
		public void visitPhi(Quad q) {
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			if (t == null || !t.isReferenceType())
				return;
	        IntArraySet[] iEnv = iDstNode.env;
			ParamListOperand ros = Phi.getSrcs(q);
			int n = ros.length();
			tmpPts.clear();
			IntArraySet pPts = tmpPts;
			for (int i = 0; i < n; i++) {
				RegisterOperand ro = ros.get(i);
				if (ro != null) {
					int rIdx = getIdx(ro);
					IntArraySet rPts = iEnv[rIdx];
					pPts.addAll(rPts);
				}
			}
			int lIdx = getIdx(lo);
			IntArraySet ilPts = iEnv[lIdx];
			IntArraySet olPts;
			if (pPts.isEmpty()) {
				if (ilPts == nilPts)
					return;
				olPts = nilPts;
			} else {
				if (pPts.equals(ilPts))
					return;
				if (pPts.size() == 1 && pPts.contains(ESC_VAL))
					olPts = escPts;
				else
					olPts = new IntArraySet(pPts);
			}
			IntArraySet[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			Set<IntTrio> oHeap = iDstNode.heap;
			IntArraySet oEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, oHeap, oEsc, false);
		}
		@Override
		public void visitALoad(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, ALoad.getBase(q));
			Operator op = q.getOperator();
			if (!((ALoad) op).getType().isReferenceType())
				return;
			IntArraySet[] iEnv = iDstNode.env;
			Set<IntTrio> iHeap = iDstNode.heap;
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			int bIdx = getIdx(bo);
			IntArraySet bPts = iEnv[bIdx];
			RegisterOperand lo = ALoad.getDest(q);
			int lIdx = getIdx(lo);
			IntArraySet ilPts = iEnv[lIdx];
			IntArraySet olPts = getPtsFromHeap(bPts, 0, iHeap, ilPts);
			if (olPts == ilPts)
				return;
			IntArraySet[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			IntArraySet iEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, iHeap, iEsc, false);
		}
		@Override
		public void visitGetfield(Quad q) {
			if (currLocHeapInsts.contains(q))
				check(q, Getfield.getBase(q));
			jq_Field f = Getfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			IntArraySet[] iEnv = iDstNode.env;
			Set<IntTrio> iHeap = iDstNode.heap;
			RegisterOperand lo = Getfield.getDest(q);
			int lIdx = getIdx(lo);
			IntArraySet ilPts = iEnv[lIdx];
			Operand bx = Getfield.getBase(q);
			IntArraySet olPts;
			if (bx instanceof RegisterOperand) {
				RegisterOperand bo = (RegisterOperand) bx;
				int bIdx = getIdx(bo);
				IntArraySet bPts = iEnv[bIdx];
				int fIdx = domF.indexOf(f);
				olPts = getPtsFromHeap(bPts, fIdx, iHeap, ilPts);
			} else
				olPts = nilPts;
			if (olPts == ilPts)
				return;
			IntArraySet[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			IntArraySet iEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, iHeap, iEsc, false);
		}
		@Override
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
			IntArraySet[] iEnv = iDstNode.env;
			int rIdx = getIdx(ro);
			IntArraySet rPts = iEnv[rIdx];
			if (rPts == nilPts)
				return;
			int bIdx = getIdx(bo);
			IntArraySet bPts = iEnv[bIdx];
			if (bPts == nilPts)
				return;
			processWrite(bPts, rPts, null);
		}
		@Override
		public void visitPutfield(Quad q) {
			if (currLocHeapInsts.contains(q))
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
			IntArraySet[] iEnv = iDstNode.env;
			RegisterOperand ro = (RegisterOperand) rx;
			int rIdx = getIdx(ro);
			IntArraySet rPts = iEnv[rIdx];
			if (rPts == nilPts)
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			IntArraySet bPts = iEnv[bIdx];
			if (bPts == nilPts)
				return;
			processWrite(bPts, rPts, f);
		}
		private void processWrite(IntArraySet bPts, IntArraySet rPts, jq_Field f) {
			IntArraySet oEsc;
			IntArraySet[] oEnv;
			Set<IntTrio> oHeap;
			if (bPts == escPts) {
				if (rPts == escPts)
					return;
				IntArraySet iEsc = iDstNode.esc;
				Set<IntTrio> iHeap = iDstNode.heap;
				oEsc = propagateEsc(rPts, iHeap, iEsc);
				if (oEsc == iEsc)
					return;
				IntArraySet[] iEnv = iDstNode.env;
				oEnv = updateEnv(iEnv, oEsc);
				oHeap = updateHeap(iHeap, oEsc);
			} else {
				int nb = bPts.size();
				int nr = rPts.size();
				Set<IntTrio> iHeap = iDstNode.heap;
				oHeap = iHeap;
				int fIdx = (f == null) ? 0 : domF.indexOf(f); 
				boolean foundEsc = false;
				for (int i = 0; i < nb; i++) {
					int hIdx = bPts.get(i);
					if (hIdx == ESC_VAL) {
						foundEsc = true;
						continue;
					}
					for (int j = 0; j < nr; j++) {
						int hIdx2 = rPts.get(j);
						IntTrio trio = new IntTrio(hIdx, fIdx, hIdx2);
						if (oHeap != iHeap)
							oHeap.add(trio);
						else if (!iHeap.contains(trio)) {
							oHeap = new ArraySet<IntTrio>(iHeap);
							((ArraySet) oHeap).addForcibly(trio);
						}
					}
				}
				if (rPts == escPts || !foundEsc) {
					if (oHeap == iHeap)
						return;
					oEsc = iDstNode.esc;
					oEnv = iDstNode.env;
				} else {
					IntArraySet iEsc = iDstNode.esc;
					oEsc = propagateEsc(rPts, oHeap, iEsc);
					if (oEsc == iEsc) {
						if (oHeap == iHeap)
							return;
						oEnv = iDstNode.env;
					} else {
						IntArraySet[] iEnv = iDstNode.env;
						oEnv = updateEnv(iEnv, oEsc);
						oHeap = updateHeap(oHeap, oEsc);
					}
				}
			}
			oDstNode = new DstNode(oEnv, oHeap, oEsc, false);
		}
		@Override
		public void visitPutstatic(Quad q) {
			jq_Field f = Putstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
	        Operand rx = Putstatic.getSrc(q);
	        if (!(rx instanceof RegisterOperand))
	        	return;
			IntArraySet[] iEnv = iDstNode.env;
            RegisterOperand ro = (RegisterOperand) rx;
            int rIdx = getIdx(ro);
            IntArraySet rPts = iEnv[rIdx];
            if (rPts == escPts || rPts == nilPts)
                return;
			Set<IntTrio> iHeap = iDstNode.heap;
			IntArraySet iEsc = iDstNode.esc;
			IntArraySet oEsc = propagateEsc(rPts, iHeap, iEsc);
			if (oEsc == iEsc)
				return;
			IntArraySet[] oEnv = updateEnv(iEnv, oEsc);
			Set<IntTrio> oHeap = updateHeap(iHeap, oEsc);
			oDstNode = new DstNode(oEnv, oHeap, oEsc, false); 
		}
		@Override
		public void visitGetstatic(Quad q) {
			jq_Field f = Getstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
			IntArraySet[] iEnv = iDstNode.env;
	        RegisterOperand lo = Getstatic.getDest(q);
	        int lIdx = getIdx(lo);
			if (iEnv[lIdx] == escPts)
				return;
	        IntArraySet[] oEnv = copy(iEnv);
	       	oEnv[lIdx] = escPts;
			Set<IntTrio> iHeap = iDstNode.heap;
			IntArraySet iEsc = iDstNode.esc;
			oDstNode = new DstNode(oEnv, iHeap, iEsc, false);
		}
		@Override
		public void visitNew(Quad q) {
			RegisterOperand vo = New.getDest(q);
			processAlloc(q, vo);
		}
		@Override
		public void visitNewArray(Quad q) {
			RegisterOperand vo = NewArray.getDest(q);
			processAlloc(q, vo);
		}
		private void processAlloc(Quad q, RegisterOperand vo) {
			IntArraySet[] iEnv = iDstNode.env;
			int vIdx = getIdx(vo);
			IntArraySet vPts = iEnv[vIdx];
			IntArraySet iEsc = iDstNode.esc;
			if (!currAllocs.contains(q)) {
				if (vPts == escPts)
					return;
				vPts = escPts;
			} else {
				int hIdx = domH.indexOf(q);
				if (iEsc.contains(hIdx)) {
					if (vPts == escPts)
						return;
					vPts = escPts;
				} else {
					if (vPts.size() == 1 && vPts.contains(hIdx))
						return;
					vPts = new IntArraySet(1);
					vPts.add(hIdx);
				}
			}
			IntArraySet[] oEnv = copy(iEnv);
			oEnv[vIdx] = vPts;
			Set<IntTrio> oHeap = iDstNode.heap;
			oDstNode = new DstNode(oEnv, oHeap, iEsc, false);
		}
		private void check(Quad q, Operand bx) {
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			IntArraySet pts = iDstNode.env[bIdx];
			if (pts.contains(ESC_VAL)) {
				currLocHeapInsts.remove(q);
				currEscHeapInsts.add(q);
				if (currLocHeapInsts.size() == 0)
					throw new ThrEscException();
			}
		}
	}
	private IntArraySet getPtsFromHeap(IntArraySet bPts, int fIdx,
			Set<IntTrio> heap, IntArraySet rPts) {
		tmpPts.clear();
		if (bPts.contains(ESC_VAL))
			tmpPts.add(ESC_VAL);
		for (IntTrio t : heap) {
			if (t.idx1 == fIdx && bPts.contains(t.idx0))
				tmpPts.add(t.idx2);
		}
		if (tmpPts.isEmpty())
			return nilPts;
		if (tmpPts.size() == 1 && tmpPts.contains(ESC_VAL))
			return escPts;
		if (tmpPts.equals(rPts))
			return rPts;
		return new IntArraySet(tmpPts);
	}
    private IntArraySet propagateEsc(IntArraySet pts, Set<IntTrio> iHeap,
			IntArraySet iEsc) {
        assert (pts != escPts);
        assert (pts != nilPts);
		IntArraySet oEsc = null;
		int n = pts.size();
		for (int i = 0; i < n; i++) {
			int x = pts.get(i);
			if (x != ESC_VAL && !iEsc.contains(x)) {
				if (oEsc == null)
					oEsc = new IntArraySet(n);
				oEsc.add(x);
			}
		}
		if (oEsc == null)
			return iEsc;
		oEsc.addAll(iEsc);
		boolean changed;
		do {
			changed = false;
			for (IntTrio t : iHeap) {
				int hIdx = t.idx0;
				assert (hIdx != ESC_VAL);
				if (oEsc.contains(hIdx)) {
					int h2Idx = t.idx2;
					if (h2Idx != ESC_VAL && oEsc.add(h2Idx))
						changed = true;
				}
			}
		} while (changed);
		return oEsc;
	}
	private IntArraySet[] updateEnv(IntArraySet[] iEnv, IntArraySet oEsc) {
		IntArraySet[] oEnv = null;
        int n = iEnv.length;
		for (int i = 0; i < n; i++) {
            IntArraySet pts = iEnv[i];
            if (pts != nilPts && pts != escPts && oEsc.overlaps(pts)) {
				if (oEnv == null) {
					oEnv = new IntArraySet[n];
					for (int j = 0; j < i; j++)
						oEnv[j] = iEnv[j];
				}
				tmpPts.clear();
				int m = pts.size();
				for (int j = 0; j < m; j++) {
					int x = pts.get(j);
					if (x != ESC_VAL && !oEsc.contains(x))
						tmpPts.addForcibly(x);
				}
				IntArraySet pts2;
				if (tmpPts.isEmpty())
					pts2 = escPts;
				else {
					tmpPts.addForcibly(ESC_VAL);
					pts2 = new IntArraySet(tmpPts);
				}
				oEnv[i] = pts2;
			} else if (oEnv != null)
				oEnv[i] = pts;
        }
		return (oEnv == null) ? iEnv : oEnv;
	}
	private Set<IntTrio> updateHeap(Set<IntTrio> iHeap, IntArraySet oEsc) {
		boolean buildHeap = false;
        for (IntTrio t : iHeap) {
            int hIdx = t.idx0;
            if (oEsc.contains(hIdx)) {
				buildHeap = true;
				break;
			}
			int h2Idx = t.idx2;
			if (oEsc.contains(h2Idx)) {
				buildHeap = true;
				break;
            }
		}
		if (!buildHeap)
			return iHeap;
		Set<IntTrio> oHeap = new ArraySet<IntTrio>(iHeap.size());
		for (IntTrio t : iHeap) {
			int hIdx = t.idx0;
			if (!oEsc.contains(hIdx)) {
				int h2Idx = t.idx2;
				if (oEsc.contains(h2Idx))
					oHeap.add(new IntTrio(hIdx, t.idx1, ESC_VAL));
				else
					oHeap.add(t);
			}
		}
		return oHeap;
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
			String x = (e == nilPts) ? "N" : toString(e);
				// (e == escPts) ? "E" : toString(e);
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

