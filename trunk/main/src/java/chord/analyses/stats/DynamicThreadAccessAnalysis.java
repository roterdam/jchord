package chord.analyses.stats;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;
import chord.util.IndexMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import chord.util.IndexMap;
import chord.util.ChordRuntimeException;
import chord.project.analyses.ProgramDom;
import chord.project.Messages;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomM;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Properties;
import chord.project.OutDirUtils;
import chord.project.Project;

@Chord(name = "dynamic-thracc-java")
public class DynamicThreadAccessAnalysis extends DynamicAnalysis {
	InstrScheme instrScheme;
	IndexMap<String> Emap;
	IndexMap<String> Hmap;
	IndexMap<String> Mmap;
	IndexMap<String> Imap;
	IndexMap<String> Fmap;
	DomE domE;
	DomH domH;
	DomM domM;
	DomI domI;
	DomF domF;

    protected <T> IndexMap<String> getUniqueStringMap(ProgramDom<T> dom) {
        IndexMap<String> map = new IndexMap<String>(dom.size());
        for (int i = 0; i < dom.size(); i++) {
            String s = dom.toUniqueString(dom.get(i));
            if (map.contains(s))
                throw new ChordRuntimeException(Messages.get("INSTR.DUPLICATE_IN_DOMAIN"));
            map.getOrAdd(s);
        }
        return map;
    }

	TIntIntHashMap o2hMap = new TIntIntHashMap();
	// objects that have been accessed by at most one thread so far (the thread denoted by the map)
	TIntIntHashMap o2tMap = new TIntIntHashMap();
	// objects that have been accessed by more than one thread
	TIntHashSet mto = new TIntHashSet();
	TIntObjectHashMap<TIntHashSet> e2oMap = new TIntObjectHashMap<TIntHashSet>();
	boolean excludedE[];
	boolean excludedF[];
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setAloadPrimitiveEvent(true, true, true, true);
		instrScheme.setAstorePrimitiveEvent(true, true, true, true);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setAloadReferenceEvent(true, true, true, true, true);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, true);

		instrScheme.setPutstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setNewAndNewArrayEvent(true, true, true);
		instrScheme.setAcquireLockEvent(true, true, true);
		instrScheme.setThreadStartEvent(true, true, true);
		instrScheme.setMethodCallEvent(true, true, true, true, true);
		// instrScheme.setEnterAndLeaveMethodEvent();
		return instrScheme;
	}
	public void initAllPasses() {
		domF = (DomF) Project.getTrgt("F");
		Project.runTask(domF);
		Fmap = getUniqueStringMap(domF);

		domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);
		Mmap = getUniqueStringMap(domM);

		domH = (DomH) Project.getTrgt("H");
		Project.runTask(domH);
		Hmap = getUniqueStringMap(domH);

		domE = (DomE) Project.getTrgt("E");
		Project.runTask(domE);
		Emap = getUniqueStringMap(domE);

		domI = (DomI) Project.getTrgt("I");
		Project.runTask(domI);
		Imap = getUniqueStringMap(domI);

		Set<jq_Class> excludedClasses = new HashSet<jq_Class>();

		String[] checkExcludedPrefixes = Properties.toArray(Properties.checkExcludeStr);
		Program program = Program.v();
		for (jq_Class c : program.getPreparedClasses()) {
			String cName = c.getName();
			for (String prefix : checkExcludedPrefixes) {
				if (cName.startsWith(prefix))
					excludedClasses.add(c);
			}
		}

		int numE = domE.size();
		excludedE = new boolean[numE];
		for (int e = 0; e < numE; e++) {
			Quad q = (Quad) domE.get(e);
			jq_Class c = program.getMethod(q).getDeclaringClass();
			if (excludedClasses.contains(c))
				excludedE[e] = true;
		}

		int numF = domF.size();
		excludedF = new boolean[numF];
		for (int f = 1; f < numF; f++) {
			jq_Field fld = domF.get(f);
			jq_Class c = fld.getDeclaringClass();
			if (excludedClasses.contains(c))
				excludedF[f] = true;
		}
	}
	public void processNewOrNewArray(int h, int t, int o) {
		assert (o >= 0);
		assert (h != 0);	// 0 is a special value in domain H
        if (o == 0) return;
        if (h > 0)
			o2hMap.put(o, h);
	}
    public void processPutstaticReference(int e, int t, int b, int f, int o) { }

