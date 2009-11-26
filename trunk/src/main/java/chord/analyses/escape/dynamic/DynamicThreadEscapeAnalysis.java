/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.dynamic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import chord.util.ChordRuntimeException;
import chord.project.Properties;
import chord.util.IndexMap;
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
		//			 "visitedL", "flosInsEscL", "flowSenEscL" 
	signs = { "E0", "E0", "E0" }
		// , "L0", "L0", "L0" }
)
public class DynamicThreadEscapeAnalysis extends DynamicAnalysis {
    // set of all currently escaping objects
    private TIntHashSet escObjs;
	// map from each object to a list containing each non-null-valued
	// instance field of reference type along with that value
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
    // isEidxFlowSenEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-sen. thread-escaping
	private boolean[] isEidxFlowSenEsc;
    // isEidxFlowInsEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-ins. thread-escaping
	private boolean[] isEidxFlowInsEsc;
	// isEidxVisited[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] isEidxVisited;

	protected ProgramRel relVisitedE;
	protected ProgramRel relFlowInsEscE;
	protected ProgramRel relFlowSenEscE;
	private int numE;

    // private TIntArrayList[] HidxToPendingLidxs;
	// private boolean[] isLidxFlowSenEsc;
	// private boolean[] isLidxFlowInsEsc;
	// private boolean[] isLidxVisited;
	// protected ProgramRel relVisitedL;
	// protected ProgramRel relFlowInsEscL;
	// protected ProgramRel relFlowSenEscL;
	// private int numL;

	private boolean isFlowIns = true;
	private boolean isFlowSen = true;

	private int numH;
	private boolean convert;

    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	boolean convert = System.getProperty(
    		"chord.convert", "false").equals("true");
    	if (convert)
    		instrScheme.setConvert();
    	instrScheme.setNewAndNewArrayEvent(true, false, true);
    	instrScheme.setPutstaticReferenceEvent(false, false, false, false, true);
    	instrScheme.setThreadStartEvent(false, false, true);

		// instrScheme.setAcquireLockEvent(true, false, true);
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
		numE = instrumentor.getEmap().size();
		// numL = instrumentor.getLmap().size();
		isEidxVisited = new boolean[numE];
		// isLidxVisited = new boolean[numL];
		if (convert) {
			relVisitedE = (ProgramRel) Project.getTrgt("visitedE");
			// relVisitedL = (ProgramRel) Project.getTrgt("visitedL");
		}
		if (isFlowIns) {
			isEidxFlowInsEsc = new boolean[numE];
			// isLidxFlowInsEsc = new boolean[numL];
			if (convert) {
				relFlowInsEscE =
					(ProgramRel) Project.getTrgt("flowInsEscE");
				// relFlowInsEscL =
				//	(ProgramRel) Project.getTrgt("flowInsEscL");
			}
			numH = instrumentor.getHmap().size();
			HidxToPendingEidxs = new TIntArrayList[numH];
			// HidxToPendingLidxs = new TIntArrayList[numH];
			isHidxEsc = new boolean[numH];
			objToHidx = new TIntIntHashMap();
		}
		if (isFlowSen) {
			isEidxFlowSenEsc = new boolean[numE];
			// isLidxFlowSenEsc = new boolean[numL];
			if (convert) {
 				relFlowSenEscE =
					(ProgramRel) Project.getTrgt("flowSenEscE");
 				// relFlowSenEscL =
				//	(ProgramRel) Project.getTrgt("flowSenEscL");
			}
		}
	}

	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		if (isFlowSen) {
			for (int i = 0; i < numH; i++) {
				HidxToPendingEidxs[i] = null;
				// HidxToPendingLidxs[i] = null;
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
		int numFlowInsOriginalEscE = 0;
		int numFlowInsAdjustedEscE = 0;
		int numFlowSenEscE = 0;
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i])
				numVisitedE++;
		}
		if (isFlowSen) {
			for (int i = 0; i < numE; i++) {
				if (isEidxFlowSenEsc[i]) {
					numFlowSenEscE++;
				}
			}
		}
		if (isFlowIns) {
			for (int i = 0; i < numH; i++) {
				if (isHidxEsc[i])
					numAllocEsc++;
			}
			for (int i = 0; i < numE; i++) {
				if (isEidxFlowInsEsc[i])
					numFlowInsOriginalEscE++;
				else if (isFlowSen && isEidxFlowSenEsc[i])
					numFlowInsAdjustedEscE++;
			}
			numFlowInsAdjustedEscE += numFlowInsOriginalEscE;
		}
		System.out.println("numAllocEsc: " + numAllocEsc);
		System.out.println("numVisitedE: " + numVisitedE +
			" numFlowSenEscE: " + numFlowSenEscE +
			" numFlowInsEscE (original): " + numFlowInsOriginalEscE +
			" numFlowInsEscE (adjusted): " + numFlowInsAdjustedEscE);

