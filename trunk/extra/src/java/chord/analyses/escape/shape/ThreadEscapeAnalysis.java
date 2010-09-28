/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.shape;

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
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

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
import joeq.Compiler.Quad.Operator.MultiNewArray;
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
import chord.project.Config;
import chord.util.ChordRuntimeException;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.util.ArraySet;
import chord.util.CompareUtils;
import chord.util.Timer;

import chord.util.Execution;
import chord.project.OutDirUtils;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "thresc-shape-java")
public class ThreadEscapeAnalysis extends ForwardRHSAnalysis<Edge, Edge> {
	private static final ArraySet<FldObj> emptyHeap = new ArraySet(0);
	private static final Obj[] emptyRetEnv = new Obj[] { Obj.EMTY };
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

	// set of heap access insts deemed esc. by whole-program analysis
	private Set<Quad> allEscEs = new HashSet<Quad>();
	// set of heap access insts proven loc. by whole-program analysis
	private Set<Quad> allLocEs = new HashSet<Quad>();
	private Set<Quad> currHs;
	private Set<Quad> currLocEs = new HashSet<Quad>();
	private Set<Quad> currEscEs = new HashSet<Quad>();
    private MyQuadVisitor qv = new MyQuadVisitor();
	private jq_Method mainMethod;
	private jq_Method threadStartMethod;

	@Override
	public void run() {
		Program program = Program.g();
		mainMethod = program.getMainMethod();
		threadStartMethod = program.getThreadStartMethod();
        domI = (DomI) ClassicProject.g().getTrgt("I");
        ClassicProject.g().runTask(domI);
        domM = (DomM) ClassicProject.g().getTrgt("M");
        ClassicProject.g().runTask(domM);
		domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		domF = (DomF) ClassicProject.g().getTrgt("F");
		ClassicProject.g().runTask(domF);
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);

		Map<Quad, Set<Quad>> e2hsMap = new HashMap<Quad, Set<Quad>>();
		ClassicProject.g().runTask("EH-dlog");

		Set<Quad> globalHs = new HashSet<Quad>();
		for (int hIdx = 1; hIdx < domH.size(); hIdx++) {
			globalHs.add((Quad) domH.get(hIdx));
		}
		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("relevantE");
		rel.load();
		for (int eIdx = 0; eIdx < domE.size(); eIdx++) {
			if (rel.contains(eIdx)) {
				Quad e = (Quad) domE.get(eIdx);
				e2hsMap.put(e, globalHs);
			}
		}
		rel.close();

/*
		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("EH");
		rel.load();
		IntPairIterable tuples = rel.getAry2IntTuples();
		for (IntPair tuple : tuples) {
			int eIdx = tuple.idx0;
			int hIdx = tuple.idx1;
			Quad e = (Quad) domE.get(eIdx);
			Quad h = (Quad) domH.get(hIdx);
			Set<Quad> hs = e2hsMap.get(e);
			if (hs == null) {
				hs = new ArraySet<Quad>();
				e2hsMap.put(e, hs);
			}
			hs.add(h);
		}
		rel.close();
*/

		Map<Set<Quad>, Set<Quad>> hs2esMap =
			new HashMap<Set<Quad>, Set<Quad>>();
        for (Map.Entry<Quad, Set<Quad>> entry : e2hsMap.entrySet()) {
            Quad e = entry.getKey();
            Set<Quad> hs = entry.getValue();
            Set<Quad> es = hs2esMap.get(hs);
            if (es == null) {
                es = new ArraySet<Quad>();
                hs2esMap.put(hs, es);
            }
            es.add(e);
        }

		int numV = domV.size();
		varId = new int[numV];
		for (int vIdx = 0; vIdx < numV;) {
			Register v = domV.get(vIdx);
			jq_Method m = domV.getMethod(v);
			int n = m.getLiveRefVars().size();
			methToNumVars.put(m, n);
			for (int i = 0; i < n; i++) {
				varId[vIdx + i] = i;
			}
			vIdx += n;
		}

		init();

