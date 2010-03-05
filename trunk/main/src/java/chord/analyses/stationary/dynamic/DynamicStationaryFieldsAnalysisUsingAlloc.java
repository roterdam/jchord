/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.stationary.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import chord.doms.DomF;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Properties;
import chord.project.analyses.DynamicAnalysis;
import chord.util.ChordRuntimeException;

/**
 * Dynamic stationary-fields analysis, where field <code>f</code> is said to be stationary if
 * all writes to <code>f</code> precede all reads of <code>f</code>, and concrete objects are
 * abstracted by their allocation site. 
 * 
 * TODO: We might want to extend this definition to array cells.
 * 
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 */
@Chord(
	name = "dynamic-statfld-alloc-java"
)
public class DynamicStationaryFieldsAnalysisUsingAlloc extends DynamicAnalysis {
	
	private InstrScheme instrScheme;
	private final TIntObjectHashMap<TIntHashSet> obj2readFields = new TIntObjectHashMap<TIntHashSet>(16);
	private final TIntHashSet stationaryFields = new TIntHashSet();
	private final TIntHashSet accessedFields = new TIntHashSet();
	private final TIntIntHashMap obj2alloc = new TIntIntHashMap();
	
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setNewAndNewArrayEvent(true, false, true);
    	instrScheme.setGetfieldReferenceEvent(true, false, true, true, false);
    	instrScheme.setGetfieldPrimitiveEvent(true, false, true, true);
    	instrScheme.setPutfieldReferenceEvent(true, false, true, true, false);
    	instrScheme.setPutfieldPrimitiveEvent(true, false, true, true);
    	return instrScheme;
    }

    public void initAllPasses() {    	
    }
    
    public void initPass() {
    	obj2readFields.clear();
    }
    
	public void donePass() {
		System.out.println("***** STATS *****");
		int numAccessedFields = accessedFields.size();
		int numStationaryFields = stationaryFields.size();
		System.out.println("numAccessedFields: " + numAccessedFields);
		System.out.println("numStationaryFields: " + numStationaryFields);
	}

	public void doneAllPasses() {
		DomF domF = instrumentor.getDomF();
		String outDirName = Properties.outDirName;
		try {
			PrintWriter writer;
			TIntIterator it;
			writer = new PrintWriter(new FileWriter(new File(outDirName, "dynamic_accessedF.txt")));
			it = accessedFields.iterator();
			while (it.hasNext()) {
				int af = it.next();
				jq_Field f = (jq_Field) domF.get(af);
				jq_Class c = f.getDeclaringClass();
				String sign = c.getName() + "." + f.getName();
				String file = Program.getSourceFileName(c);
				writer.println(sign + "@" + file);
			}
			writer.close();
			writer = new PrintWriter(new FileWriter(new File(outDirName, "dynamic_stationaryF.txt")));
			it = stationaryFields.iterator();
			while (it.hasNext()) {
				int sf = it.next();
				jq_Field f = (jq_Field) domF.get(sf);
				jq_Class c = f.getDeclaringClass();
				String sign = c.getName() + "." + f.getName();
				String file = Program.getSourceFileName(c);
				writer.println(sign + "@" + file);
			}
			writer.close();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	
	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0 && h >= 0)
			obj2alloc.put(o, h);
	}
	
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (obj2alloc.containsKey(b)) {
			onFieldWrite(e, obj2alloc.get(b), f);
		}
	}

	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (obj2alloc.containsKey(b)) {
			onFieldWrite(e, obj2alloc.get(b), f);
		}
	}

	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (obj2alloc.containsKey(b)) {
			onFieldRead(e, obj2alloc.get(b), f);
		}
	}

	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (obj2alloc.containsKey(b)) {
			onFieldRead(e, obj2alloc.get(b), f);
		}
	}
	
	private void onFieldRead(int e, int b, int f) {
		if (e >= 0 && b != 0 && f != 0) {
			if (!accessedFields.contains(f)) {
				accessedFields.add(f);
				stationaryFields.add(f);
			}
			TIntHashSet S = obj2readFields.get(b);
			if (S == null) {
				S = new TIntHashSet();
				obj2readFields.put(b, S);
			}
			S.add(f);
		}
	}
	
	private void onFieldWrite(int e, int b, int f) {
		if (e >= 0 && b != 0 && f != 0) {
			if (!accessedFields.contains(f)) {
				accessedFields.add(f);
				stationaryFields.add(f);
			}
			if (obj2readFields.containsKey(b)) {
				TIntHashSet S = obj2readFields.get(b);
				if (S != null) {
					stationaryFields.remove(f);
				}
			}
		}		
	}
}
