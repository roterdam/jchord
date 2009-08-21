/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.thread.escape;

import java.util.List;
import java.util.ArrayList;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.project.ProgramRel;
import chord.project.Project;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

// todo: specify output rels in @Chord annotation
// and enable user to enable isFlowSen/isFlowIns

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dynamic-thresc-java",
	namesOfSigns = { "visitedE", "flowInsEscE", "flowSenEscE" },
	signs = { "E0", "E0", "E0" }
)
public class DynamicThreadEscapeAnalysis extends DynamicAnalysis {
	// map from each object to a list containing each non-null-valued
	// instance field of reference type along with that value
	private TIntObjectHashMap objToFldObjs;
    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToHidx; 
    // set of all currently escaping objects
    private TIntHashSet escObjs;
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
    // isEidxFlowSenEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-sen. thread-escaping
	private boolean[] isEidxFlowSenEsc;
    // isEidxFlowInsEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-ins. thread-escaping
	private boolean[] isEidxFlowInsEsc;
	// isEidxVisited[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] isEidxVisited;

	private boolean isFlowIns = true;
	private boolean isFlowSen = true;

	protected ProgramRel relVisitedE;
	protected ProgramRel relFlowInsEscE;
	protected ProgramRel relFlowSenEscE;

	public boolean handlesNewOrNewArray() { return true; }
	public boolean handlesInstFldRd() { return true; }
	public boolean handlesInstFldWr() { return true; }
	public boolean handlesAryElemRd() { return true; }
	public boolean handlesAryElemWr() { return true; }
	public boolean handlesStatFldWr() { return true; }
	public boolean handlesThreadSpawnAndStart() { return true; }

	private boolean isFirst = true;
	private int numE;
	private int numH;

    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setNewAndNewArrayEvent(true, false, true);
    	instrScheme.setPutstaticReferenceEvent(false, false, false, true);

    	instrScheme.setGetfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setGetfieldReferenceEvent(true, false, true, false, false);

    	instrScheme.setPutfieldPrimitiveEvent(true, false, true, false);
    	instrScheme.setPutfieldReferenceEvent(true, false, true, true, true);

    	instrScheme.setAloadPrimitiveEvent(true, false, true, false);
    	instrScheme.setAloadReferenceEvent(true, false, true, false, false);

    	instrScheme.setAstorePrimitiveEvent(true, false, true, false);
    	instrScheme.setAstoreReferenceEvent(true, false, true, true, true);