		for (Map.Entry<Set<Quad>, Set<Quad>> entry : hs2esMap.entrySet()) {
			currHs = entry.getKey();
			currLocEs = entry.getValue();
			currEscEs.clear();
			System.out.println("**************");
			System.out.println("currEs:");
			for (Quad q : currLocEs) {
				int x = domE.indexOf(q);
				System.out.println("\t" + q.toVerboseStr() + " " + x);
			}
			System.out.println("currHs:");
			for (Quad q : currHs)
				System.out.println("\t" + q.toVerboseStr());
			Timer timer = new Timer("thresc-shape-timer");
			timer.init();
			try {
				runPass();
			} catch (ThrEscException ex) {
				// do nothing
			}
			for (Quad q : currLocEs)
				System.out.println("LOC: " + q.toVerboseStr());
			for (Quad q : currEscEs)
				System.out.println("ESC: " + q.toVerboseStr());
			allLocEs.addAll(currLocEs);
			allEscEs.addAll(currEscEs);
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
		}
		try {
			String outDirName = Config.outDirName;
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullEscE.txt")));
				for (Quad e : allEscEs)
					writer.println(e.toLocStr());
				writer.close();
			}
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_fullLocE.txt")));
				for (Quad e : allLocEs)
					writer.println(e.toLocStr());
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
				(ThrOblAbbrCICGAnalysis) ClassicProject.g().getTrgt("throbl-abbr-cicg-java");
			ClassicProject.g().runTask(cicgAnalysis);
			cicg = cicgAnalysis.getCallGraph();
		}
		return cicg;
	}

	// m is either the main method or the thread root method
	private Edge getRootPathEdge(jq_Method m) {
		assert (m == mainMethod || m == threadStartMethod);
		int n = methToNumVars.get(m);
		Obj[] env = new Obj[n];
		for (int i = 0; i < n; i++)
			env[i] = Obj.EMTY;
		if (m == threadStartMethod) {
			// arg of start method of java.lang.Thread escapes
			env[0] = Obj.ONLY_ESC;
		}
		SrcNode srcNode = new SrcNode(env, emptyHeap);
		DstNode dstNode = new DstNode(env, emptyHeap, false, false);
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
	public Edge getInitPathEdge(Quad q, jq_Method m2, Edge pe) {
		Edge pe2;
		if (m2 == threadStartMethod) {
			// ignore pe
			pe2 = getRootPathEdge(m2);
		} else {
			DstNode dstNode = pe.dstNode;
			Obj[] dstEnv = dstNode.env;
			ParamListOperand args = Invoke.getParamList(q);
			int numArgs = args.length();
			int numVars = methToNumVars.get(m2);
			Obj[] env = new Obj[numVars];
			int z = 0;
			for (int i = 0; i < numArgs; i++) {
				RegisterOperand ao = args.get(i);
				if (ao.getType().isReferenceType()) {
					int aIdx = getIdx(ao);
					Obj o = dstEnv[aIdx];
					env[z++] = o;
				}
			}
			while (z < numVars)
				env[z++] = Obj.EMTY;
			SrcNode srcNode2 = new SrcNode(env, dstNode.heap);
			DstNode dstNode2 = new DstNode(env, dstNode.heap, false, false);
			pe2 = new Edge(srcNode2, dstNode2);
		}
		return pe2;
	}

	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		qv.iDstNode = dstNode;
		qv.oDstNode = dstNode;
		q.accept(qv);
		DstNode dstNode2 = qv.oDstNode;
		return new Edge(pe.srcNode, dstNode2);
	}

	private Edge getForkPathEdge(Quad q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		Obj[] iEnv = dstNode.env;
		RegisterOperand ao = Invoke.getParam(q, 0);
		int aIdx = getIdx(ao);
		Obj aPts = iEnv[aIdx];
		DstNode dstNode2;
		if (aPts == Obj.ONLY_ESC || aPts == Obj.EMTY)
			dstNode2 = dstNode;
		else
			dstNode2 = reset(dstNode);
		Edge pe2 = new Edge(pe.srcNode, dstNode2);
		return pe2;
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		Edge se;
		DstNode dstNode = pe.dstNode;
		if (dstNode.isRetn)
			se = new Edge(pe.srcNode, dstNode);
		else {
			dstNode = new DstNode(emptyRetEnv, dstNode.heap,
				dstNode.isKill, true);
			se = new Edge(pe.srcNode, dstNode);
		}
		return se;
	}

	@Override
	public Edge getCopy(Edge pe) {
		return new Edge(pe.srcNode, pe.dstNode);
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
		Obj[] clrDstEnv = clrDstNode.env;
		Obj[] tgtSrcEnv = tgtSrcNode.env;
        ParamListOperand args = Invoke.getParamList(q);
        int numArgs = args.length();
        for (int i = 0, fIdx = 0; i < numArgs; i++) {
            RegisterOperand ao = args.get(i);
            if (ao.getType().isReferenceType()) {
                int aIdx = getIdx(ao);
                Obj aPts = clrDstEnv[aIdx];
                Obj fPts = tgtSrcEnv[fIdx];
                if (aPts != fPts)
                	return null;
                fIdx++;
			}
		}
		DstNode tgtRetNode = tgtSE.dstNode;
        int n = clrDstEnv.length;
        Obj[] clrDstEnv2 = new Obj[n];
        RegisterOperand ro = Invoke.getDest(q);
        int rIdx = -1;
        if (ro != null && ro.getType().isReferenceType()) {
        	rIdx = getIdx(ro);
        	clrDstEnv2[rIdx] = tgtRetNode.env[0];
        }
		boolean isKill = tgtRetNode.isKill;
		if (isKill) {
			for (int i = 0; i < n; i++) {
				if (i != rIdx) {
        			clrDstEnv2[i] = Obj.ONLY_ESC;
				}
			}
		} else {
			for (int i = 0; i < n; i++) {
				if (i != rIdx) {
					Obj pts = clrDstEnv[i];
        			clrDstEnv2[i] = pts;
				}
			}
        }
        DstNode clrDstNode2 = new DstNode(clrDstEnv2,
			tgtRetNode.heap, isKill, false);
		return new Edge(clrPE.srcNode, clrDstNode2);
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		DstNode iDstNode;
		DstNode oDstNode;
		@Override
		public void visitReturn(Quad q) {
			Obj[] oEnv = null;
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				if (ro.getType().isReferenceType()) {
					int rIdx = getIdx(ro);
					Obj rPts = iDstNode.env[rIdx];
					oEnv = new Obj[1];
					oEnv[0] = rPts;
				}
			}
			if (oEnv == null)
				oEnv = emptyRetEnv;
			oDstNode = new DstNode(oEnv, iDstNode.heap,
				iDstNode.isKill, true);
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
	        Obj[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Operand rx = Move.getSrc(q);
			Obj olPts;
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				int rIdx = getIdx(ro);
				olPts = iEnv[rIdx];
			} else
				olPts = Obj.EMTY;
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap,
				iDstNode.isKill, false);
		}
		@Override
		public void visitPhi(Quad q) {
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			if (t == null || !t.isReferenceType())
				return;
	        Obj[] iEnv = iDstNode.env;
			ParamListOperand ros = Phi.getSrcs(q);
			int n = ros.length();
			Obj olPts = Obj.EMTY;
			for (int i = 0; i < n; i++) {
				RegisterOperand ro = ros.get(i);
				if (ro != null) {
					int rIdx = getIdx(ro);
					Obj rPts = iEnv[rIdx];
					olPts = getObj(olPts, rPts);
				}
			}
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap,
				iDstNode.isKill, false);
		}
		@Override
		public void visitALoad(Quad q) {
			if (currLocEs.contains(q))
				check(q, ALoad.getBase(q));
			Operator op = q.getOperator();
			if (!((ALoad) op).getType().isReferenceType())
				return;
			Obj[] iEnv = iDstNode.env;
			ArraySet<FldObj> iHeap = iDstNode.heap;
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			int bIdx = getIdx(bo);
			Obj bPts = iEnv[bIdx];
			RegisterOperand lo = ALoad.getDest(q);
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Obj olPts = getPtsFromHeap(bPts, null, iHeap);
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iHeap,
				iDstNode.isKill, false);
		}
		@Override
		public void visitGetfield(Quad q) {
			if (currLocEs.contains(q))
				check(q, Getfield.getBase(q));
			jq_Field f = Getfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			Obj[] iEnv = iDstNode.env;
			ArraySet<FldObj> iHeap = iDstNode.heap;
			RegisterOperand lo = Getfield.getDest(q);
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Operand bx = Getfield.getBase(q);
			Obj olPts;
			if (bx instanceof RegisterOperand) {
				RegisterOperand bo = (RegisterOperand) bx;
				int bIdx = getIdx(bo);
				Obj bPts = iEnv[bIdx];
				olPts = getPtsFromHeap(bPts, f, iHeap);
			} else
				olPts = Obj.EMTY;
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iHeap,
				iDstNode.isKill, false);
		}
		@Override
		public void visitAStore(Quad q) {
			if (currLocEs.contains(q))
				check(q, AStore.getBase(q));
			Operator op = q.getOperator();
			if (!((AStore) op).getType().isReferenceType())
				return;
			Operand rx = AStore.getValue(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
			RegisterOperand ro = (RegisterOperand) rx;
			Obj[] iEnv = iDstNode.env;
			int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
			if (rPts == Obj.EMTY)
				return;
			int bIdx = getIdx(bo);
			Obj bPts = iEnv[bIdx];
			if (bPts == Obj.EMTY)
				return;
			processWrite(bPts, rPts, null);
		}
		@Override 
		public void visitPutfield(Quad q) {
			if (currLocEs.contains(q))
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
			Obj[] iEnv = iDstNode.env;
			RegisterOperand ro = (RegisterOperand) rx;
			int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
			if (rPts == Obj.EMTY)
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			Obj bPts = iEnv[bIdx];
			if (bPts == Obj.EMTY)
				return;
			processWrite(bPts, rPts, f);
		}
		private void processWrite(Obj bPts, Obj rPts, jq_Field f) {
			if ((bPts == Obj.ONLY_ESC || bPts == Obj.BOTH) &&
				(rPts == Obj.ONLY_LOC || rPts == Obj.BOTH)) {
				oDstNode = reset(iDstNode);
				return;
			}
			if (bPts == Obj.ONLY_LOC || bPts == Obj.BOTH) {
				ArraySet<FldObj> iHeap = iDstNode.heap;
				int n = iHeap.size();
				ArraySet<FldObj> oHeap = null;
				for (int i = 0; i < n; i++) {
					FldObj fo = iHeap.get(i);
					if (fo.f == f) {
						boolean isLoc = fo.isLoc;
						boolean isEsc = fo.isEsc;
						Obj pts1 = getObj(isLoc, isEsc);
						Obj pts2 = getObj(pts1, rPts);
						boolean isLoc2 = isLoc(pts2);
						boolean isEsc2 = isEsc(pts2);
						if (isLoc == isLoc2 && isEsc == isEsc2)
							return;
						oHeap = new ArraySet<FldObj>(n);
						for (int j = 0; j < i; j++)
							oHeap.add(iHeap.get(j));
						FldObj fo2 = new FldObj(f, isLoc2, isEsc2);
						oHeap.add(fo2);
						for (int j = i + 1; j < n; j++)
							oHeap.add(iHeap.get(j));
						break;
					}
				}
				if (oHeap == null) {
					oHeap = new ArraySet<FldObj>(iHeap);
					boolean isLoc = isLoc(rPts);
					boolean isEsc = isEsc(rPts);
					FldObj fo = new FldObj(f, isLoc, isEsc);
					oHeap.add(fo);
				}
				oDstNode = new DstNode(iDstNode.env, oHeap,
					iDstNode.isKill, false);
			}
		}
		@Override
		public void visitPutstatic(Quad q) {
			jq_Field f = Putstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
	        Operand rx = Putstatic.getSrc(q);
	        if (!(rx instanceof RegisterOperand))
	        	return;
			Obj[] iEnv = iDstNode.env;
            RegisterOperand ro = (RegisterOperand) rx;
            int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
            if (rPts == Obj.ONLY_ESC || rPts == Obj.EMTY)
                return;
			oDstNode = reset(iDstNode);
		}
		@Override
		public void visitGetstatic(Quad q) {
			jq_Field f = Getstatic.getField(q).getField();
	        if (!f.getType().isReferenceType())
	        	return;
			Obj[] iEnv = iDstNode.env;
	        RegisterOperand lo = Getstatic.getDest(q);
	        int lIdx = getIdx(lo);
			if (iEnv[lIdx] == Obj.ONLY_ESC)
				return;
	        Obj[] oEnv = copy(iEnv);
	       	oEnv[lIdx] = Obj.ONLY_ESC;
			oDstNode = new DstNode(oEnv, iDstNode.heap,
				iDstNode.isKill, false);
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
		@Override
		public void visitMultiNewArray(Quad q) {
			RegisterOperand vo = MultiNewArray.getDest(q);
			processAlloc(q, vo);
		}
		private void processAlloc(Quad q, RegisterOperand lo) {
			Obj[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Obj olPts;
			if (!currHs.contains(q)) {
				olPts = Obj.ONLY_ESC;
			} else {
				olPts = Obj.ONLY_LOC;
			}
			if (ilPts == olPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap,
				iDstNode.isKill, false);
		}
		private void check(Quad q, Operand bx) {
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			int bIdx = getIdx(bo);
			Obj pts = iDstNode.env[bIdx];
			if (pts == Obj.ONLY_ESC || pts == Obj.BOTH) {
				currLocEs.remove(q);
				currEscEs.add(q);
				if (currLocEs.size() == 0)
					throw new ThrEscException();
			}
		}
	}

	private static Obj getPtsFromHeap(Obj bPts, jq_Field f,
			ArraySet<FldObj> heap) {
		if (bPts == Obj.EMTY || bPts == Obj.ONLY_ESC)
			return bPts;
		Obj pts = null;
		int n = heap.size();
		for (int i = 0; i < n; i++) {
			FldObj fo = heap.get(i);
			if (fo.f == f) {
				pts = getObj(fo.isLoc, fo.isEsc);
				break;
			}
		}
		if (pts == null) {
			return (bPts == Obj.ONLY_LOC) ? Obj.EMTY : Obj.ONLY_ESC;
		} else {
			return (bPts == Obj.ONLY_LOC) ? pts :
				getObj(pts, Obj.ONLY_ESC);
		}
	}

	private static DstNode reset(DstNode in) {
		Obj[] iEnv = in.env;
		int n = iEnv.length;
		boolean change = false;
		for (int i = 0; i < n; i++) {
			Obj pts = iEnv[i];
			if (pts != Obj.ONLY_ESC && pts != Obj.EMTY) {
				change = true;
				break;
			}
		}
		Obj[] oEnv;
		if (change) {
			oEnv = new Obj[n];
			for (int i = 0; i < n; i++) {
				Obj pts = iEnv[i];
				oEnv[i] = (pts == Obj.EMTY) ? Obj.EMTY : Obj.ONLY_ESC;
			}
		} else {
			if (in.isKill && in.heap.isEmpty())
				return in;
			oEnv = iEnv;
		}
		return new DstNode(oEnv, emptyHeap, true, false);
	}

	/*****************************************************************
	 * Frequently used functions
     *****************************************************************/

	private int getIdx(RegisterOperand ro) {
		Register r = ro.getRegister();
		int vIdx = domV.indexOf(r);
		return varId[vIdx];
	}

	private static Obj[] copy(Obj[] a) {
		int n = a.length;
		Obj[] b = new Obj[n];
        for (int i = 0; i < n; i++)
        	b[i] = a[i];
        return b;
	}

	private static Obj getObj(boolean isLoc, boolean isEsc) {
		if (isLoc)
			return isEsc ? Obj.BOTH : Obj.ONLY_LOC;
		else
			return isEsc ? Obj.ONLY_ESC : Obj.EMTY;
	}

	private static Obj getObj(Obj o1, Obj o2) {
		if (o1 == o2) return o1;
		if (o1 == Obj.EMTY) return o2;
		if (o2 == Obj.EMTY) return o1;
		return Obj.BOTH;
	}

	private static boolean isLoc(Obj pts) {
 		return pts == Obj.ONLY_LOC || pts == Obj.BOTH;
	}

	private static boolean isEsc(Obj pts) {
 		return pts == Obj.ONLY_ESC || pts == Obj.BOTH;
	}

	/*****************************************************************
	 * Printing functions
     *****************************************************************/

	public static String toString(Obj[] env) {
		String s = null;
		for (Obj o : env) {
			String x = toString(o);
			s = (s == null) ? x : (s + "," + x);
		}
		if (s == null) return "[]";
		return "[" + s + "]";
	}

	public static String toString(Obj o) {
		switch (o) {
		case EMTY: return "N";
		case ONLY_LOC: return "L";
		case ONLY_ESC: return "E";
		case BOTH: return "*";
		}
		assert (false);
		return null;
	}

	public static String toString(ArraySet<FldObj> heap) {
		String s = null;
		for (FldObj fo : heap) {
			String o;
			if (fo.isLoc)
				o = fo.isEsc ? "*" : "L";
			else
				o = fo.isEsc ? "E" : "N";
			String x = "<" + fo.f + "," + o + ">";
			s = (s == null) ? x : (s + "," + x);
		}
		if (s == null) return "[]";
		return "[" + s + "]";
	}
}

