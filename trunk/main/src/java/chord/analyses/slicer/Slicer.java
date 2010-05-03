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
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;
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

import chord.util.FileUtils;
import chord.project.Messages;
import chord.program.Location;
import chord.program.MethodSign;
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
	private final Set<Edge> emptyEdgeSet = Collections.emptySet();
	private final Set<Expr> emptyExprSet = Collections.emptySet();
	private final Set<Edge> tmpEdgeSet = new ArraySet<Edge>();
	private final Set<Expr> tmpExprSet = new ArraySet<Expr>();
	private DomM domM;
	private DomI domI;
	private DomV domV;
	private DomF domF;
    private ICICG cicg;
    private MyQuadVisitor qv = new MyQuadVisitor();
	private Set<Quad> currSlice = new HashSet<Quad>();
	private Pair<Location, Expr> currSeed;

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
		Project.runTask("P");

		List<String> fStrList = FileUtils.readFileToList("seeds.txt");
		Set<Pair<Location, Expr>> seeds =
			new HashSet<Pair<Location, Expr>>(fStrList.size());
		Location mainExitLoc = getMainExitLoc();
		for (String fStr : fStrList) {
			System.out.println("XXX: " + fStr);
			MethodSign sign = MethodSign.parse(fStr);
			jq_Class c = Program.v().getPreparedClass(sign.cName);
			if (c == null) {
				Messages.logAnon("WARN: Ignoring slicing on field %s: " +
					" its declaring class was not found.", fStr);
				continue;
			}
			jq_Field f = (jq_Field) c.getDeclaredMember(sign.mName, sign.mDesc);
			if (f == null) {
				Messages.logAnon("WARN: Ignoring slicing on field %s: " +
					"it was not found in its declaring class.", fStr);
				continue;
			}
			assert(f.isStatic());
			Expr e = new StatField(f);
			seeds.add(new Pair<Location, Expr>(mainExitLoc, e));
		}

        for (Pair<Location, Expr> seed : seeds) {
			currSlice.clear();
			currSeed = seed;
			System.out.println("*********** Seed: " + seed);
			Timer timer = new Timer("slicer-timer");
			timer.init();
			runPass();
			for (Quad p : currSlice) {
				jq_Method m = Program.v().getMethod(p);
				System.out.println("\t" + m + " " + p);
			}
			timer.done();
			System.out.println(timer.getInclusiveTimeStr());
		}
	}

	private static Location getMainExitLoc() {
		jq_Method mainMethod = Program.v().getMainMethod();
		BasicBlock bb = mainMethod.getCFG().exit();
		return new Location(mainMethod, bb, -1, null);
	}

	@Override
	public boolean doMerge() {
		return false;
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
        Edge pe = new Edge(e, e);
        Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
        initPEs.add(pair);
        return initPEs;
    }

	@Override
	public Set<Edge> getInitPathEdges(Quad q, jq_Method m2, Edge pe) {
		// check if pe.dstExpr is return var
		// if so then create init edge srcExpr==dstExpr==null
		// else if pe.dstExpr is non-local var then create init edge similarly
		// else return emptyset
		Expr dstExpr = pe.dstExpr;
		if (dstExpr instanceof LocalVar) {
			LocalVar x = (LocalVar) dstExpr;
            RegisterOperand vo = Invoke.getDest(q);
            if (vo != null) {
                Register v = vo.getRegister();
				if (x.v == v) {
					tmpEdgeSet.clear();
					Edge pe2 = new Edge(null, null);
					tmpEdgeSet.add(pe2);
					return tmpEdgeSet;
				}
			}
			// ignore local vars other than return vars;
			// they will be handled by getInvkPathEdges
			return emptyEdgeSet;
		}
		tmpEdgeSet.clear();
		tmpEdgeSet.add(pe);
		return tmpEdgeSet;
	}

	@Override
	public Set<Edge> getMiscPathEdges(Quad q, Edge pe) {
		qv.iDstExpr = pe.dstExpr;
		qv.oDstExprSet = null;
		q.accept(qv);
		Set<Expr> oDstExprSet = qv.oDstExprSet;
		tmpEdgeSet.clear();
		if (oDstExprSet == null) {
			tmpEdgeSet.add(pe);
			return tmpEdgeSet;
		}
		currSlice.add(q);
		Expr srcExpr = pe.srcExpr;
		// todo: use currentMethod and srcExpr to get control-dep info
		for (Expr e : oDstExprSet) {
			Edge pe2 = new Edge(srcExpr, e);
			tmpEdgeSet.add(pe2);
		}
		return tmpEdgeSet;
	}

	@Override
	public Set<Edge> getInvkPathEdges(Quad q, Edge pe) {
		Expr dstExpr = pe.dstExpr;
		if (dstExpr instanceof LocalVar) {
			LocalVar x = (LocalVar) dstExpr;
            RegisterOperand vo = Invoke.getDest(q);
            if (vo != null) {
                Register v = vo.getRegister();
				if (x.v != v) {
					tmpEdgeSet.clear();
					tmpEdgeSet.add(pe);
					return tmpEdgeSet;
				}
			}
		}
		return emptyEdgeSet;
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		return pe;
	}

	private Expr getArgExpr(Quad q, Expr e) {
		if (e instanceof LocalVar) {
			Register v = ((LocalVar) e).v;
			int i = v.getNumber();
            ParamListOperand l = Invoke.getParamList(q);
            int numArgs = l.length();
			assert(i >= 0 && i < numArgs);
			Register u = l.get(i).getRegister();
			return new LocalVar(u); 
		}
		return e;
	}

	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method tgtM, Edge tgtSE) {
		Expr dstExpr = clrPE.dstExpr;
		Expr srcExpr = tgtSE.srcExpr;
		if (dstExpr instanceof LocalVar) {
            LocalVar x = (LocalVar) dstExpr;
            RegisterOperand vo = Invoke.getDest(q);
            if (vo != null) {
                Register v = vo.getRegister();
                if (x.v == v && srcExpr == null) {
					Expr e = getArgExpr(q, tgtSE.dstExpr);
					return new Edge(clrPE.srcExpr, e);
                }
			}
			return null;
		}
		if (dstExpr.equals(srcExpr)) {
			Expr e = getArgExpr(q, tgtSE.dstExpr);
			return new Edge(clrPE.srcExpr, e);
		}
		return null;
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		// iDstExpr can be read by visit* methods (if it is null
		// then the visited quad is guaranteed to be a return stmt).
		// oDstExprSet is set to null and may be written by visit*
		// methods; if it is null upon exit then it will be assumed
		// that the visited quad is not relevant to the slice, and
		// the outgoing pe will be the same as the incoming pe
		Expr iDstExpr;
		Set<Expr> oDstExprSet;
		@Override
		public void visitReturn(Quad q) {
		}
		@Override
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}
		@Override
		public void visitMove(Quad q) {
			assert (iDstExpr != null);
			if (iDstExpr instanceof LocalVar) {
				LocalVar x = (LocalVar) iDstExpr;
				Register l = Move.getDest(q).getRegister();
				if (x.v == l) {
					tmpExprSet.clear();
					Operand rx = Move.getSrc(q);
					if (rx instanceof RegisterOperand) {
						Register r = ((RegisterOperand) rx).getRegister();
						tmpExprSet.add(new LocalVar(r));
					}
					oDstExprSet = tmpExprSet;
				}
			}
		}
		@Override
		public void visitPhi(Quad q) {
			assert (iDstExpr != null);
		 	if (iDstExpr instanceof LocalVar) {
				LocalVar x = (LocalVar) iDstExpr;
				Register l = Phi.getDest(q).getRegister();
				if (x.v == l) {
					tmpExprSet.clear();
					ParamListOperand ros = Phi.getSrcs(q);
					int n = ros.length();
					for (int i = 0; i < n; i++) {
						RegisterOperand ro = ros.get(i);
						if (ro != null) {
							Register r = ro.getRegister();
							LocalVar y = new LocalVar(r);
							tmpExprSet.add(y);
						}
					}
					oDstExprSet = tmpExprSet;
				}
			}
		}
		@Override
		public void visitALoad(Quad q) {
			assert (iDstExpr != null);
/*
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			Register b = bo.getRegister();
			// CIObj bObj = cipa.pointsTo(b);
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
			assert (iDstExpr != null);
/*
			jq_Field f = Getfield.getField(q).getField();
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
			assert (iDstExpr != null);
/*
			Operator op = q.getOperator();
			Operand rx = AStore.getValue(q);
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
			assert (iDstExpr != null);
/*
			jq_Field f = Putfield.getField(q).getField();
			Operand rx = Putfield.getSrc(q);
			Operand bx = Putfield.getBase(q);
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
			assert (iDstExpr != null);
/*
			jq_Field f = Putstatic.getField(q).getField();
	        Operand rx = Putstatic.getSrc(q);
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
			assert (iDstExpr != null);
/*
			jq_Field f = Getstatic.getField(q).getField();
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
*/
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
			assert (iDstExpr != null);
		 	if (iDstExpr instanceof LocalVar) {
				LocalVar x = (LocalVar) iDstExpr;
				Register v = vo.getRegister();
				if (x.v == v)
					oDstExprSet = emptyExprSet;
			}
		}
	}
}