    	instrScheme.setThreadStartEvent(false, false, true);
    	return instrScheme;
    }

	public void initPass() {
		if (isFirst) {
			isFirst = false;
 			escObjs = new TIntHashSet();
			numE = Emap.size();
			isEidxVisited = new boolean[numE];
			if (convert)
				relVisitedE = (ProgramRel) Project.getTrgt("visitedE");
			if (isFlowIns) {
				isEidxFlowInsEsc = new boolean[numE];
				if (convert) {
					relFlowInsEscE =
						(ProgramRel) Project.getTrgt("flowInsEscE");
				}
			}
			if (isFlowSen) {
				isEidxFlowSenEsc = new boolean[numE];
				numH = Hmap.size();
				HidxToPendingEidxs = new TIntArrayList[numH];
				isHidxEsc = new boolean[numH];
				if (convert) {
 					relFlowSenEscE =
						(ProgramRel) Project.getTrgt("flowSenEscE");
				}
			}
		} else {
			printStats();
			escObjs.clear();
			if (isFlowSen) {
				for (int i = 0; i < numH; i++)
					HidxToPendingEidxs[i] = null;
				for (int i = 0; i < numH; i++)
					isHidxEsc[i] = false;
			}
		}
		objToFldObjs = new TIntObjectHashMap();
		if (isFlowSen)
			objToHidx = new TIntIntHashMap();
	}
	public void done() {
		if (convert) {
			relVisitedE.zero();
			for (int i = 0; i < numE; i++) {
				if (isEidxVisited[i])
					relVisitedE.add(i);
			}
			relVisitedE.save();
			if (isFlowIns) {
				relFlowInsEscE.zero();
				for (int i = 0; i < numE; i++) {
					if (isEidxFlowInsEsc[i])
						relFlowInsEscE.add(i);
				}
				relFlowInsEscE.save();
			}
			if (isFlowSen) {
				relFlowSenEscE.zero();
				for (int i = 0; i < numE; i++) {
					if (isEidxFlowSenEsc[i])
						relFlowSenEscE.add(i);
				}
				relFlowSenEscE.save();
			}
		}
		printStats();
	}

	private void printStats() {
		System.out.println("***** STATS *****");
		int numVisited = 0, numFlowInsEsc = 0, numFlowSenEsc = 0,
			numAllocEsc = 0;
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i])
				numVisited++;
		}
		if (isFlowSen) {
			for (int i = 0; i < numE; i++) {
				if (isEidxFlowSenEsc[i]) {
					numFlowSenEsc++;
				}
			}
		}
		if (isFlowIns) {
			for (int i = 0; i < numE; i++) {
				if (isEidxFlowInsEsc[i])
					numFlowInsEsc++;
			}
			for (int i = 0; i < numH; i++) {
				if (isHidxEsc[i])
					numAllocEsc++;
			}
		}
		System.out.println("numVisited: " + numVisited +
			(isFlowSen ? " numFlowSenEsc: " + numFlowSenEsc : "") +
			(isFlowIns ? " numFlowInsEsc: " + numFlowInsEsc +
				" numAllocEsc: " + numAllocEsc : ""));
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
			if (isFlowIns) {
				objToHidx.remove(o);
				if (h != -1)
					objToHidx.put(o, h);
			}
		}
	}
	public void processGetfieldReference(int e, int t, int b, int f, int o) { 
		processHeapRd(e, b);
	}
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		processHeapWr(e, b, f, o);
	}
	public void processAloadReference(int e, int t, int b, int i, int o) { 
		processHeapRd(e, b);
	}
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		processHeapWr(e, b, i, o);
	}

	public void processGetfieldPrimitive(int e, int t, int b, int f) { 
		processHeapRd(e, b);
	}
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		processHeapRd(e, b);
	}
	public void processAloadPrimitive(int e, int t, int b, int i) { 
		processHeapRd(e, b);
	}
	public void processAstorePrimitive(int e, int t, int b, int i) {
		processHeapRd(e, b);
	}

	public void processPutstaticReference(int e, int t, int f, int o) { 
		if (o != 0) {
			markAndPropEsc(o);
		}
	}
	public void processThreadStart(int p, int t, int o) { 
		if (o != 0) {
			markAndPropEsc(o);
		}
	}
	private void processHeapRd(int eIdx, int b) {
		if (eIdx != -1 && b != 0) {
			isEidxVisited[eIdx] = true;
			if (isFlowSen) {
				if (!isEidxFlowSenEsc[eIdx] && escObjs.contains(b)) {
					isEidxFlowSenEsc[eIdx] = true;
				}
			}
			if (isFlowIns) {
				if (!isEidxFlowInsEsc[eIdx]) {
					if (objToHidx.containsKey(b)) {
						int hIdx = objToHidx.get(b);
						if (isHidxEsc[hIdx]) {
							isEidxFlowInsEsc[eIdx] = true;
						} else {
							TIntArrayList l = HidxToPendingEidxs[hIdx];
							if (l == null) {
								l = new TIntArrayList();
								HidxToPendingEidxs[hIdx] = l;
								l.add(eIdx);
							} else if (!l.contains(eIdx)) {
								l.add(eIdx);
							}
						}
					}
				}
			}
		}
	}
	private void processHeapWr(int eIdx, int b, int fIdx, int r) {
		processHeapRd(eIdx, b);
		if (b != 0 && fIdx != -1) {
			if (r == 0) {
				// remove field fIdx if it is there
				List l = (List) objToFldObjs.get(b);
				if (l != null) {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = (FldObj) l.get(i);
						if (fo.f == fIdx) {
							l.remove(i);
							return;
						}
					}
				}
			} else {
				List l = (List) objToFldObjs.get(b);
				if (l == null) {
					l = new ArrayList();
					objToFldObjs.put(b, l);
				} else {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = (FldObj) l.get(i);
						if (fo.f == fIdx) {
							fo.o = r;
							return;
						}
					}
				}
				l.add(new FldObj(fIdx, r));
				if (escObjs.contains(b))
					markAndPropEsc(r);
			}
		}
	}
	private void markHesc(int hIdx) {
		if (!isHidxEsc[hIdx]) {
			isHidxEsc[hIdx] = true;
			TIntArrayList l = HidxToPendingEidxs[hIdx];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int eIdx = l.get(i);
					isEidxFlowInsEsc[eIdx] = true;
				}
				HidxToPendingEidxs[hIdx] = null;
			}
		}
	}
    private void markAndPropEsc(int o) {
        if (escObjs.add(o)) {
        	if (isFlowSen) {
				if (objToHidx.containsKey(o)) {
					int hIdx = objToHidx.get(o);
					markHesc(hIdx);
				}
        	}
			List l = (List) objToFldObjs.get(o);
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					FldObj fo = (FldObj) l.get(i);
					markAndPropEsc(fo.o);
				}
			}
		}
	}
}

class FldObj {
    public int f;
    public int o;
    public FldObj(int f, int o) { this.f = f; this.o = o; }
}