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
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;

/**
 * Dynamic thread-escape analysis.
 * 
 * Outputs the following relations:
 * <ul>
 * <li><tt>visitedE</tt> containing each heap-accessing statement 
 * that was visited at least once.</li>
 * <li><tt>escE</tt> containing those visited heap-accessing
 * statements that were deemed to access thread-escaping data by
 * the chosen thread-escape analysis.</li>
 * </ul>
 * Recognized system properties:
 * <ul>
 * <li><tt>chord.dynamic.escape.flowins</tt> ([true|false]; default=false)</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dynamic-thresc-java",
	producedNames = { "escE" }, 
	namesOfSigns = { "visitedE", "escE" },
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
    // invariant: isHidxEsc[h] = true => HidxToPendingEidxs[h] == null
    private TIntArrayList[] HidxToPendingEidxs;

    // isHidxEsc[h] == true iff alloc site having index h in domain H
	// is flow-ins. thread-escaping
	private boolean[] isHidxEsc;

	// isEidxVisited[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] isEidxVisited;

    // isEidxEsc[e] == true iff:
	// 1. kind is flowSen and instance field/array deref site having
	//    index e in domain E is flow-sen. thread-escaping
    // 2. kind is flowIns and instance field/array deref site having
    //    index e in domain E is flow-ins. thread-escaping
	private boolean[] isEidxEsc;

	private int numE;
	private int numH;
	private boolean isFlowIns;

    private InstrScheme instrScheme;

	private ProgramRel relVisitedE;
	private ProgramRel relEscE;

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

	public void run() {
		isFlowIns = System.getProperty(
			"chord.escape.dynamic.flowins", "false").equals("true");
		super.run();
	}

	public void initAllPasses() {
		escObjs = new TIntHashSet();
		objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
		DomE domE = (DomE) Project.getTrgt("E");
		Project.runTask(domE);
		numE = domE.size();
		isEidxVisited = new boolean[numE];
		isEidxEsc = new boolean[numE];
		relVisitedE = (ProgramRel) Project.getTrgt("visitedE");
		relEscE = (ProgramRel) Project.getTrgt("escE");
		if (isFlowIns) {
			DomH domH = (DomH) Project.getTrgt("H");
			Project.runTask(domH);
			numH = domH.size();
			HidxToPendingEidxs = new TIntArrayList[numH];
			isHidxEsc = new boolean[numH];
			objToHidx = new TIntIntHashMap();
		}
	}

	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		if (isFlowIns) {
			for (int i = 0; i < numH; i++) {
				HidxToPendingEidxs[i] = null;
			}
			for (int i = 0; i < numH; i++)
				isHidxEsc[i] = false;
			objToHidx.clear();
		}
	}

	public void donePass() {
		System.out.println("***** STATS *****");
		int numAllocEsc = 0;
		int numVisitedE = 0;
		int numEscE = 0;
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i]) {
				numVisitedE++;
				if (isEidxEsc[i])
					numEscE++;
			}
		}
		if (isFlowIns) {
			for (int i = 0; i < numH; i++) {
				if (isHidxEsc[i])
					numAllocEsc++;
			}
		}
		System.out.println("numAllocEsc: " + numAllocEsc);
		System.out.println("numVisitedE: " + numVisitedE +
			" numEscE: " + numEscE);
	}

	public void doneAllPasses() {
		relVisitedE.zero();
		relEscE.zero();
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i]) {
				relVisitedE.add(i);
				if (isEidxEsc[i])
					relEscE.add(i);
			}
		}
		relVisitedE.save();
		relEscE.save();

		DomE domE = (DomE) Project.getTrgt("E");
		Program program = Program.getProgram();
		PrintWriter writer1 =
			 OutDirUtils.newPrintWriter("dynamic_visitedE.txt");
		PrintWriter writer2 =
			OutDirUtils.newPrintWriter("dynamic_escE.txt");
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i]) {
				Quad q = domE.get(i);
				String s = q.toVerboseStr();
				writer1.println(s);
				if (isEidxEsc[i])
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
		if (isEidxEsc[e])
			return;
		isEidxVisited[e] = true;
		if (b == 0)
			return;
		if (isFlowIns) {
			if (objToHidx.containsKey(b)) {
				int h = objToHidx.get(b);
				if (isHidxEsc[h]) {
					isEidxEsc[e] = true;
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
				isEidxEsc[e] = true;
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
		if (!isHidxEsc[h]) {
			isHidxEsc[h] = true;
			TIntArrayList l = HidxToPendingEidxs[h];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int e = l.get(i);
					isEidxEsc[e] = true;
				}
				HidxToPendingEidxs[h] = null;
			}
		}
	}
}

class FldObj {
    public int f;
    public int o;
    public FldObj(int f, int o) {
		this.f = f;
		this.o = o;
	}
}

