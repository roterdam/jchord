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
	private static boolean doStrongUpdates = true;
	private static boolean smashArrayElems = false;

	// set of IDs of currently escaping concrete/abstract objects
	private TIntHashSet escObjs;

	// map from each object to a list of each non-null instance field
	// of reference type along with its value
	private TIntObjectHashMap<List<FldObj>> objToFldObjsFwd;
	private TIntObjectHashMap<List<FldObj>> objToFldObjsInv;

    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToHid; 

	private boolean[] chkE;

	// accE[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] accE;

    // escE[e] == true iff instance field/array deref site having
	// index e in domain E is thread-escaping
	private boolean[] escE;

	private IntArraySet[] locEH;

	private DomH domH;
	private DomE domE;
	private int numH, numE;

    private InstrScheme instrScheme;

	private final IntArraySet tmp = new IntArraySet();

	@Override
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
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
		accE = new boolean[numE];
		escE = new boolean[numE];
		locEH = new IntArraySet[numE];
		ProgramRel relCheckExcludedE =
			(ProgramRel) ClassicProject.g().getTrgt("checkExcludedE");
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
		ProgramRel relAccE  = (ProgramRel) ClassicProject.g().getTrgt("accE");
		ProgramRel relEscE  = (ProgramRel) ClassicProject.g().getTrgt("escE");
		ProgramRel relLocEH = (ProgramRel) ClassicProject.g().getTrgt("locEH");

		relAccE.zero();
		relEscE.zero();
		relLocEH.zero();

		PrintWriter accEout  = OutDirUtils.newPrintWriter("shape_pathAccE.txt");
		PrintWriter escEout  = OutDirUtils.newPrintWriter("shape_pathEscE.txt");
		PrintWriter locEHout = OutDirUtils.newPrintWriter("shape_pathLocEH.txt");
		for (int e = 0; e < numE; e++) {
			if (accE[e]) {
				relAccE.add(e);
				String estr = domE.get(e).toLocStr();
				accEout.println(estr);
				if (escE[e]) {
					relEscE.add(e);
					escEout.println(estr);
				} else {
					IntArraySet hs = locEH[e];
					locEHout.println(estr);
					if (hs != null) {
						int n = hs.size();
						for (int i = 0; i < n; i++) {
							int h = hs.get(i);
							relLocEH.add(e, h);
							String hstr = ((Quad) domH.get(h)).toLocStr();
							locEHout.println("#" + hstr);
						}
					}
				}
			}
		}

		accEout.close();
		escEout.close();
		locEHout.close();

		relAccE.save();
		relEscE.save();
		relLocEH.save();
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o == 0)
			return;
		assert (!escObjs.contains(o));
		assert (!objToFldObjsFwd.containsKey(o));
		assert (!objToFldObjsInv.containsKey(o));
		assert (!objToHid.containsKey(o));
		if (h >= 0)
			objToHid.put(o, h);
	}

	public void processGetfieldPrimitive(int e, int t, int b, int f) { 
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processAloadPrimitive(int e, int t, int b, int i) { 
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processGetfieldReference(int e, int t, int b, int f, int o) { 
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processAloadReference(int e, int t, int b, int i, int o) { 
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (e >= 0)
			processHeapRd(e, b);
	}

	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (e >= 0)
			processHeapWr(e, b, f, o);
	}

	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (e >= 0)
			processHeapWr(e, b, smashArrayElems ? 0 : i, o);
	}

	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		if (o != 0)
			markAndPropEsc(o);
	}
	public void processThreadStart(int p, int t, int o) { 
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

