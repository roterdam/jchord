/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.escape.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Project;
import chord.project.ChordProperties;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.ChordRuntimeException;
import chord.doms.DomH;
import chord.doms.DomE;

/**
 * Dynamic thread-escape analysis where concrete objects are abstracted using
 * allocation sites.
 * 
 * Outputs the following relations:
 * <ul>
 * <li><tt>visitedE</tt> containing each heap-accessing statement that was
 * visited at least once.</li>
 * <li><tt>escE</tt> containing those visited heap-accessing statements that
 * were deemed to access thread-escaping data by the chosen thread-escape
 * analysis.</li>
 * </ul>
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "dynamic-thresc-alloc-java",
	   namesOfSigns = { "visitedE", "escE" },
	   signs = { "E0", "E0" })
public class DynamicThreadEscapeAnalysisUsingAlloc extends DynamicAnalysis {
	// set of IDs of currently escaping concrete/abstract objects
	private TIntHashSet escObjs;

	// map from each object to a list of each non-null instance field
	// of reference type along with its value
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;

	// map from each object to the index in domain H of its alloc site
	private TIntIntHashMap objToHidx;

	// having index e in domain E is visited during the execution
	private boolean[] isEidxVisited;

	// isEidxEsc[e] == true iff:
	// 1. kind is flowSen and instance field/array deref site having
	// index e in domain E is flow-sen. thread-escaping
	// 2. kind is flowIns and instance field/array deref site having
	// index e in domain E is flow-ins. thread-escaping
	private boolean[] isEidxEsc;

	private int numE;

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

	public void initAllPasses() {
		escObjs = new TIntHashSet();
		objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
		objToHidx = new TIntIntHashMap();
		numE = ((DomE) Project.getTrgt("E")).size();
		isEidxVisited = new boolean[numE];
		isEidxEsc = new boolean[numE];
		relVisitedE = (ProgramRel) Project.getTrgt("visitedE");
		relEscE = (ProgramRel) Project.getTrgt("escE");
	}

	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		objToHidx.clear();
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
		System.out.println("numAllocEsc: " + numAllocEsc);
		System.out.println("numVisitedE: " + numVisitedE + " numEscE: " + numEscE);
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
		String outDirName = ChordProperties.outDirName;
		try {
			PrintWriter writer;
			writer = new PrintWriter(new FileWriter(new File(outDirName,
					"dynamic_visitedE.txt")));
			for (int i = 0; i < numE; i++) {
				if (isEidxVisited[i])
					writer.println(domE.toUniqueString(i));
			}
			writer.close();
			writer = new PrintWriter(new FileWriter(new File(outDirName,
					"dynamic_escE.txt")));
			for (int i = 0; i < numE; i++) {
				if (isEidxEsc[i])
					writer.println(domE.toUniqueString(i));
			}
			writer.close();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0 && h >= 0) {
			objToHidx.put(o, h);
			objToFldObjs.remove(h);
		}
	}

	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processAloadPrimitive(int e, int t, int b, int i) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processAloadReference(int e, int t, int b, int i, int o) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (e >= 0 && objToHidx.containsKey(b))
			processHeapRd(e, objToHidx.get(b));
	}

	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (e >= 0 && objToHidx.containsKey(b) && (objToHidx.containsKey(o) || o == 0))
			processHeapWr(e, objToHidx.get(b), f, o == 0 ? 0 : objToHidx.get(o));
	}

	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (e >= 0 && objToHidx.containsKey(b) && (objToHidx.containsKey(o) || o == 0))
			processHeapWr(e, objToHidx.get(b), i, o == 0 ? 0 : objToHidx.get(o));
	}

	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (o != 0 && objToHidx.containsKey(o))
			markAndPropEsc(objToHidx.get(o));
	}

	public void processThreadStart(int p, int t, int o) {
		if (o != 0 && objToHidx.containsKey(o))
			markAndPropEsc(objToHidx.get(o));
	}

	private void processHeapRd(int e, int h) {
		if (isEidxEsc[e])
			return;
		isEidxVisited[e] = true;
		if (escObjs.contains(h))
			isEidxEsc[e] = true;
	}

	private void processHeapWr(int e, int h, int fIdx, int r) {
		processHeapRd(e, h);
		if (h == 0 || fIdx < 0)
			return;
		List<FldObj> l = objToFldObjs.get(h);
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
			objToFldObjs.put(h, l);
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
		if (escObjs.contains(h))
			markAndPropEsc(r);
	}


	private void markAndPropEsc(int h) {
		if (escObjs.add(h)) {
			List<FldObj> l = objToFldObjs.get(h);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
		}
	}
}
