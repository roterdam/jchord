package chord.analyses.escape.dynamic;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Collections;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomT;

import chord.util.IntArraySet;
import chord.util.ChordRuntimeException;
import chord.project.Properties;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.Project;
import chord.project.OutDirUtils;
import chord.program.Program;

import gnu.trove.TIntProcedure;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

@Chord(name = "dynamic-stats-java")
public class DynamicStatsAnalysis extends DynamicAnalysis {
	InstrScheme instrScheme;
	IndexMap<String> Mmap;
	IndexMap<String> Emap;
	IndexMap<String> Hmap;
	IndexMap<String> Imap;
	TIntIntHashMap objToHidx = new TIntIntHashMap();
	TIntHashSet visitedE = new TIntHashSet();
	TIntHashSet badE = new TIntHashSet();
	int numDynamicVisitedE, numDynamicBadE, numDynamicBadO;
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setNewAndNewArrayEvent(true, true, true);
		instrScheme.setEnterAndLeaveMethodEvent();
		instrScheme.setMethodCallEvent(true, true, true, true, true);
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
		Mmap = instrumentor.getMmap();
		Emap = instrumentor.getEmap();
		Hmap = instrumentor.getHmap();
		Imap = instrumentor.getImap();
	}
	public void processNewOrNewArray(int h, int t, int o) {
		assert (o >= 0);
        if (o == 0)
			numDynamicBadO++;
		assert (h != 0);	// 0 is a special value in domain H
        if (h > 0) {
			System.out.println("NEW: " + Hmap.get(h));
			objToHidx.put(o, h);
		} else
			objToHidx.remove(o);
	}
    public void processEnterMethod(int m, int t) {
		if (m >= 0) System.out.println("ENTER_METHOD " + Mmap.get(m));
    }
    public void processLeaveMethod(int m, int t) {
		if (m >= 0) System.out.println("LEAVE_METHOD " + Mmap.get(m));
    }
	public void processMethodCallBef(int i, int t, int o) {
		if (i >= 0) System.out.println("BEF CALL: " + Imap.get(i));
	}
	public void processMethodCallAft(int i, int t, int o) {
		if (i >= 0) System.out.println("AFT CALL: " + Imap.get(i));
	}
    public void processGetfieldPrimitive(int e, int t, int b, int f) {
		processHeapRd(e, b);
    }
    public void processAloadPrimitive(int e, int t, int b, int i) {
		processHeapRd(e, b);
    }
    public void processGetfieldReference(int e, int t, int b, int f, int o) {
		processHeapRd(e, b);
    }
    public void processAloadReference(int e, int t, int b, int i, int o) {
		processHeapRd(e, b);
    }
    public void processPutfieldPrimitive(int e, int t, int b, int f) {
		processHeapRd(e, b);
    }
    public void processAstorePrimitive(int e, int t, int b, int i) {
		processHeapRd(e, b);
    }
    public void processPutfieldReference(int e, int t, int b, int f, int o) {
		processHeapWr(e, b, f, o);
    }
    public void processAstoreReference(int e, int t, int b, int i, int o) {
		processHeapWr(e, b, i, o);
	}
	private void processHeapRd(int e, int b) {
		assert (b >= 0);	// allow null object value
		if (e >= 0) {
			visitedE.add(e);
			numDynamicVisitedE++;
			if (b > 0 && !objToHidx.containsKey(b)) {
				if (badE.add(e))
					System.out.println("BAD: " + Emap.get(e));
				numDynamicBadE++;
			}
		}
    }
	private void processHeapWr(int e, int b, int i, int o) {
		assert (b >= 0);	// allow null object value
		assert (o >= 0);	// allow null object value
		if (e >= 0) {
			visitedE.add(e);
			numDynamicVisitedE++;
			if ((b > 0 && !objToHidx.containsKey(b)) ||
			 	(o > 0 && !objToHidx.containsKey(o))) {
				if (badE.add(e))
					System.out.println("BAD: " + Emap.get(e));
				numDynamicBadE++;
			}
		}
    }
	public void doneAllPasses() {
		OutDirUtils.logOut("Visited statements without allocation site: %d/%d static and %d/%d dynamic",
			badE.size(), visitedE.size(), numDynamicBadE, numDynamicVisitedE);
		OutDirUtils.logOut("# dynamic bad objects: " + numDynamicBadO);
		final TIntProcedure procedure = new TIntProcedure() {
			public boolean execute(int e) {
				OutDirUtils.logOut(Emap.get(e));
				return true;
			}
		};
		badE.forEach(procedure);
	}
}