/*
    public void processEnterMethod(int m, int t) {
		if (m >= 0) System.out.println("ENTER_METHOD " + Mmap.get(m) + " " + t);
    }
    public void processLeaveMethod(int m, int t) {
		if (m >= 0) System.out.println("LEAVE_METHOD " + Mmap.get(m) + " " + t);
    }
*/
	public void processMethodCallBef(int i, int t, int o) {
		// if (i >= 0) System.out.println("BEF CALL: " + Imap.get(i));
	}
	public void processMethodCallAft(int i, int t, int o) {
		// if (i >= 0) System.out.println("AFT CALL: " + Imap.get(i));
	}
    public void processGetfieldPrimitive(int e, int t, int b, int f) {
		assert(b >= 0);
		if (e < 0 || f < 0 || b == 0 || isIgnore(b)) return;
		processHeapRd(e, b, t);
    }
    public void processAloadPrimitive(int e, int t, int b, int i) {
		assert(b >= 0);
		if (e < 0 || i < 0 || b == 0 || isIgnore(b)) return;
		processHeapRd(e, b, t);
    }
    public void processGetfieldReference(int e, int t, int b, int f, int o) {
		assert(b >= 0 && o >= 0);
		if (e < 0 || f < 0 || b == 0 || isIgnore(b) || isIgnore(o)) return;
		processHeapRd(e, b, t);
    }
    public void processAloadReference(int e, int t, int b, int i, int o) {
		assert(b >= 0 && o >= 0);
		if (e < 0 || i < 0 || b == 0 || isIgnore(b) || isIgnore(o)) return;
		processHeapRd(e, b, t);
    }
    public void processPutfieldPrimitive(int e, int t, int b, int f) {
		assert(b >= 0);
		if (e < 0 || f < 0 || b == 0 || isIgnore(b)) return;
		processHeapRd(e, b, t);
    }
    public void processAstorePrimitive(int e, int t, int b, int i) {
		assert(b >= 0);
		if (e < 0 || i < 0 || b == 0 || isIgnore(b)) return;
		processHeapRd(e, b, t);
    }
    public void processPutfieldReference(int e, int t, int b, int f, int o) {
		assert(b >= 0 && o >= 0);
		if (e < 0 || f < 0 || b == 0 || isIgnore(b) || isIgnore(o)) return;
		processHeapRd(e, b, t);
    }
    public void processAstoreReference(int e, int t, int b, int i, int o) {
		assert(b >= 0 && o >= 0);
		if (e < 0 || i < 0 || b == 0 || isIgnore(b) || isIgnore(o)) return;
		processHeapRd(e, b, t);
	}
	private void processHeapRd(int e, int b, int t) {
		assert (b > 0);
		assert (t > 0);
		if (!o2hMap.containsKey(b))
			return;
		TIntHashSet oSet = e2oMap.get(e);
		if (oSet == null) {
			oSet = new TIntHashSet();
			e2oMap.put(e, oSet);
		}
		oSet.add(b);
		int t2 = o2tMap.get(b);
		if (t2 == 0) {
			// b is being dereferenced for the first time by some thread in an e
			o2tMap.put(b, t);
		} else if (t2 != t)
			mto.add(b);
    }
	public void processAcquireLock(int l, int t, int o) { }
	public void processThreadStart(int i, int t, int o) { }

	public void doneAllPasses() {
		final TIntProcedure procedure = new TIntProcedure() {
			public boolean execute(int o) {
				if (mto.contains(o))
					return false;
				return true;
			}
		};
		System.out.println("START THREAD ACCESS");
		int numExcluded = 0;
		int numThracced = 0;
		int[] keys = e2oMap.keys();
		for (int e : keys) {
			if (excludedE[e]) {
				numExcluded++;
				continue;
			}
			TIntHashSet oSet = e2oMap.get(e);
			if (!oSet.forEach(procedure)) {
				System.out.println(Emap.get(e));
				numThracced++;
			}
		}
		System.out.println("END THREAD ACCESS");
		System.out.println("NUM VISITED: " + keys.length);
		System.out.println("NUM EXCLUDED: " + numExcluded);
		System.out.println("NUM THRACCED: " + numThracced);
	}
    private boolean isIgnore(int o) {
		return o > 0 && !o2hMap.containsKey(o);
    }
}
