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
 * chord.dynamic.escape.flowins ([true|false]; default=false)
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
	// set of IDs of currently escaping concrete/abstract objects
    private TIntHashSet escObjs;

	// map from each object to a list of each non-null instance field
	// of reference type along with its value
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;

    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToHidx; 

    // map from the index in domain H of each alloc site not yet known
	// to be flow-ins. thread-escaping to the list of indices in
	// domain E of instance field/array deref sites that should become
    // flow-ins. thread-escaping if this alloc site becomes flow-ins.
	// thread-escaping
    // invariant: escH[h] = true => HidxToPendingEidxs[h] == null
    private TIntArrayList[] HidxToPendingEidxs;

    // escH[h] == true iff alloc site having index h in domain H
	// is flow-ins. thread-escaping
	private boolean[] escH;

	// accE[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] accE;

    // escE[e] == true iff:
	// 1. kind is flowSen and instance field/array deref site having
	//    index e in domain E is flow-sen. thread-escaping
    // 2. kind is flowIns and instance field/array deref site having
    //    index e in domain E is flow-ins. thread-escaping
	private boolean[] escE;

	private int numE;
	private int numH;
	private boolean isFlowIns;

    private InstrScheme instrScheme;

	private ProgramRel relAccE;
	private ProgramRel relEscE;

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
	public void run() {
		isFlowIns = System.getProperty(
			"chord.escape.dynamic.flowins", "false").equals("true");
		super.run();
	}

	@Override
	public void initAllPasses() {
		escObjs = new TIntHashSet();
		objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
		DomE domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);
		numE = domE.size();
		accE = new boolean[numE];
		chkE = new boolean[numE];
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

		escE = new boolean[numE];
		relAccE = (ProgramRel) ClassicProject.g().getTrgt("accE");
		relEscE = (ProgramRel) ClassicProject.g().getTrgt("escE");
		if (isFlowIns) {
			DomH domH = (DomH) ClassicProject.g().getTrgt("H");
			ClassicProject.g().runTask(domH);
			numH = domH.size();
			HidxToPendingEidxs = new TIntArrayList[numH];
			escH = new boolean[numH];
			objToHidx = new TIntIntHashMap();
		}
	}

	@Override
	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		if (isFlowIns) {
			for (int i = 0; i < numH; i++) {
				HidxToPendingEidxs[i] = null;
			}
			for (int i = 0; i < numH; i++)
				escH[i] = false;
			objToHidx.clear();
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
		relAccE.zero();
		relEscE.zero();
		for (int i = 0; i < numE; i++) {
			if (accE[i]) {
				relAccE.add(i);
				if (escE[i])
					relEscE.add(i);
			}
		}
		relAccE.save();
		relEscE.save();

		DomE domE = (DomE) ClassicProject.g().getTrgt("E");
		Program program = Program.g();
		PrintWriter writer1 =
			 OutDirUtils.newPrintWriter("dynamic_visitedE.txt");
		PrintWriter writer2 =
			OutDirUtils.newPrintWriter("dynamic_escE.txt");
		for (int i = 0; i < numE; i++) {
			if (accE[i]) {
				Quad q = domE.get(i);
				String s = q.toVerboseStr();
				writer1.println(s);
				if (escE[i])
					writer2.println(s);
			}
		}
		writer1.close();
		writer2.close();
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o == 0)
			return;
		objToFldObjs.remove(o);
		escObjs.remove(o);
		if (isFlowIns) {
			if (h >= 0)
				objToHidx.put(o, h);
			else
				objToHidx.remove(o);
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
			if (objToHidx.containsKey(b)) {
				int h = objToHidx.get(b);
				if (escH[h]) {
					escE[e] = true;
				} else {
					TIntArrayList list = HidxToPendingEidxs[h];
					if (list == null) {
						list = new TIntArrayList();
						HidxToPendingEidxs[h] = list;
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

	private void processHeapWr(int e, int b, int fIdx, int r) {
		processHeapRd(e, b);
		if (b == 0 || fIdx < 0)
			return;
		List<FldObj> l = objToFldObjs.get(b);
		if (r == 0) {
			// this is a strong update; so remove field fIdx if it is there
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					FldObj fo = l.get(i);
					if (fo.f == fIdx) {
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
				if (fo.f == fIdx) {
					fo.o = r;
					added = true;
					break;
				}
			}
		}
		if (!added)
			l.add(new FldObj(fIdx, r));
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
				if (objToHidx.containsKey(o)) {
					int h = objToHidx.get(o);
					markHesc(h);
				}
        	}
		}
	}

	private void markHesc(int h) {
		if (!escH[h]) {
			escH[h] = true;
			TIntArrayList l = HidxToPendingEidxs[h];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int e = l.get(i);
					escE[e] = true;
				}
				HidxToPendingEidxs[h] = null;
			}
		}
	}
}

