/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.dynamic;

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

/**
 * Dynamic thread-escape analysis.
 * 
 * Outputs the following relations:
 *
 * 1. accE containing each heap-accessing statement that was visited at least once.
 * 2. escE containing those visited heap-accessing statements that were deemed to
 *    access thread-escaping data by the chosen thread-escape analysis.
 *
 * Recognized system properties:
 *
 * chord.escape.flowins ([true|false]; default=false)
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thresc-dynamic-java",
	produces = { "escE" }, 
	namesOfSigns = { "accE", "escE" },
	signs = { "E0", "E0" }
)
public class DynamicThreadEscapeAnalysis extends DynamicAnalysis {
	private final boolean isFlowIns = System.getProperty("chord.escape.flowins", "false").equals("true");
    private boolean[] chkE;	// true iff heap access stmt with index e in domE must be checked
    private boolean[] accE;	// true iff heap access stmt with index e in domE is visited
    private boolean[] escE;	// true iff heap access stmt with index e in domE thread-escapes
    private boolean[] accH;	// true iff alloc site with index h in domH is visited
	private boolean[] escH;	// true iff alloc site with index h in domH thread-escapes
    private TIntHashSet escObjs;	// set of IDs of currently escaping objects/alloc sites
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;	// map from each object to list of each non-null instance field of ref type with its value
    private TIntIntHashMap objToHid;	// map from each object to the index in domH of its alloc site
    private TIntArrayList[] HidToPendingEids;	// map from index in domH of each alloc site not yet known to be escaping to list of indices in domE of
												// stmts that should escape if this alloc site escapes; escH[h] = true => HidToPendingEids[h] == null
    private InstrScheme instrScheme;
	private DomE domE;
	private DomH domH;
	private int numE, numH;

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
		escObjs = new TIntHashSet();
		objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
		domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);
		numE = domE.size();
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
		if (isFlowIns) {
			DomH domH = (DomH) ClassicProject.g().getTrgt("H");
			ClassicProject.g().runTask(domH);
			numH = domH.size();
			HidToPendingEids = new TIntArrayList[numH];
			escH = new boolean[numH];
			objToHid = new TIntIntHashMap();
		}
	}

	@Override
	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		if (isFlowIns) {
			for (int i = 0; i < numH; i++)
				HidToPendingEids[i] = null;
			for (int i = 0; i < numH; i++)
				escH[i] = false;
			objToHid.clear();
		}
	}

	@Override
	public void donePass() {
		System.out.println("***** STATS *****");
		int numEscH = 0;
		int numAccE = 0;
		int numEscE = 0;
		for (int i = 0; i < numE; i++) {
			if (accE[i]) {
				numAccE++;
				if (escE[i])
					numEscE++;
			}
		}
		if (isFlowIns) {
			for (int i = 0; i < numH; i++) {
				if (escH[i])
					numEscH++;
			}
		}
		System.out.println("numEscH: " + numEscH);
		System.out.println("numAccE: " + numAccE);
		System.out.println("numEscE: " + numEscE);
	}

	@Override
	public void doneAllPasses() {
		ProgramRel  accErel = (ProgramRel) ClassicProject.g().getTrgt("accE");
		ProgramRel  escErel = (ProgramRel) ClassicProject.g().getTrgt("escE");
		PrintWriter accEout = OutDirUtils.newPrintWriter("dynamic_accE.txt");
		PrintWriter escEout = OutDirUtils.newPrintWriter("dynamic_escE.txt");
		accErel.zero();
		escErel.zero();
		for (int i = 0; i < numE; i++) {
			if (accE[i]) {
				String s = domE.get(i).toVerboseStr();
				accEout.println(s);
				accErel.add(i);
				if (escE[i]) {
					escEout.println(s);
					escErel.add(i);
				}
			}
		}
		accErel.save();
		escErel.save();
		accEout.close();
		escEout.close();
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o == 0)
			return;
		objToFldObjs.remove(o);
		escObjs.remove(o);
		if (isFlowIns) {
			if (h >= 0)
				objToHid.put(o, h);
			else
				objToHid.remove(o);
		}
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
			processHeapWr(e, b, i, o);
	}

	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		if (o != 0)
			markAndPropEsc(o);
	}
	public void processThreadStart(int p, int t, int o) { 
		if (o != 0)
			markAndPropEsc(o);
	}

	private void processHeapRd(int e, int b) {
		if (!chkE[e] || escE[e])
			return;
		accE[e] = true;
		if (b == 0)
			return;
		if (isFlowIns) {
			if (objToHid.containsKey(b)) {
				int h = objToHid.get(b);
				if (escH[h]) {
					escE[e] = true;
				} else {
					TIntArrayList list = HidToPendingEids[h];
					if (list == null) {
						list = new TIntArrayList();
						HidToPendingEids[h] = list;
						list.add(e);
					} else if (!list.contains(e)) {
						list.add(e);
					}
				}
			}
		} else {
			if (escObjs.contains(b))
				escE[e] = true;
		}
	}

	private void processHeapWr(int e, int b, int fId, int r) {
		processHeapRd(e, b);
		if (b == 0 || fId < 0)
			return;
		List<FldObj> l = objToFldObjs.get(b);
		if (r == 0) {
			// this is a strong update; so remove field fId if it is there
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					FldObj fo = l.get(i);
					if (fo.f == fId) {
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
				if (fo.f == fId) {
					fo.o = r;
					added = true;
					break;
				}
			}
		}
		if (!added)
			l.add(new FldObj(fId, r));
		if (escObjs.contains(b))
			markAndPropEsc(r);
	}


    private void markAndPropEsc(int o) {
		if (escObjs.add(o)) {
			List<FldObj> l = objToFldObjs.get(o);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
        	if (isFlowIns) {
				if (objToHid.containsKey(o)) {
					int h = objToHid.get(o);
					markHesc(h);
				}
        	}
		}
	}

	private void markHesc(int h) {
		if (!escH[h]) {
			escH[h] = true;
			TIntArrayList l = HidToPendingEids[h];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int e = l.get(i);
					escE[e] = true;
				}
				HidToPendingEids[h] = null;
			}
		}
	}
}

