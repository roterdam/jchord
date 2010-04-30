/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

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

import chord.program.Location;
import chord.analyses.alias.ICICG;
import chord.analyses.alias.ThrOblAbbrCICGAnalysis;
import chord.util.tuple.object.Pair;
import chord.util.tuple.integer.IntPair;
import chord.program.Program;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.BackwardRHSAnalysis;
import chord.bddbddb.Rel.IntPairIterable;
import chord.project.Properties;
import chord.util.ChordRuntimeException;
import chord.doms.DomF;
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
	    name = "slicer-java"
	)
public class Slicer extends BackwardRHSAnalysis<Edge, Edge> {
	private static final Set<Quad> emptyQuadSet = Collections.emptySet();
	private static final Set<Expr> emptyExprSet = Collections.emptySet();
	private DomM domM;
	private DomI domI;
	private DomV domV;
	private DomF domF;
    private ICICG cicg;
    private MyQuadVisitor qv = new MyQuadVisitor();
	private Pair<Location, Expr> currSeed;
	private Set<Pair<jq_Method, Quad>> currSlice =
		new HashSet<Pair<jq_Method, Quad>>();

	public void run() {
        domM = (DomM) Project.getTrgt("M");
        Project.runTask(domM);
		domV = (DomV) Project.getTrgt("V");
		Project.runTask(domV);
		domF = (DomF) Project.getTrgt("F");
		Project.runTask(domF);
		domI = (DomI) Project.getTrgt("I");
		Project.runTask(domI);
		domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);

		Set<Pair<Location, Expr>> seeds = new HashSet<Pair<Location, Expr>>();
		List<String> fields = FileUtils.readFileToList("seeds.txt");

        for (Pair<Location, Expr> seed : seeds) {
			currSlice.clear();
			currSeed = seed;
			System.out.println("*********** Seed: " + seed);
			Timer timer = new Timer("slicer-timer");
			timer.init();
			runPass();
			for (Pair<jq_Method, Quad> p : currSlice) {
				System.out.println("\t" + p);
			}
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
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

    @Override
    public Set<Pair<Location, Edge>> getInitPathEdges() {
        Set<Pair<Location, Edge>> initPEs =
            new ArraySet<Pair<Location, Edge>>(1);
		Location loc = currSeed.val0;
		Expr e = currSeed.val1;
        Edge pe = new Edge(e, emptyExprSet, emptyQuadSet);
        Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
        initPEs.add(pair);
        return initPEs;
    }

	@Override
	public Edge getInitPathEdge(Quad q, jq_Method m2, Edge pe) {
		// todo
/*
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
		DstNode dstNode2 = new DstNode(env, dstNode.heap, nilPts);
		Edge pe2 = new PathEdge(srcNode2, dstNode2);
		return pe2;
*/
		return null;
	}

	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		// todo
/*
		DstNode dstNode = pe.dstNode;
		qv.iDstNode = dstNode;
		qv.oDstNode = dstNode;
		q.accept(qv);
		DstNode dstNode2 = qv.oDstNode;
		PathEdge pe2 = // (dstNode2 == dstNode) ? pe :
			new PathEdge(pe.srcNode, dstNode2);
		return pe2;
*/
		return null;
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		// todo
/*
		DstNode dstNode = pe.dstNode;
		IntArraySet rPts = nilPts;
		RetNode retNode = new RetNode(rPts, dstNode.heap, dstNode.esc);
		SummaryEdge se = new SummaryEdge(pe.srcNode, retNode);
		return se;
*/
		return null;
	}

	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		// todo
/*
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
        DstNode clrDstNode2 = new DstNode(clrDstEnv2,
        	tgtRetNode.heap, clrDstEsc2);
        PathEdge pe2 = new PathEdge(clrPE.srcNode, clrDstNode2);
		return pe2;
*/
		return null;
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		// DstNode iDstNode;
		// DstNode oDstNode;
		@Override
		public void visitReturn(Quad q) {
			// todo
		}
		@Override
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}
		@Override
		public void visitMove(Quad q) {
			// todo
/*
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
			oDstNode = new DstNode(oEnv, oHeap, oEsc);
*/
		}
		@Override
		public void visitPhi(Quad q) {
			// todo
/*
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
			oDstNode = new DstNode(oEnv, oHeap, oEsc);
*/
		}
		@Override
		public void visitALoad(Quad q) {
			// todo
/*
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
			oDstNode = new DstNode(oEnv, iHeap, iEsc);
*/
		}
		@Override
		public void visitGetfield(Quad q) {
/*
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
			oDstNode = new DstNode(oEnv, iHeap, iEsc);
*/
		}
		@Override
		public void visitAStore(Quad q) {
/*
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
*/
		}
		@Override
		public void visitPutfield(Quad q) {
/*
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
*/
		}
		private void processWrite(IntArraySet bPts, IntArraySet rPts, jq_Field f) {
/*
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
			oDstNode = new DstNode(oEnv, oHeap, oEsc);
*/
		}
		@Override
		public void visitPutstatic(Quad q) {
/*
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
			oDstNode = new DstNode(oEnv, oHeap, oEsc); 
*/
		}
		@Override
		public void visitGetstatic(Quad q) {
/*
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
			oDstNode = new DstNode(oEnv, iHeap, iEsc);
		}
		@Override
		public void visitNew(Quad q) {
/*
			RegisterOperand vo = New.getDest(q);
			processAlloc(q, vo);
*/
		}
		@Override
		public void visitNewArray(Quad q) {
/*
			RegisterOperand vo = NewArray.getDest(q);
			processAlloc(q, vo);
*/
		}
		private void processAlloc(Quad q, RegisterOperand vo) {
/*
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
			oDstNode = new DstNode(oEnv, oHeap, iEsc);
*/
		}
	}
}

