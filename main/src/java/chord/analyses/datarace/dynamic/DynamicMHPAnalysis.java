/**
 * 
 */
package chord.analyses.datarace.dynamic;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TIntProcedure;
import gnu.trove.TLongHashSet;
import gnu.trove.TLongProcedure;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.bddbddb.Rel.IntPairIterable;
import chord.doms.DomE;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
@Chord(	name="dynamic-mhp-java",
		consumedNames = { "noLckSyncEE" },
		producedNames = { "mhpEE" }, 
		namesOfSigns = { "mhpEE" },
		signs = { "E0,E1:E0xE1" })
public class DynamicMHPAnalysis extends DynamicAnalysis {

	class ThrStrtAndEnd {
		long start = -1, end = -1;
	}
	
	private InstrScheme instrScheme;
	private Set<IntPair> mhp;
	private TIntObjectHashMap<ThrStrtAndEnd> t2startAndEnd;
	private TIntObjectHashMap<TLongHashSet> e2TCs;
	private TIntObjectHashMap<TIntHashSet> e2thrds;
	private ProgramRel relNoLckSync;
	private ProgramRel relMHP;
	private long tc;
	
	@Override
	public void initAllPasses() {
		mhp = new HashSet<IntPair>();
		t2startAndEnd = new TIntObjectHashMap<ThrStrtAndEnd>();
		e2TCs = new TIntObjectHashMap<TLongHashSet>();
		e2thrds = new TIntObjectHashMap<TIntHashSet>();
		relNoLckSync = (ProgramRel) Project.getTrgt("noLckSyncEE");
		relMHP =  (ProgramRel) Project.getTrgt("mhpEE");
	}
	
	@Override
	public void initPass() {
		mhp.clear();
		e2TCs.clear();
		e2thrds.clear();
		t2startAndEnd.clear();
		tc = 0;
	}
	
	@Override
	public void donePass() {
		final TIntObjectHashMap<TIntHashSet> e2liveThrds = new TIntObjectHashMap<TIntHashSet>();
		e2TCs.forEachEntry(new TIntObjectProcedure<TLongHashSet>() {
//			@Override
			public boolean execute(final int e, final TLongHashSet TCs) {
				t2startAndEnd.forEachEntry(new TIntObjectProcedure<ThrStrtAndEnd>() {
//					@Override
					public boolean execute(final int t, final ThrStrtAndEnd strtAndEnd) {
						TCs.forEach(new TLongProcedure() {
//							@Override
							public boolean execute(long tc) {
								if (strtAndEnd.start <= tc && strtAndEnd.end >= tc) {
									TIntHashSet liveThrds = e2liveThrds.get(e);
									if (liveThrds == null) e2liveThrds.put(e, liveThrds = new TIntHashSet());
									liveThrds.add(t);
									return false;
								}
								return true;
							}
						});
						return true;
					}
				});
				return true;
			}
		});
//		e2liveThrds.forEachEntry(new TIntObjectProcedure<TIntHashSet>() {
//			public boolean execute(int arg0, TIntHashSet arg1) {
//				DomE domE = instrumentor.getDomE();
//				Quad q = (Quad) domE.get(arg0);
//				String s = Program.v().toVerboseStr(q);
//				Messages.logAnon(s);
//				Messages.logAnon("\n\t" + Arrays.toString(arg1.toArray()));
//				return true;
//			}
//		});
		relNoLckSync.load();
		IntPairIterable racePairs = relNoLckSync.getAry2IntTuples();
		L: for (IntPair tmpPair : racePairs) {
			final IntPair pair = tmpPair;
			final TIntHashSet liveThrds1 = e2liveThrds.get(pair.idx0);
			final TIntHashSet liveThrds2 = e2liveThrds.get(pair.idx1);
			if (liveThrds1 == null || liveThrds2 == null) {
				continue L;
			}
			liveThrds1.forEach(new TIntProcedure() {
				boolean fstMatch = false;
//				@Override
				public boolean execute(int arg0) {
					if (liveThrds2.contains(arg0)) {
						if (fstMatch) {
							mhp.add(pair);
							return false;
						} else {
							fstMatch = true;
						}
					}
					return true;
				}
			});
		}
	}
	
	@Override
	public void doneAllPasses() {
		relMHP.zero();
		for (IntPair pair : mhp) {
			relMHP.add(pair.idx0, pair.idx1);
		}
		relMHP.save();
		DomE domE = (DomE) Project.getTrgt("E");
		Program program = Program.getProgram();
		PrintWriter writer =
			 OutDirUtils.newPrintWriter("dynamic_mhp.txt");
		for (IntPair pair : mhp) {
			Quad q1 = (Quad) domE.get(pair.idx0);
			String s1 = program.toVerboseStr(q1);
			Quad q2 = (Quad) domE.get(pair.idx1);
			String s2 = program.toVerboseStr(q2);
			writer.println(s1);
			writer.println("\t" + s2);
		}
		writer.close();
	}
		
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setQuadEvent();
//		instrScheme.setThreadStartEvent(true, true, false);
		instrScheme.setAloadPrimitiveEvent(true, true, true, false);
		instrScheme.setAloadReferenceEvent(true, true, true, true, false);
		instrScheme.setAstorePrimitiveEvent(true, true, true, false);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, false);
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, false);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, false);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, false);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, false);
		return instrScheme;
	}
	
	@Override
	public void processQuad(int p, int t) {
		tc += 1;
		ThrStrtAndEnd strtAndEnd = t2startAndEnd.get(t);
		if (strtAndEnd == null) t2startAndEnd.put(t, strtAndEnd = new ThrStrtAndEnd());
		if (strtAndEnd.start == -1) {
			strtAndEnd.start = tc;
		}
		strtAndEnd.end = tc;
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updateTcSet(e);
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updateTcSet(e);
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		updateTcSet(e);
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updateTcSet(e);
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updateTcSet(e);
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updateTcSet(e);
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updateTcSet(e);
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		updateTcSet(e);
	}

	private void updateTcSet(int e) {
		TLongHashSet TCs = e2TCs.get(e);
		if (TCs == null) e2TCs.put(e, TCs = new TLongHashSet());
		TCs.add(tc);
	}
}
