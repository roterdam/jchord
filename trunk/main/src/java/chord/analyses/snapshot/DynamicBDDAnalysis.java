/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.ChordProperties;
import chord.project.Project;
import chord.program.Program;
import chord.project.analyses.DynamicAnalysis;
import chord.doms.DomH;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDException;
import net.sf.javabdd.BDDFactory;

@Chord(name = "dynamic-bdd-java")
public class DynamicBDDAnalysis extends DynamicAnalysis {
	private static final boolean sound = false;
	private static final boolean DEBUG = false;
	private static final int NUM_FLDS = 3000;
	private static final int NUM_OBJS = 40000;
	InstrScheme instrScheme;
	TIntObjectHashMap<List<FldObj>> objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
	TIntIntHashMap objToHidx = new TIntIntHashMap();
	DomH domH;
	int numDoms;
	int H0_IDX, F0_IDX, O0_IDX, O1_IDX;
	String[] domNames;
	int[] domSizes;
    int[] domIdxs;
    BDDFactory factory;
    BDDDomain[] domBdds;
	BDD/*O0,F0,O1*/ heapBdd;
	BDD/*O0,H0*/ initLabelsBdd;  // same as objToHidx
	BDD/*O0,H0*/ currLabelsBdd;
	BDD h0dom, o0dom, o1dom, f0dom;
	BDDPairing pair;
	// BDD iterLabelsBdd;