/*
		int numVisitedL = 0;
		int numFlowInsOriginalEscL = 0;
		int numFlowInsAdjustedEscL = 0;
		int numFlowSenEscL = 0;
		for (int i = 0; i < numL; i++) {
			if (isLidxVisited[i])
				numVisitedL++;
		}
		if (isFlowSen) {
			for (int i = 0; i < numL; i++) {
				if (isLidxFlowSenEsc[i]) {
					numFlowSenEscL++;
				}
			}
		}
		if (isFlowIns) {
			for (int i = 0; i < numL; i++) {
				if (isLidxFlowInsEsc[i])
					numFlowInsOriginalEscL++;
				else if (isFlowSen && isLidxFlowSenEsc[i])
					numFlowInsAdjustedEscL++;
			}
			numFlowInsAdjustedEscL += numFlowInsOriginalEscL;
		}
		System.out.println("numVisitedL: " + numVisitedL +
			" numFlowSenEscL: " + numFlowSenEscL +
			" numFlowInsEscL (original): " + numFlowInsOriginalEscL +
			" numFlowInsEscL (adjusted): " + numFlowInsAdjustedEscL);
*/
	}

	public void doneAllPasses() {
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

/*
			relVisitedL.zero();
			for (int i = 0; i < numL; i++) {
				if (isLidxVisited[i])
					relVisitedL.add(i);
			}
			relVisitedL.save();
			if (isFlowIns) {
				relFlowInsEscL.zero();
				for (int i = 0; i < numL; i++) {
					if (isLidxFlowInsEsc[i])
						relFlowInsEscL.add(i);
				}
				relFlowInsEscL.save();
			}
			if (isFlowSen) {
				relFlowSenEscL.zero();
				for (int i = 0; i < numL; i++) {
					if (isLidxFlowSenEsc[i])
						relFlowSenEscL.add(i);
				}
				relFlowSenEscL.save();
			}
*/
		}

		IndexMap<String> Emap = instrumentor.getEmap();
		IndexMap<String> Lmap = instrumentor.getLmap();
		String outDirName = Properties.outDirName;
		try {
			PrintWriter writer;
			writer = new PrintWriter(new FileWriter(
				new File(outDirName, "dynamic_visitedE.txt")));
			for (int i = 0; i < numE; i++) {
				if (isEidxVisited[i])
					writer.println(Emap.get(i));
			}
			writer.close();
/*
			writer = new PrintWriter(new FileWriter(
				new File(outDirName, "dynamic_visitedL.txt")));
			for (int i = 0; i < numL; i++) {
				if (isLidxVisited[i])
					writer.println(Lmap.get(i));
			}
			writer.close();
*/
			if (isFlowIns) {
				writer = new PrintWriter(new FileWriter(
					new File(outDirName, "dynamic_flowInsEscE.txt")));
				for (int i = 0; i < numE; i++) {
					if (isEidxFlowInsEsc[i])
						writer.println(Emap.get(i));
				}
				writer.close();
/*
				writer = new PrintWriter(new FileWriter(
					new File(outDirName, "dynamic_flowInsEscL.txt")));
				for (int i = 0; i < numL; i++) {
					if (isLidxFlowInsEsc[i])
						writer.println(Lmap.get(i));
				}
				writer.close();
*/
			}
			if (isFlowSen) {
				writer = new PrintWriter(new FileWriter(
					new File(outDirName, "dynamic_flowSenEscE.txt")));
				for (int i = 0; i < numE; i++) {
					if (isEidxFlowSenEsc[i])
						writer.println(Emap.get(i));
				}
				writer.close();
/*
				writer = new PrintWriter(new FileWriter(
					new File(outDirName, "dynamic_flowSenEscL.txt")));
				for (int i = 0; i < numL; i++) {
					if (isLidxFlowSenEsc[i])
						writer.println(Lmap.get(i));
				}
				writer.close();
*/
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
			if (isFlowIns) {
				objToHidx.remove(o);
				if (h >= 0)
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

	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		if (o != 0)
			markAndPropEsc(o);
	}
	public void processThreadStart(int p, int t, int o) { 
		if (o != 0)
			markAndPropEsc(o);
	}
/*
    public void processAcquireLock(int l, int t, int o) {
		if (l >= 0 && o != 0) {
			isLidxVisited[l] = true;
			if (isFlowSen) {
				if (!isLidxFlowSenEsc[l] && escObjs.contains(o))
					isLidxFlowSenEsc[l] = true;
			}
			if (isFlowIns) {
				if (!isLidxFlowInsEsc[l]) {
					if (objToHidx.containsKey(o)) {
						int h = objToHidx.get(o);
						if (isHidxEsc[h]) {
							isLidxFlowInsEsc[l] = true;
						} else {
							TIntArrayList list = HidxToPendingLidxs[h];
							if (list == null) {
								list = new TIntArrayList();
								HidxToPendingLidxs[h] = list;
								list.add(l);
							} else if (!list.contains(l)) {
								list.add(l);
							}
						}
					}
				}
			}
		}
	}
*/
	private void processHeapRd(int e, int b) {
		if (e >= 0 && b != 0) {
			isEidxVisited[e] = true;
			if (isFlowSen) {
				if (!isEidxFlowSenEsc[e] && escObjs.contains(b))
					isEidxFlowSenEsc[e] = true;
			}
			if (isFlowIns) {
				if (!isEidxFlowInsEsc[e]) {
					if (objToHidx.containsKey(b)) {
						int h = objToHidx.get(b);
						if (isHidxEsc[h]) {
							isEidxFlowInsEsc[e] = true;
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
				}
			}
		}
	}
	private void processHeapWr(int e, int b, int fIdx, int r) {
		processHeapRd(e, b);
		if (b != 0 && fIdx >= 0) {
			if (r == 0) {
				// remove field fIdx if it is there
				List<FldObj> l = objToFldObjs.get(b);
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
				List<FldObj> l = objToFldObjs.get(b);
				if (l == null) {
					l = new ArrayList<FldObj>();
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
	private void markHesc(int h) {
		if (!isHidxEsc[h]) {
			isHidxEsc[h] = true;
			TIntArrayList l = HidxToPendingEidxs[h];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int e = l.get(i);
					isEidxFlowInsEsc[e] = true;
				}
				HidxToPendingEidxs[h] = null;
			}
		}
	}
    private void markAndPropEsc(int o) {
        if (escObjs.add(o)) {
        	if (isFlowSen) {
				if (objToHidx.containsKey(o)) {
					int h = objToHidx.get(o);
					markHesc(h);
				}
        	}
			List<FldObj> l = objToFldObjs.get(o);
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
