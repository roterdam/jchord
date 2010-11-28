/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.shape.path;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Putstatic;
import chord.doms.DomE;
import chord.doms.DomH;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IntArraySet;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thresc-shape-path-java",
	produces = { "accE", "escE", "locEH" }, 
	consumes = { "checkExcludedE" },
	namesOfSigns = { "accE", "escE", "locEH" },
	signs = { "E0", "E0", "E0,H0:E0_H0" }
)
public class ThreadEscapePathAnalysis extends DynamicAnalysis {
	private final boolean verbose = false;
	private final boolean doStrongUpdates = true;
	private final boolean smashArrayElems = false;
	private final IntArraySet tmp = new IntArraySet();
	private boolean[] chkE;	// true iff heap access stmt with index e in domE must be checked
	private boolean[] accE;	// true iff heap access stmt with index e in domE is visited
	private boolean[] escE;	// true iff heap access stmt with index e in domE thread-escapes
	private boolean[] accH;	// true iff alloc site with index h in domH is visited
	private IntArraySet[] locEH;	// map from each heap access stmt deemed thread-local to set of alloc sites deemed relevant to it
	private TIntHashSet escObjs;	// set of currently escaping concrete/abstract objects
	private TIntObjectHashMap<List<FldObj>> objToFldObjsFwd;	// map from each object to a list of each non-null instance field of ref type along with its value
	private TIntObjectHashMap<List<FldObj>> objToFldObjsInv;	// inverse of above map
    private TIntIntHashMap objToHid;	// map from each object to the index in domH of its alloc site
    private InstrScheme instrScheme;
	private DomH domH;
	private DomE domE;
	private int numH, numE;