	public void initAllPasses() {
		domH = (DomH) Project.getTrgt("H");
		String varOrder;
		if (sound) {
			numDoms = 4;
			H0_IDX = 0; O0_IDX = 1; O1_IDX = 2; F0_IDX = 3;
			domSizes = new int[] { domH.size(), NUM_OBJS, NUM_OBJS, NUM_FLDS };
			domNames = new String[] { "H0", "O0", "O1", "F0" };
			varOrder = "O0xH0_O1_F0";
		} else {
			H0_IDX = 0; O0_IDX = 1; O1_IDX = 2;
			numDoms = 3;
			domSizes = new int[] { domH.size(), NUM_OBJS, NUM_OBJS };
			domNames = new String[] { "H0", "O0", "O1" };
			// varOrder = "O0_H0_O1";
			// varOrder = "O0_O1_H0";
			// varOrder = "H0_O0_O1";
			// varOrder = "H0_O1_O0";
			// varOrder = "O1_H0_O0"; TRIED; consumes less memory but slow
			// varOrder = "O1_O0_H0";
			// varOrder = "H0_O0xO1"; TRIED
			// varOrder = "O0_H0xO1"; 
			// varOrder = "O1_H0xO0";
			// varOrder = "O0xH0_O1"; 
			// varOrder = "O1xH0_O0"; TRIED: consumes less memory but slow
			varOrder = "O0xO1_H0"; 
			// varOrder = "O0xH0xO1";
		}

        int bddnodes = Integer.parseInt(System.getProperty("bddnodes", "500000"));
        int bddcache = Integer.parseInt(System.getProperty("bddcache", "125000"));
        double bddminfree = Double.parseDouble(System.getProperty("bddminfree", ".20"));
        factory = BDDFactory.init("java", bddnodes, bddcache);
        factory.setIncreaseFactor(2);
        factory.setMinFreeNodes(bddminfree);
        domBdds = new BDDDomain[numDoms];
        domIdxs = new int[numDoms];
		for (int i = 0; i < numDoms; i++) {
			String name = domNames[i];
            int numElems = domSizes[i];
            if (numElems == 0)
                numElems = 1;
            BDDDomain d = factory.extDomain(new long[] { numElems })[0];
            d.setName(name);
            domBdds[i] = d;
            domIdxs[i] = d.getIndex();
			assert(domIdxs[i] == i);
        }
        boolean reverseLocal = System.getProperty("bddreverse","true").equals("true");
        int[] order = factory.makeVarOrdering(reverseLocal, varOrder);
        factory.setVarOrder(order);
		if (sound)
			f0dom = domBdds[F0_IDX].set();
		o0dom = domBdds[O0_IDX].set();
		o1dom = domBdds[O1_IDX].set();
		h0dom = domBdds[H0_IDX].set();
		pair = factory.makePair();
		pair.set(domBdds[O1_IDX], domBdds[O0_IDX]);
		// iterLabelsBdd = factory.one();
		// iterLabelsBdd.andWith(o0dom.id());
		// iterLabelsBdd.andWith(h0dom.id());
	}

	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setNewAndNewArrayEvent(true, true, true);
		instrScheme.setPutfieldReferenceEvent(true, false, true, true, true);
		instrScheme.setAstoreReferenceEvent(true, false, true, true, true);
		return instrScheme;
	}
	public void initPass() {
        objToFldObjs.clear();
        objToHidx.clear();
		heapBdd = factory.zero();
		initLabelsBdd = factory.zero();
		currLabelsBdd = factory.zero();
	}
	public void processNewOrNewArray(int h, int t, int o) {
		assert (o >= 0);
		assert (h != 0);	// 0 is a special value in domain H
		if (o == 0 || h < 0)
			return;
		assert (o < NUM_OBJS);
		if (DEBUG) System.out.println("NEW: h=" + h + " o=" + o);
		objToHidx.put(o, h);
		try {
			initLabelsBdd.orWith(
				domBdds[H0_IDX].ithVar(h).andWith(
				domBdds[O0_IDX].ithVar(o)));
			currLabelsBdd.orWith(
				domBdds[H0_IDX].ithVar(h).andWith(
				domBdds[O0_IDX].ithVar(o)));
		} catch (BDDException ex) {
			throw new RuntimeException(ex);
		}
	}
    public void processPutfieldReference(int e, int t, int b, int f, int o) {
		processHeapWr(e, b, f, o, t);
    }
    public void processAstoreReference(int e, int t, int b, int i, int o) {
		processHeapWr(e, b, i, o, t);
	}
	private void processHeapWr(int e, int b, int f, int r, int t) {
		assert (b >= 0);	// allow null object value
		assert (r >= 0);	// allow null object value
		// if (e < 0) return;
		if (f < 0)
			return;
		if (b == 0 || !objToHidx.containsKey(b))
			return;
		if (r != 0 && !objToHidx.containsKey(r))
			return;
		assert (f < NUM_FLDS);
		assert (b < NUM_OBJS);
		assert (r < NUM_OBJS);
		if (DEBUG) System.out.println("processHeapWr: b=" + b + " f=" + f + " r=" + r);
        List<FldObj> foList = objToFldObjs.get(b);
        if (r == 0) {
			// don't insert edge (b,f,null)
			// remove edge (b,f,o) if present
            if (foList != null) {
                int n = foList.size();
                for (int i = 0; i < n; i++) {
                    FldObj fo = foList.get(i);
                    if (fo.f == f) {
                        foList.remove(i);
						removeEdge(b, f, fo.o);
						recomputeLabels();
						break;
                    }
                }
            }
        } else {
			// insert edge (b,f,r)
			// remove edge (b,f,o) if present
			boolean doAdd = true;
			if (foList == null) {
				foList = new ArrayList<FldObj>();
				objToFldObjs.put(b, foList);
			} else {
				for (FldObj fo : foList) {
					if (fo.f == f) {
						int oldR = fo.o;
						if (oldR == r) {
							doAdd = false;
							break;
						}
						removeEdge(b, f, oldR);
						insertEdge(b, f, r);
						fo.o = r;
						recomputeLabels();
						doAdd = false;
						break;
					}
				}
			}
			if (doAdd) {
				foList.add(new FldObj(f, r));
				insertEdge(b, f, r);
				propagateLabels();
			}
		}
    }
	private void propagateLabels() {
		if (DEBUG) System.out.println("Propagate called");
		BDD quantifiedHeapBdd;
		if (sound) {
			// step 1: quantify out attribute f from heap
			quantifiedHeapBdd = heapBdd.exist(f0dom);
		} else
			quantifiedHeapBdd = heapBdd.id();
		int count = 0;
		while (true) {
			count++;
			// step 2: if currLabels contains (o1,h) and
			// quantifiedHeap contains (o1,o2) then
			// add (o2,h) to currLabels
			BDD nextLabelsBdd = currLabelsBdd.relprod(quantifiedHeapBdd, o0dom);
			nextLabelsBdd.replaceWith(pair);
			BDD tempLabelsBdd = currLabelsBdd.id();
			nextLabelsBdd.orWith(tempLabelsBdd);  // tempLabelsBdd is consumed
			if (nextLabelsBdd.equals(currLabelsBdd)) {
				nextLabelsBdd.free();
				break;
			}
			currLabelsBdd.free();
			currLabelsBdd = nextLabelsBdd;
		} 
		System.out.println("Iter: " + count);
		quantifiedHeapBdd.free();
	}
	private void recomputeLabels() {
		System.out.println("Recompute called");
		currLabelsBdd = initLabelsBdd.id();
		propagateLabels();
	}
	private void insertEdge(int b, int f, int r) {
		if (DEBUG) System.out.println("Insert: " + b + " " + f + " " + r);
		try {
			if (sound) {
				heapBdd.orWith(
					domBdds[O0_IDX].ithVar(b).andWith(
					domBdds[F0_IDX].ithVar(f).andWith(
					domBdds[O1_IDX].ithVar(r))));
			} else {
				heapBdd.orWith(
					domBdds[O0_IDX].ithVar(b).andWith(
					domBdds[O1_IDX].ithVar(r)));
			}
		} catch (BDDException ex) {
			throw new RuntimeException(ex);
		}
	}
	private void removeEdge(int b, int f, int r) {
		if (DEBUG) System.out.println("Remove: " + b + " " + f + " " + r);
		try {
			if (sound) {
				heapBdd.andWith(
					domBdds[O0_IDX].ithVar(b).andWith(
					domBdds[F0_IDX].ithVar(f).andWith(
					domBdds[O1_IDX].ithVar(r))).not());
			} else {
				heapBdd.andWith(
					domBdds[O0_IDX].ithVar(b).andWith(
					domBdds[O1_IDX].ithVar(r)).not());
			}
		} catch (BDDException ex) {
			throw new RuntimeException(ex);
		}
	}
	public void donePass() {
		try {
			printDoms();
			if (sound)
				saveBdd("heap", new int[] { O0_IDX, O1_IDX, F0_IDX }, heapBdd);
			else
				saveBdd("heap", new int[] { O0_IDX, O1_IDX }, heapBdd);
			saveBdd("initLabels", new int[] { H0_IDX, O0_IDX }, initLabelsBdd);
			saveBdd("currLabels", new int[] { H0_IDX, O0_IDX }, currLabelsBdd);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
	}
	private void printDoms() throws IOException {
		for (int i = 0; i < numDoms; i++) {
        	String fileName = domNames[i] + ".dom";
        	File file = new File(fileName);
			PrintWriter out = new PrintWriter(file);
			int size = domSizes[i];
			out.println(size);
			out.close();
		}
	}
	private void saveBdd(String name, int[] idxs, BDD bdd) throws IOException {
		File file = new File(ChordProperties.bddbddbWorkDirName, name + ".bdd");
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write('#');
		for (int idx : idxs) {
			BDDDomain d = domBdds[idx];
			out.write(" " + d + ":" + d.varNum());
		}
		out.write('\n');
		for (int idx : idxs) {
			BDDDomain d = domBdds[idx];
			out.write('#');
			for (int v : d.vars())
				out.write(" " + v);
			out.write('\n');
		}
		factory.save(out, bdd);
		out.close();
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
