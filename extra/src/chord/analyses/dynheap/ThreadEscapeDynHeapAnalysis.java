/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.dynheap;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


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
	name = "thresc-heap-java"
)
public class ThreadEscapeDynHeapAnalysis extends DynamicAnalysis {
	private final boolean verbose = true;
	private final boolean doStrongUpdates = true;
	private final boolean smashArrayElems = false;
	private boolean[] chkE;
	private boolean[] accE;
	private boolean[] escE;
	private boolean [] printE;
	private TIntHashSet escObjs;
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;
    private TIntIntHashMap objToHid;
    private InstrScheme instrScheme;
	private DomH domH;
	private DomE domE;
	private int numH, numE;
	private	PrintWriter heapPrinter;
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
		Random random;
		if (smashArrayElems)
			assert (!doStrongUpdates);
		escObjs = new TIntHashSet();
		objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
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
		printE = new boolean[numE];
		random = new Random();
		for (int i=0; i<numE; i++) {
			printE[i] = random.nextBoolean();
		}
		heapPrinter = OutDirUtils.newPrintWriter("heap.dot"); 
	}

	@Override
	public void initPass() {
		escObjs.clear();
		objToFldObjs.clear();
		objToHid.clear();
	}

	@Override
	public void doneAllPasses() {
		PrintWriter accEout = OutDirUtils.newPrintWriter("shape_pathAccE.txt");
		PrintWriter escEout = OutDirUtils.newPrintWriter("shape_pathEscE.txt");
		PrintWriter locEout = OutDirUtils.newPrintWriter("shape_pathLocE.txt");
		for (int e = 0; e < numE; e++) {
			if (accE[e]) {
				String estr = domE.get(e).toVerboseStr();
				accEout.println(estr);
				if (escE[e]) {
					escEout.println(estr);
				} else {
					locEout.println(estr);
				}
			}
		}
		accEout.close();
		escEout.close();
		locEout.close();
		heapPrinter.close();
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
		assert (!objToFldObjs.containsKey(o));
		assert (!objToHid.containsKey(o));
		if (h >= 0) {
			objToHid.put(o, h);
		}
		//printHeap();
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) { 
		if (verbose) System.out.println(t + " GETFLD_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();
	}

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) { 
		if (verbose) System.out.println(t + " ALOAD_PRI " + eStr(e));
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) { 
		if (verbose) System.out.println(t + " GETFLD_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) { 
		if (verbose) System.out.println(t + " ALOAD_REF " + eStr(e));
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();

	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println(t + " PUTFLD_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();

	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println(t + " ASTORE_PRI " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapRd(e, b);
		//printHeap();

	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println(t + " PUTFLD_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapWr(e, b, f, o);
		//printHeap();

	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println(t + " ASTORE_REF " + eStr(e) + " b=" + b);
		if (e >= 0)
			processHeapWr(e, b, smashArrayElems ? 0 : i, o);
		//printHeap();

	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		if (verbose) System.out.println(t + " PUTSTATIC_REF " + eStr(e));
		if (o != 0)
			markAndPropEsc(o);
		//printHeap();
	}

	@Override
	public void processThreadStart(int p, int t, int o) { 
		if (verbose) System.out.println(t + " START " + p);
		if (o != 0)
			markAndPropEsc(o);
		//printHeap();
	}

	private void processHeapRd(int e, int b) {
		if (!chkE[e] || escE[e])
			return;
		accE[e] = true;
		if (b == 0)
			return;
		if (escObjs.contains(b))
			escE[e] = true;
		if (printE[e])
		{
			printHeap();
		}
	}

	private void processHeapWr(int e, int b, int f, int r) {
		processHeapRd(e, b);
		if (b == 0 || f < 0)
			return;
		if (r == 0) {
			if (!doStrongUpdates)
				return;
			// this is a strong update; so remove field f if it is there
			List<FldObj> fwd = objToFldObjs.get(b);
			if (fwd == null)
				return;
			int rOld = -1;
			int n = fwd.size();
			for (int i = 0; i < n; i++) {
				FldObj fo = fwd.get(i);
				if (fo.f == f) {
					fwd.remove(i);
					break;
				}
			}
			return;
		}
		List<FldObj> fwd = objToFldObjs.get(b);
		boolean added = false;
		if (fwd == null) {
			fwd = new ArrayList<FldObj>();
			objToFldObjs.put(b, fwd);
		} else if (doStrongUpdates) {
			int n = fwd.size();
			for (int i = 0; i < n; i++) {
				FldObj fo = fwd.get(i);
				if (fo.f == f) {
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
		if (escObjs.contains(b))
			markAndPropEsc(r);
	}

	private void printHeap()
	{	
	
		heapPrinter.println("digraph G {");	
		int[] objs = objToFldObjs.keys();
		for (int i=0; i<objs.length; i++)
		{
			int o = objs[i];
			String s;
			if (escObjs.contains(o)) {
				s = " escaped\",style=filled,color=red];";
			} else {
				s = " local\",style=filled,color=green];";				
			}
			heapPrinter.println(o+"[label=\"obj "+o+s);
			List<FldObj> fwd = objToFldObjs.get(o);
			for (int j=0; j<fwd.size(); j++) {
				FldObj f = fwd.get(j);
				if (objToFldObjs.get(f.o) == null)
				{
					heapPrinter.println(f.o+"[label=\"obj "+f.o+s);
				}
				heapPrinter.println(o+"->"+f.o+"[label= \" f"+f.f+"\"];");
			}
			
		}

		heapPrinter.println("}");
	}

    private void markAndPropEsc(int o) {
		if (escObjs.add(o)) {
			List<FldObj> l = objToFldObjs.get(o);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
		}
	}
}