	@Override
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null) return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setNewAndNewArrayEvent(true, false, true);
    	instrScheme.setPutstaticReferenceEvent(false, false, false, false, true);
    	instrScheme.setThreadStartEvent(false, false, true);
    	instrScheme.setGetfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setPutfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setAloadPrimitiveEvent(true, false, true, false);
    	instrScheme.setAstorePrimitiveEvent(true, false, true, false);
    	instrScheme.setGetfieldReferenceEvent(true, false, true, false, false);
    	instrScheme.setPutfieldReferenceEvent(true, false, true, true, true);
    	instrScheme.setAloadReferenceEvent(true, false, true, false, false);
    	instrScheme.setAstoreReferenceEvent(true, false, true, true, true);
    	return instrScheme;
    }

	@Override
	public void initAllPasses() {
		if (smashArrayElems)
			assert (!doStrongUpdates);
		escObjs = new TIntHashSet();
		objToFldObjsFwd = new TIntObjectHashMap<List<FldObj>>();
		objToFldObjsInv = new TIntObjectHashMap<List<FldObj>>();
		objToHid = new TIntIntHashMap();
		domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);
		numE = domE.size();
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		numH = domH.size();
		chkE = new boolean[numE];
		ProgramRel relCheckExcludedE = (ProgramRel) ClassicProject.g().getTrgt("checkExcludedE");
		relCheckExcludedE.load();
		for (int e = 0; e < numE; e++) {
			Quad q = domE.get(e);
			Operator op = q.getOperator();
			if (op instanceof Getstatic || op instanceof Putstatic)
				continue;
			if (!relCheckExcludedE.contains(e))
				chkE[e] = true;
		}
		relCheckExcludedE.close();
		accE = new boolean[numE];
		escE = new boolean[numE];
		locEH = new IntArraySet[numE];
		accH = new boolean[numH];
	}

	@Override
	public void initPass() {
		escObjs.clear();
		objToFldObjsFwd.clear();
		objToFldObjsInv.clear();
		objToHid.clear();
	}

	@Override
	public void doneAllPasses() {
        int numAccH = 0;
        for (int h = 0; h < numH; h++) {
            if (accH[h]) numAccH++;
        }
        System.out.println("numAccH: " + numAccH);

		ProgramRel  accErel  = (ProgramRel) ClassicProject.g().getTrgt("pathAccE");
		ProgramRel  escErel  = (ProgramRel) ClassicProject.g().getTrgt("pathEscE");
		ProgramRel  locEHrel = (ProgramRel) ClassicProject.g().getTrgt("pathLocEH");
		PrintWriter accEout  = OutDirUtils.newPrintWriter("shape_pathAccE.txt");
		PrintWriter escEout  = OutDirUtils.newPrintWriter("shape_pathEscE.txt");
		PrintWriter locEHout = OutDirUtils.newPrintWriter("shape_pathLocEH.txt");
		accErel.zero();
		escErel.zero();
		locEHrel.zero();
		for (int e = 0; e < numE; e++) {
			if (accE[e]) {
				accErel.add(e);
				String estr = domE.get(e).toVerboseStr();
				accEout.println(estr);
				if (escE[e]) {
					escErel.add(e);
					escEout.println(estr);
				} else {
					IntArraySet hs = locEH[e];
					locEHout.println(estr);
					if (hs != null) {
						int n = hs.size();
						for (int i = 0; i < n; i++) {
							int h = hs.get(i);
							locEHrel.add(e, h);
							String hstr = ((Quad) domH.get(h)).toVerboseStr();
							locEHout.println("#" + hstr);
						}
					}
				}
			}
		}
		accEout.close();
		escEout.close();
		locEHout.close();
		accErel.save();
		escErel.save();
		locEHrel.save();
	}

	private String eStr(int e) {
		return ("[" + e + "] ") + (e >= 0 ? domE.get(e).toLocStr() : "-1");
	}

	private String hStr(int h) {
		return ("[" + h + "] ") + (h >= 0 ? ((Quad) domH.get(h)).toLocStr() : "-1");
	}

	@Override
	public void processNewOrNewArray(int h, int t, int o) {
		if (verbose) System.out.println(t + " NEW " + hStr(h) + " o=" + o);
		if (o == 0)
			return;
		assert (!escObjs.contains(o));
		assert (!objToFldObjsFwd.containsKey(o));
		assert (!objToFldObjsInv.containsKey(o));
		assert (!objToHid.containsKey(o));
		if (h >= 0) {
			accH[h] = true;
			objToHid.put(o, h);
		}
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) { 
		if (verbose) System.out.println(t + " GETFLD_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) { 
		if (verbose) System.out.println(t + " ALOAD_PRI " + eStr(e));
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) { 
		if (verbose) System.out.println(t + " GETFLD_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) { 
		if (verbose) System.out.println(t + " ALOAD_REF " + eStr(e));
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println(t + " PUTFLD_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println(t + " ASTORE_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println(t + " PUTFLD_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapWr(e, b, f, o);
	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println(t + " ASTORE_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapWr(e, b, smashArrayElems ? 0 : i, o);
	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		if (verbose) System.out.println(t + " PUTSTATIC_REF " + eStr(e));
		if (o != 0)
			markAndPropEsc(o);
	}

	@Override
	public void processThreadStart(int p, int t, int o) { 
		if (verbose) System.out.println(t + " START " + p);
		if (o != 0)
			markAndPropEsc(o);
	}

	private void computeInvTC(int b) {
		int h = objToHid.get(b);
		if (h != 0 && tmp.add(h)) {
			List<FldObj> foList = objToFldObjsInv.get(b);
			if (foList != null) {
				int n = foList.size();
				for (int i = 0; i < n; i++) {
					FldObj fo = foList.get(i);
					computeInvTC(fo.o);
				}
			}
		}
	}

	private void processHeapRd(int e, int b) {
		if (!chkE[e] || escE[e])
			return;
		accE[e] = true;
		if (b == 0)
			return;
		if (escObjs.contains(b))
			escE[e] = true;
		else {
			computeInvTC(b);
			IntArraySet hs = locEH[e];
			if (hs == null) {
				hs = new IntArraySet();
				locEH[e] = hs;
			}
			hs.addAll(tmp);
			tmp.clear();
		}
	}

	private void removeInv(int rOld, int f, int b) {
		List<FldObj> inv = objToFldObjsInv.get(rOld);
		assert (inv != null);
		int n = inv.size();
		for (int i = 0; i < n; i++) {
			FldObj fo = inv.get(i);
			if (fo.f == f && fo.o == b) {
				inv.remove(i);
				return;
			}
		}
		assert (false);
	}

	private void processHeapWr(int e, int b, int f, int r) {
		processHeapRd(e, b);
		if (b == 0 || f < 0)
			return;
		if (r == 0) {
			if (!doStrongUpdates)
				return;
			// this is a strong update; so remove field f if it is there
			List<FldObj> fwd = objToFldObjsFwd.get(b);
			if (fwd == null)
				return;
			int rOld = -1;
			int n = fwd.size();
			for (int i = 0; i < n; i++) {
				FldObj fo = fwd.get(i);
				if (fo.f == f) {
					removeInv(fo.o, f, b);
					fwd.remove(i);
					break;
				}
			}
			return;
		}
		List<FldObj> fwd = objToFldObjsFwd.get(b);
		boolean added = false;
		if (fwd == null) {
			fwd = new ArrayList<FldObj>();
			objToFldObjsFwd.put(b, fwd);
		} else if (doStrongUpdates) {
			int n = fwd.size();
			for (int i = 0; i < n; i++) {
				FldObj fo = fwd.get(i);
				if (fo.f == f) {
					removeInv(fo.o, f, b);
					fo.o = r;
					added = true;
					break;
				}
			}
		} else {
			// do not add to fwd if already there;
			// since fwd is a list as opposed to a set, this
			// check must be done explicitly
			int n = fwd.size();
			for (int i = 0; i < n; i++) {
				FldObj fo = fwd.get(i);
				if (fo.f == f && fo.o == r) {
					added = true;
					break;
				}
			}
		}
		if (!added)
			fwd.add(new FldObj(f, r));
		List<FldObj> inv = objToFldObjsInv.get(r);
		if (inv == null) {
			inv = new ArrayList<FldObj>();
			objToFldObjsInv.put(r, inv);
		}
		boolean found = false;
		int n = inv.size();
		for (int i = 0; i < n; i++) {
			FldObj fo = inv.get(i);
			if (fo.f == f && fo.o == b) {
				found = true;
				break;
			}
		}
		if (!found)
			inv.add(new FldObj(f, b));
		if (escObjs.contains(b))
			markAndPropEsc(r);
	}

    private void markAndPropEsc(int o) {
		if (escObjs.add(o)) {
			List<FldObj> l = objToFldObjsFwd.get(o);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
		}
	}
}

