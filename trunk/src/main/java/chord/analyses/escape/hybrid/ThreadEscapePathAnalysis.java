package chord.analyses.escape.hybrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

import chord.program.Program;
import chord.project.Properties;
import chord.util.ChordRuntimeException;
import chord.util.IndexSet;
import chord.util.ArraySet;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.util.ProcessExecutor;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntQuad;
import chord.util.tuple.object.Pair;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.IntPairIterable;
import chord.bddbddb.Rel.IntTrioIterable;
import chord.doms.DomH;
import chord.doms.DomE;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomF;
import chord.doms.DomP;
import chord.doms.DomB;
import chord.doms.DomT;
import chord.project.Project;
import chord.project.ProgramDom;
import chord.project.ProgramRel;
import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.instr.InstrScheme;
import chord.instr.Instrumentor;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.*;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.*;
import joeq.Compiler.Quad.Operator.Return.*;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Util.Templates.ListIterator;

@Chord(
    name = "thresc-path-java",
    namesOfSigns = { "EH" },
    signs = { "E0,H0:E0_H0" }
)
public class ThreadEscapePathAnalysis extends DynamicAnalysis {
	public final static int REDIRECTED = -1;
	public final static int NULL_Q_VAL = 0;
	public final static int NULL_U_VAL = -2;
	private final static int NUMQ_ESTIMATE = 100000;

	/***** for tracking statistics *****/
	private long numNew;
	private long numNewArray;
	private long numGetfield;
	private long numPutfield;
	private long numAload;
	private long numAstore;
	private long numPhi;
	private long numMove;
	private long numCheckCast;
	private long numInvk;
	private long numRet;
	private long numGetstatic;
	private long numPutstatic;

	private final static int MAX_CALLS = 2;
	private final static boolean DEBUG = false;
	private final static List<IntPair> emptyArgsList =
		Collections.emptyList();
	private final static TIntArrayList emptyTmpsList =
		new TIntArrayList(0);

    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
        if (instrScheme != null)
            return instrScheme;
        instrScheme = new InstrScheme();
		instrScheme.setConvert();
		instrScheme.setBasicBlockEvent();
		instrScheme.setQuadEvent();
		instrScheme.setEnterAndLeaveMethodEvent();
        return instrScheme;
    }

    /***** data structures set once for all runs *****/
    // set of heap insts deemed escaping in some run so far
	private Set<Quad> escHeapInsts;
   	private Map<Quad, Set<Quad>> heapInstToAllocInsts;
	private Map<Set<Quad>, Set<Quad>> allocInstsToHeapInsts;
	private int[] methToFstP;
	private int[] methToNumP;
	private boolean[] isIgnoredMeth;
	private String[] methToSign;
	private List/*IntPair*/[] methToArgs;
	private TIntArrayList[] methToTmps;
	private InvkInfo[] invkToInfo;
	private int[] startInvks;
	private int numStartInvks;

	/***** data structures set once for each run *****/
	private Set<Quad> done = new HashSet<Quad>();
	private boolean[] isDoneMeth;
	private TIntObjectHashMap<ThreadHandler> threadToHandlerMap =
		new TIntObjectHashMap<ThreadHandler>();

	private DomF domF;
	private DomM domM;
	private DomH domH;
	private DomP domP;
	private DomB domB;
	private DomT domT;
	private ProgramDom<IntPair> domQ;
	private ProgramDom<Register> domU;
	private Set<IntTrio> allocSet;
	private Set<IntPair> asgnSet;
	private Set<IntTrio> copyPset;
	private Set<IntTrio> copyQset;
	private Set<IntQuad> getinstSet;
	private Set<IntQuad> putinstSet;
	private Set<IntPair> getstatSet;
	private Set<IntPair> putstatSet;
	private Set<IntPair> spawnSet;
	private Set<IntPair> startSet;
	private Set<IntPair> basePUset;
	private Set<IntPair> PQset;
	private int[] succ;
	private int[][] succs;
	private IndexMap<String> cIdMap;

	public Map<Set<Quad>, Set<Quad>> getAllocInstsToHeapInstsMap() {
		return allocInstsToHeapInsts;
	}

    public void initAllPasses() {
		domF = (DomF) Project.getTrgt("F");
		Project.runTask(domF);
		domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);
		domH = (DomH) Project.getTrgt("H");
		Project.runTask(domH);
		domP = (DomP) Project.getTrgt("P");
		Project.runTask(domP);
		int numP = domP.size();
		domB = (DomB) Project.getTrgt("B");
		Project.runTask(domB);
		domT = (DomT) Project.getTrgt("T");
		Project.runTask(domT);
        domQ = (ProgramDom) Project.getTrgt("Q");
		domU = (ProgramDom) Project.getTrgt("U");
		int numM = domM.size();
    	escHeapInsts = new HashSet<Quad>();

		methToFstP = new int[numM];
		methToNumP = new int[numM];
    	methToArgs = new List[numM];
		methToTmps = new TIntArrayList[numM];
		methToSign = new String[numM];
		invkToInfo = new InvkInfo[numP];
		isIgnoredMeth = new boolean[numM];
		startInvks = new int[10];
		int fstP = 0;
		Program program = Program.v();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract())
				continue;
			String mName = m.getName().toString();
			String mDesc = m.getDesc().toString();
			String mSign = mName + mDesc;
			String cName = m.getDeclaringClass().getName();
			methToSign[mIdx] = mSign;
			jq_Class cls = program.getPreparedClass(cName);
			if (cls == null) {
				System.out.println("WARNING: Ingoring method " + m);
				isIgnoredMeth[mIdx] = true;
			}
			methToFstP[mIdx] = fstP;
			ControlFlowGraph cfg = m.getCFG();
			int numQ = 0;
            for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
                BasicBlock bb = it.nextBasicBlock();
                int n = bb.size();
                if (n == 0)
					numQ++;
                else {
					numQ += n;
					for (int i = 0; i < n; i++) {
						Quad q = bb.getQuad(i);
						if (!(q.getOperator() instanceof Invoke))
							continue;
						int pId = domP.indexOf(q);
						assert (pId != -1);
						MethodOperand mo = Invoke.getMethod(q);
						mo.resolve();
						jq_Method m2 = mo.getMethod();
						String mName2 = m2.getName().toString();
						String mDesc2 = m2.getDesc().toString();
						String mSign2 = mName2 + mDesc2;
						String cName2 = m2.getDeclaringClass().getName();
						if (mSign2.equals("start()V") && cName2.equals("java.lang.Thread")) {
							if (numStartInvks == startInvks.length) {
								int[] startInvks2 = new int[startInvks.length * 2];
								System.arraycopy(startInvks, 0, startInvks2, 0, numStartInvks);
								startInvks = startInvks2;
							}
							startInvks[numStartInvks++] = pId;		
						}
					}
				}
			}
			methToNumP[mIdx] = numQ;
			fstP += numQ;
        }
		assert (fstP == numP);
		isDoneMeth = new boolean[numM];
		cIdMap = new IndexHashMap<String>();
 		heapInstToAllocInsts = new HashMap<Quad, Set<Quad>>();
    }
	
	public void initPass() {
		int numM = domM.size();
		for (int mId = 0; mId < numM; mId++)
			isDoneMeth[mId] = false;
		done.clear();
		threadToHandlerMap.clear();

		domQ.clear();
		domQ.getOrAdd(null);
		succ = new int[NUMQ_ESTIMATE];
		succs = new int[NUMQ_ESTIMATE][];
		allocSet = new HashSet<IntTrio>();
		asgnSet = new HashSet<IntPair>();
		copyPset = new HashSet<IntTrio>();
		copyQset = new HashSet<IntTrio>();
		getinstSet = new HashSet<IntQuad>();
		putinstSet = new HashSet<IntQuad>();
		getstatSet = new HashSet<IntPair>();
		putstatSet = new HashSet<IntPair>();
		spawnSet = new HashSet<IntPair>();
		startSet = new HashSet<IntPair>();
		basePUset = new HashSet<IntPair>();
		PQset = new HashSet<IntPair>();
		cIdMap.clear();
		cIdMap.getOrAdd(null);
	}

	public void doneAllPasses() {
		System.out.println("STATS:" +
		 	"\nnew: " + numNew + 
			"\nnewarray: " + numNewArray +
			"\ngetfield: " + numGetfield + 
			"\nputfield: " + numPutfield + 
			"\naload: " + numAload + 
			"\nastore: " + numAstore +
			"\nphi: " + numPhi + 
			"\nmove: " + numMove + 
			"\ncheckcast: " + numCheckCast + 
			"\ninvk: " + numInvk +
			"\nret: " + numRet + 
			"\ngetstatic: " + numGetstatic +
			"\nputstatic: " + numPutstatic);

		ProgramRel relEH = (ProgramRel) Project.getTrgt("EH");
		relEH.zero();
		allocInstsToHeapInsts = new HashMap<Set<Quad>, Set<Quad>>();
        for (Map.Entry<Quad, Set<Quad>> e :
				heapInstToAllocInsts.entrySet()) {
            Quad heapInst = e.getKey();
            Set<Quad> allocInsts = e.getValue();
			for (Quad allocInst : allocInsts)
				relEH.add(heapInst, allocInst);
			Set<Quad> heapInsts = allocInstsToHeapInsts.get(allocInsts);
			if (heapInsts == null) {
				heapInsts = new ArraySet<Quad>();
				allocInstsToHeapInsts.put(allocInsts, heapInsts);
			}
			heapInsts.add(heapInst);
		}
		relEH.save();

		try {
			String outDirName = Properties.outDirName;
			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_pathEscE.txt")));
				for (Quad e : escHeapInsts)
					writer.println(Program.v().toPosStr(e));
				writer.close();
			}

			{
				PrintWriter writer = new PrintWriter(new FileWriter(
					new File(outDirName, "hybrid_pathLocE.txt")));
   		     	for (Quad e : heapInstToAllocInsts.keySet()) {
					writer.println(Program.v().toPosStr(e));
					for (Quad h : heapInstToAllocInsts.get(e)) {
						writer.println("\t" + Program.v().toPosStr(h));
					}
				}
				writer.close();
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	public void donePass() {
		System.out.println("DONE PASS");
		domQ.save();
		domU.save();

		ProgramRel relSucc = (ProgramRel) Project.getTrgt("succ");
		relSucc.zero();
		final int numQ = domQ.size();
		for (int q = 0; q < numQ; q++) {
			final int r = succ[q];
			if (r == NULL_Q_VAL)
				continue;
			if (r != REDIRECTED)
				relSucc.add(q, r);
			else {
				final int[] S = succs[q];
				final int nS = S[0];
				for (int i = 1; i <= nS; i++)
					relSucc.add(q, S[i]);
			}
		}
		relSucc.save();

		ProgramRel relAsgn = (ProgramRel) Project.getTrgt("asgnP");
		relAsgn.zero();
		for (IntPair p : asgnSet) {
			int q = p.idx0;
			int v = p.idx1;
			relAsgn.add(q, v);
		}
		asgnSet.clear();
		relAsgn.save();

		ProgramRel relPcopy = (ProgramRel) Project.getTrgt("copyP");
		relPcopy.zero();
		for (IntTrio t : copyPset) {
			int p = t.idx0;
			int l = t.idx1;
			int r = t.idx2;
			relPcopy.add(p, l, r);
		}
		copyPset.clear();
		relPcopy.save();

		ProgramRel relQcopy = (ProgramRel) Project.getTrgt("copyQ");
		relQcopy.zero();
		for (IntTrio t : copyQset) {
			int q = t.idx0;
			int l = t.idx1;
			int r = t.idx2;
			relQcopy.add(q, l, r);
		}
		copyQset.clear();
		relQcopy.save();

		ProgramRel relAlloc = (ProgramRel) Project.getTrgt("allocP");
		relAlloc.zero();
		for (IntTrio p : allocSet) {
			int q = p.idx0;
			int v = p.idx1;
			int h = p.idx2;
			relAlloc.add(q, v, h);
		}
		allocSet.clear();
		relAlloc.save();

		ProgramRel relGetinst = (ProgramRel) Project.getTrgt("getinstP");
		relGetinst.zero();
		for (IntQuad p : getinstSet) {
			int q = p.idx0;
			int l = p.idx1;
			int b = p.idx2;
			int f = p.idx3;
			relGetinst.add(q, l, b, f);
		}
		getinstSet.clear();
		relGetinst.save();

		ProgramRel relPutinst = (ProgramRel) Project.getTrgt("putinstP");
		relPutinst.zero();
		for (IntQuad p : putinstSet) {
			int q = p.idx0;
			int b = p.idx1;
			int f = p.idx2;
			int r = p.idx3;
			relPutinst.add(q, b, f, r);
		}
		putinstSet.clear();
		relPutinst.save();

		ProgramRel relGetstat = (ProgramRel) Project.getTrgt("getstatP");
		relGetstat.zero();
		for (IntPair p : getstatSet) {
			int q = p.idx0;
			int v = p.idx1;
			relGetstat.add(q, v);
		}
		getstatSet.clear();
		relGetstat.save();

		ProgramRel relPutstat = (ProgramRel) Project.getTrgt("putstatP");
		relPutstat.zero();
		for (IntPair p : putstatSet) {
			int q = p.idx0;
			int v = p.idx1;
			relPutstat.add(q, v);
		}
		putstatSet.clear();
		relPutstat.save();

		ProgramRel relSpawn = (ProgramRel) Project.getTrgt("spawnP");
		relSpawn.zero();
		for (IntPair p : spawnSet) {
			int q = p.idx0;
			int v = p.idx1;
			relSpawn.add(q, v);
		}
		spawnSet.clear();
		relSpawn.save();

		ProgramRel relStart = (ProgramRel) Project.getTrgt("startP");
		relStart.zero();
		for (IntPair t : startSet) {
			int p = t.idx0;
			int u = t.idx1;
			relStart.add(p, u);
		}
		startSet.clear();
		relStart.save();

		ProgramRel relBasePU = (ProgramRel) Project.getTrgt("basePU");
		relBasePU.zero();
		for (IntPair t : basePUset) {
			int p = t.idx0;
			int u = t.idx1;
			relBasePU.add(p, u);
		}
		basePUset.clear();
		relBasePU.save();

		ProgramRel relPQ = (ProgramRel) Project.getTrgt("PQ");
		relPQ.zero();
		for (IntPair t : PQset) {
			int p = t.idx0;
			int q = t.idx1;
			relPQ.add(p, q);
		}
		PQset.clear();
		relPQ.save();

        String checkExcludeNames = Properties.checkExcludeNames;
        String[] excluded = checkExcludeNames.equals("") ? new String[0] :
            checkExcludeNames.split(Properties.LIST_SEPARATOR);
		ProgramRel relRelevantT = (ProgramRel) Project.getTrgt("relevantT");
        IndexSet<jq_Class> classes = Program.v().getPreparedClasses();
		relRelevantT.zero();
		for (jq_Class c : classes) {
			String cName = c.getName();
            boolean match = false;
            for (String s : excluded) {
                if (cName.startsWith(s)) {
                    match = true;
                    break;
                }
            }
			if (!match) {
				int tIdx = domT.indexOf(c);
				relRelevantT.add(tIdx);
			}
		}
		relRelevantT.save();
		Project.resetTaskDone("relevant-Q-dlog");
		Project.runTask("relevant-Q-dlog");

		boolean useLivenessAnalysis = false;

		if (useLivenessAnalysis) {
			Project.resetTaskDone("liveness-def-use-dlog");
			Project.runTask("liveness-def-use-dlog");
		} else {
			Project.resetTaskDone("relevant-def-use-dlog");
			Project.runTask("relevant-def-use-dlog");
		}

		int[] def = new int[numQ];
		int[][] defs = new int[numQ][];
		int[] use = new int[numQ];
		int[][] uses = new int[numQ][];
		for (int q = 0; q < numQ; q++) {
			def[q] = NULL_U_VAL;
			use[q] = NULL_U_VAL;
		}

		{
			ProgramRel defQU = (ProgramRel) Project.getTrgt("defQU");
			defQU.load();
			IntPairIterable tuples = defQU.getAry2IntTuples();
			for (IntPair tuple : tuples) {
				int q = tuple.idx0;
				int v = tuple.idx1;
				int u = def[q];
				if (u == NULL_U_VAL)
					def[q] = v;
				else if (u == REDIRECTED) {
					int[] D = defs[q];
					int n = D[0] + 1;
					int len = D.length;
					if (n == len) {
						int[] D2 = new int[len * 2];
						System.arraycopy(D, 0, D2, 0, len);
						defs[q] = D2;
						D = D2;
					}
					D[++D[0]] = v;
				} else {
					def[q] = REDIRECTED;
					int[] D = new int[3];
					D[0] = 2;
					D[1] = u;
					D[2] = v;
					defs[q] = D;
				}
			}
			defQU.close();
		}

		{
			ProgramRel useQU = (ProgramRel) Project.getTrgt("useQU");
			useQU.load();
			IntPairIterable tuples = useQU.getAry2IntTuples();
			for (IntPair tuple : tuples) {
				int q = tuple.idx0;
				int v = tuple.idx1;
				int u = use[q];
				if (u == NULL_U_VAL)
					use[q] = v;
				else if (u == REDIRECTED) {
					int[] U = uses[q];
					int n = U[0];
					int len = U.length;
					if (n == len - 1) {
						int[] U2 = new int[len * 2];
						System.arraycopy(U, 0, U2, 0, len);
						uses[q] = U2;
						U = U2;
					}
					U[++U[0]] = v;
				} else {
					use[q] = REDIRECTED;
					int[] U = new int[3];
					U[0] = 2;
					U[1] = u;
					U[2] = v;
					uses[q] = U;
				}
			}
			useQU.close();
		}

		
		if (useLivenessAnalysis) {
			LivenessAnalysis.run(def, defs, use, uses, succ, succs, numQ);
			Project.resetTaskDone("liveness-pres-dlog");
			Project.runTask("liveness-pres-dlog");
		} else {
			int[][] movs = new int[numQ][];
			ProgramRel movsRel = (ProgramRel) Project.getTrgt("movs");
			movsRel.load();
			IntTrioIterable tuples = movsRel.getAry3IntTuples();
			for (IntTrio tuple : tuples) {
				int q = tuple.idx0;
				int l = tuple.idx1;
				int r = tuple.idx2;
				int[] M = movs[q];
				if (M == null) {
					M = new int[3];
					M[0] = 1;
					M[1] = l;
					M[2] = r;
					movs[q] = M;
				} else {
					int n = (M[0] * 2) + 1;
					int len = M.length;
					if (n == len) {
						int[] M2 = new int[2 * len - 1];
						System.arraycopy(M, 0, M2, 0, len);
						movs[q] = M2;
						M = M2;
					}
					M[n] = l;
					M[n + 1] = r;
					M[0]++;
				}
			}
			movsRel.close();
			RelevantAnalysis.run(def, defs, use, uses, movs, succ, succs, numQ);
			Project.resetTaskDone("relevant-pres-dlog");
			Project.runTask("relevant-pres-dlog");
		}
        Project.resetTaskDone("hybrid-thresc-dlog");
        Project.runTask("hybrid-thresc-dlog");

        ProgramRel relRelevantEH =
            (ProgramRel) Project.getTrgt("relevantEH");
        relRelevantEH.load();
        PairIterable<Quad, Quad> tuples =
            relRelevantEH.getAry2ValTuples();
        for (Pair<Quad, Quad> tuple : tuples) {
            Quad e = tuple.val0;
            if (escHeapInsts.contains(e)) {
                System.out.println("Deemed esc in earlier path: " + e);
                // already proven definitely escaping
                // in an earlier path program
                continue;
            }
            Quad h = tuple.val1;
            Set<Quad> allocs = heapInstToAllocInsts.get(e);
            if (allocs == null) {
                allocs = new ArraySet<Quad>();
                heapInstToAllocInsts.put(e, allocs);
            }
            allocs.add(h);
        }
        relRelevantEH.close();

        ProgramRel relHybridEscE =
            (ProgramRel) Project.getTrgt("hybridEscE");
        relHybridEscE.load();
        Iterable<Quad> tuples2 = relHybridEscE.getAry1ValTuples();
        for (Quad e : tuples2) {
            if (escHeapInsts.add(e) && heapInstToAllocInsts.remove(e) != null) {
                // was deemed thread local in an earlier path program
				System.out.println("Deemed loc in earlier path: " + e);
            }
        }
        relHybridEscE.close();
	}

	private InvkInfo getInvkInfo(Quad q, int pId) {
		List<IntPair> invkArgs = null;
		ParamListOperand lo = Invoke.getParamList(q);
		int numArgs = lo.length();
		for (int j = 0; j < numArgs; j++) {
			RegisterOperand vo = lo.get(j);
			Register v = vo.getRegister();
			if (v.getType().isReferenceType()) {
				int vId = domU.getOrAdd(v);
				if (invkArgs == null)
					invkArgs = new ArrayList<IntPair>();
				invkArgs.add(new IntPair(j, vId));
			}
		}
		if (invkArgs == null)
			invkArgs = emptyArgsList;
		int invkRetn = -1;
		RegisterOperand vo = Invoke.getDest(q);
		if (vo != null) {
			Register v = vo.getRegister();
			if (v.getType().isReferenceType()) {
				invkRetn = domU.getOrAdd(v);
			}
		}
		MethodOperand mo = Invoke.getMethod(q);
		mo.resolve();
		jq_Method m = mo.getMethod();
		String mName = m.getName().toString();
		String mDesc = m.getDesc().toString();
		String mSign = mName + mDesc;
		InvkInfo info = new InvkInfo(pId, mSign, invkArgs, invkRetn);
		return info;
	}
	private List<IntPair> getMethArgs(int mIdx) {
		jq_Method m = domM.get(mIdx);
		ControlFlowGraph cfg = m.getCFG();
		// process method's args and tmps
		List<IntPair> args = null;
		TIntArrayList tmps = null;
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = m.getParamTypes().length;
		int numVars = rf.size();
		for (int zId = 0; zId < numArgs; zId++) {
			Register v = rf.get(zId);
			if (v.getType().isReferenceType()) {
				int vId = domU.getOrAdd(v);
				assert (vId != -1);
				if (args == null)
					args = new ArrayList<IntPair>();
				args.add(new IntPair(zId, vId));
			}
		}
		for (int zId = numArgs; zId < numVars; zId++) {
			Register v = rf.get(zId);
			if (v.getType().isReferenceType()) {
				int vId = domU.getOrAdd(v);
				assert (vId != -1);
				if (tmps == null)
					tmps = new TIntArrayList();
				tmps.add(vId);
			}
		}
		if (args == null)
			args = emptyArgsList;
		if (tmps == null)
			tmps = emptyTmpsList;
		methToArgs[mIdx] = args;
		methToTmps[mIdx] = tmps;
		return args;
	}
	public void processEnterMethod(int m, int t) {
/*
		jq_Method meth = domM.get(m);
		String name = meth.getDeclaringClass().getName();
		if (!name.startsWith("java.") && !name.startsWith("sun.") && !name.startsWith("com.ibm."))
			System.out.println("METHOD: " + meth + " " + t);
*/
		// System.out.println("EM " + domM.get(m) + " " + t);
		// System.out.println(t + " EM " + m);
		ThreadHandler handler = threadToHandlerMap.get(t);
		if (handler == null) {
			handler = new ThreadHandler(t);
			threadToHandlerMap.put(t, handler);
		}
		handler.processEnterMethod(m);
	}

    public void processLeaveMethod(int m, int t) {
		// System.out.println("LM " + domM.get(m) + " " + t);
		// System.out.println(t + " LM " + m);
		ThreadHandler handler = threadToHandlerMap.get(t);
		if (handler != null)
			handler.processLeaveMethod(m);
	}

	public void processBasicBlock(int b, int t) {
		// System.out.println("B " + b + " " + t);
		ThreadHandler handler = threadToHandlerMap.get(t);
		if (handler != null)
			handler.processBasicBlock(b);
	}

	public void processQuad(int p, int t) {
		// System.out.println("Q " + p + " " + t);
		ThreadHandler handler = threadToHandlerMap.get(t);
		if (handler != null)
			handler.processQuad(p);
	}

	class Frame {
		final int mId;
		// context of this method, i.e., number of times this method has
		// been called until now in the current run, across all threads
		final int cId;
		// isExplicitlyCalled is true iff method was explicitly called
		// if isExplicitlyCalled is true then invkRet may be -1 meaning that either
		// method doesn't return a value of reference type or call site
		// ignores returned value
		// if isExplicitlyCalled is false then invkRet is undefined
		boolean isExplicitlyCalled;
		int invkRet;
		BasicBlock prevBB;
		BasicBlock currBB;
		int prevXid;
		InvkInfo pendingInvk;
		public Frame(int mId, int cId) {
			this.mId = mId;
			this.cId = cId;
		}
	}

	class InvkInfo {
		final int pId;
		final String sign;
		final List<IntPair> invkArgs;
		final int invkRet;
		public InvkInfo(int pId, String sign, List<IntPair> invkArgs, int invkRet) {
			this.pId = pId;
			this.sign = sign;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
		}
		public String toString() {
			return sign + " " + invkRet;
		}
	}

	class ThreadHandler {
		private Stack<Frame> frames = new Stack<Frame>();
		private Frame top;
		private boolean foundThreadRoot;
		// ignoredMethNumFrames > 0 means currently ignoring code
		// reachable from method with id ignoredMethId
		private int ignoredMethId;
		private int ignoredMethNumFrames;
		// private boolean[] alreadyCalled;
		private int prevQid;
		private int currPid;
		private int tId;
		public ThreadHandler(int tId) {
			this.tId = tId;
			prevQid = -1;
			// alreadyCalled = new boolean[domM.size()];
		}
		private int getCid(int pId) {
			String s = Integer.toString(pId);
			int n = frames.size();
			for (int i = n - 2; i >= 0; i--) {
				Frame frame = frames.get(i);
				InvkInfo pendingInvk = frame.pendingInvk;
				if (pendingInvk == null)
					break;
				s += "_" + pendingInvk.pId;
			}
			int cId = cIdMap.getOrAdd(s);
			// System.out.println("cId: " + cId + " s: " + s);
			return cId;
		}
		private void addSucc(int q, int r) {
			// assert (q != r);
			int s = succ[q];
			if (s == NULL_Q_VAL)
				succ[q] = r;
			else if (s == REDIRECTED) {
				int[] S = succs[q];
				int n = S[0];
				int len = S.length;
				if (n == len - 1) {
					int[] S2 = new int[len * 2];
					System.arraycopy(S, 0, S2, 0, len);
					succs[q] = S2;
					S = S2;
				}
				S[++S[0]] = r;
			} else {
				succ[q] = REDIRECTED;
				assert (succs[q] == null);
				int[] S = new int[3];
				S[0] = 2;
				S[1] = s;
				S[2] = r;
				succs[q] = S;
			}
		}
		private int setCurrQid(int pId) {
			currPid = pId;
			int currQid = domQ.getOrAdd(new IntPair(pId, top.cId));
			// System.out.println("currQid: " + currQid + " pId: " + pId + " cId: " + top.cId);
			if (prevQid != -1) {
				addSucc(prevQid, currQid);
				// System.out.println("Adding edge: " + prevQid);
			}
			prevQid = currQid;
			if (currQid == domQ.size() - 1) {
				PQset.add(new IntPair(pId, currQid));
				int n = succ.length;
				if (currQid == n) {
					int[] succ2 = new int[n * 2];
					int[][] succs2 = new int[n * 2][];
					System.arraycopy(succ , 0, succ2 , 0, n);
					System.arraycopy(succs, 0, succs2, 0, n);
					succ = succ2;
					succs = succs2;
				}
			}
			return currQid;
		}
		private int setCurrQid(Quad q) {
			int pId = domP.indexOf(q);
			return setCurrQid(pId);
		}
		public void processEnterMethod(int mId) {
			assert (mId >= 0);
			if (ignoredMethNumFrames > 0) {
				if (mId == ignoredMethId)
					ignoredMethNumFrames++;
				return;
			}
			if (isIgnoredMeth[mId]) {
				ignoredMethId = mId;
				ignoredMethNumFrames = 1;
				return;
			}
/*
			if (alreadyCalled[mId]) {
				ignoredMethId = mId;
				ignoredMethNumFrames = 1;
				return;
			}
*/
			InvkInfo pendingInvk = (top != null) ? top.pendingInvk : null;
			List<IntPair> methArgs = methToArgs[mId];
			if (methArgs == null)
				methArgs = getMethArgs(mId);
			final String mSign = methToSign[mId];
			List<IntPair> invkArgs;
			if (pendingInvk != null && pendingInvk.sign.equals(mSign)) {
				invkArgs = pendingInvk.invkArgs;
				if (invkArgs.size() != methArgs.size())
					invkArgs = null;
			} else
				invkArgs = null;
			// at this point invkArgs != null iff this was not an explicitly
			// called method

			boolean doThreadRoot = false;
			if (invkArgs == null) {
				if (!foundThreadRoot && (mSign.equals("main([Ljava/lang/String;)V") ||
						mSign.equals("run()V"))) {
					doThreadRoot = true;
					System.out.println("WARNING: Treating method: " +
						domM.get(mId) + " as root of thread");
				} else if (!mSign.equals("<clinit>()V")) {
					// System.out.println("WARNING: Ignoring phantom method: " +
					//	domM.get(mId));
					ignoredMethId = mId;
					ignoredMethNumFrames = 1;
					return;
				}
			}
			// alreadyCalled[mId] = true;

			// System.out.println("XXX EM: " + domM.get(mId));

			if (top != null)
				frames.push(top);
			final int cId = (invkArgs == null) ? 0 : getCid(pendingInvk.pId);
			top = new Frame(mId, cId);

			final int currQid = setCurrQid(methToFstP[mId]);
			boolean initArgs = false;
			if (invkArgs != null) {
				int numArgs = methArgs.size();
				top.invkRet = pendingInvk.invkRet;
				top.isExplicitlyCalled = true;
				for (int i = 0; i < numArgs; i++) {
					IntPair zv = methArgs.get(i);
					int zId = zv.idx0;
					int vId = zv.idx1;
					IntPair zu = invkArgs.get(i);
					assert (zu.idx0 == zId);
					int uId = zu.idx1;
					copyQset.add(new IntTrio(currQid, vId, uId));
				}
			} else if (doThreadRoot) {
				foundThreadRoot = true;
				IntPair thisArg = methArgs.get(0);
				assert (thisArg.idx0 == 0);
					int vId = thisArg.idx1;
				if (mSign.equals("run()V"))
					startSet.add(new IntPair(currPid, vId));
				else
					asgnSet.add(new IntPair(currPid, vId));
			} else
				initArgs = true;
			if (!isDoneMeth[mId]) {
				if (initArgs) {
                    for (IntPair p : methArgs) {
                        int vId = p.idx1;
                        asgnSet.add(new IntPair(currPid, vId));
                    }
				}
				TIntArrayList methTmps = methToTmps[mId];
				int numTmps = methTmps.size();
				for (int i = 0; i < numTmps; i++) {
					int vId = methTmps.get(i);
					asgnSet.add(new IntPair(currPid, vId));
				}
				isDoneMeth[mId] = true;
			}
		}
		public void processLeaveMethod(int mId) {
			assert (mId >= 0);
			if (ignoredMethNumFrames > 0) {
				if (mId == ignoredMethId)
					ignoredMethNumFrames--;
				return;
			}
			if (top == null)
				return;
			if (mId != top.mId) {
				System.out.println("mId: " + mId + " top.mId: " + top.mId); 
				assert (false);
			}
			// alreadyCalled[mId] = false;
			top = (frames.isEmpty()) ? null : frames.pop();

			// System.out.println("XXX LM: " + domM.get(mId));
		}
		public void processBasicBlock(int bId) {
			if (ignoredMethNumFrames > 0)
				return;
			if (top == null)
				return;
			BasicBlock bb = domB.get(bId);
			assert (bb != null);
			BasicBlock currBB = top.currBB;
			if (currBB != null) {
				int n = currBB.size();
				for (int i = top.prevXid; i < n; i++) {
					Quad q = currBB.getQuad(i);
					if (Instrumentor.isRelevant(q))
						break;
					processQuad(q);
				}
			}
			top.prevBB = currBB;
			top.currBB = bb;
			top.prevXid = 0;
		}
		public void processQuad(int pId) {
			if (ignoredMethNumFrames > 0)
				return;
			if (top == null) 
				return;
			BasicBlock currBB = top.currBB;
			assert (currBB != null);
			Quad p = (Quad) domP.get(pId);
			while (true) {
				Quad q = currBB.getQuad(top.prevXid++);
				processQuad(q);
				if (p == q)
					break;
			}
		}
		private void processQuad(Quad q) {
			// System.out.println("PROCESSING: " + q);
			top.pendingInvk = null;
			Operator op = q.getOperator();
			if (op instanceof Move) {
				numMove++;
				processMove(q);
			} else if (op instanceof Getfield) {
				numGetfield++;
				processGetfield(q);
			} else if (op instanceof Invoke) {
				numInvk++;
				processInvoke(q);
			} else if (op instanceof ALoad) {
				numAload++;
				processAload(q);
			} else if (op instanceof Phi) {
				numPhi++;
				processPhi(q);
			} else if (op instanceof RETURN_A) {
				numRet++;
				processReturn(q);
			} else if (op instanceof Putfield) {
				numPutfield++;
				processPutfield(q);
			} else if (op instanceof AStore) {
				numAstore++;
				processAstore(q);
			} else if (op instanceof Getstatic) {
				numGetstatic++;
				processGetstatic(q);
			} else if (op instanceof Putstatic) {
				numPutstatic++;
				processPutstatic(q);
			} else if (op instanceof New) {
				numNew++;
				processNewOrNewArray(q, true);
			} else if (op instanceof NewArray) {
				numNewArray++;
				processNewOrNewArray(q, false);
			} else if (op instanceof CheckCast) {
				numCheckCast++;
				processMove(q);
			}
		}
		private void processPhi(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			if (t == null) {
				// System.out.println("XXX: " + q + " " + domM.get(top.mId) + " " + top.currBB + " " + top.prevBB);
				return;
			}
			if (!t.isReferenceType())
				return;
			setCurrQid(q);
			BasicBlockTableOperand bo = Phi.getPreds(q);
			int n = bo.size();
			int i = 0;
			// if (top.prevBB == null)
			//	System.out.println("XXX: " + domM.get(top.mId));
			assert (top.prevBB != null);
			for (; i < n; i++) {
				BasicBlock bb = bo.get(i);
				if (bb == top.prevBB)
					break;
			}
			assert (i < n);
			RegisterOperand ro = Phi.getSrc(q, i);
			Register r = ro.getRegister();
			int rId = domU.indexOf(r);
			assert (rId != -1);
			Register l = lo.getRegister();
			int lId = domU.indexOf(l);
			assert (lId != -1);
			copyPset.add(new IntTrio(currPid, lId, rId));
			done.add(q);
		}
		private void processNewOrNewArray(Quad q, boolean isNew) {
			setCurrQid(q);
			RegisterOperand vo = isNew ? New.getDest(q) : NewArray.getDest(q);
			Register v = vo.getRegister();
			int vId = domU.indexOf(v);
			assert (vId != -1);
			int hId = domH.indexOf(q);
			assert (hId != -1);
			allocSet.add(new IntTrio(currPid, vId, hId));
			done.add(q);
		}
		private void processMove(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
			Operand rx = Move.getSrc(q);
			RegisterOperand ro;
			if (rx instanceof RegisterOperand) {
				ro = (RegisterOperand) rx;
				if (!ro.getType().isReferenceType())
					return;
			} else {
				assert (rx instanceof ConstOperand);
				assert (!(rx instanceof PConstOperand));
				if (!(rx instanceof AConstOperand))
					return;
				ro = null;
			}
			setCurrQid(q);
			RegisterOperand lo = Move.getDest(q);
			Register l = lo.getRegister();
			int lId = domU.indexOf(l);
			// if (lId == -1)
			// 	System.out.println(q + " " + " XXX" + l + "XXX " + domM.get(top.mId) + " " + tId);
			assert (lId != -1);
			if (ro != null) {
				Register r = ro.getRegister();
				int rId = domU.indexOf(r);
				assert (rId != -1);
				copyPset.add(new IntTrio(currPid, lId, rId));
			} else {
				asgnSet.add(new IntPair(currPid, lId));
			}
			done.add(q);
		}
		private void processGetstatic(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
        	jq_Field f = Getstatic.getField(q).getField();
        	if (!f.getType().isReferenceType())
        		return;
			setCurrQid(q);
			RegisterOperand lo = Getstatic.getDest(q);
			Register l = lo.getRegister();
			int lId = domU.indexOf(l);
			assert (lId != -1);
			getstatSet.add(new IntPair(currPid, lId));
			done.add(q);
		}
		private void processPutstatic(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
        	jq_Field f = Putstatic.getField(q).getField();
        	if (!f.getType().isReferenceType())
        		return;
        	Operand rx = Putstatic.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			setCurrQid(q);
			RegisterOperand ro = (RegisterOperand) rx;
			Register r = ro.getRegister();
			int rId = domU.indexOf(r);
			assert (rId != -1);
			putstatSet.add(new IntPair(currPid, rId));
			done.add(q);
		}
		private void processAload(Quad q) {
			setCurrQid(q);
            if (done.contains(q))
                return;
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			Register b = bo.getRegister();
			int bId = domU.indexOf(b);
			assert (bId != -1);
			basePUset.add(new IntPair(currPid, bId));
			if (!((ALoad) q.getOperator()).getType().isReferenceType())
				return;
			RegisterOperand lo = ALoad.getDest(q);
			Register l = lo.getRegister();
			int lId = domU.indexOf(l);
			assert (lId != -1);
			int fId = 0;
			getinstSet.add(new IntQuad(currPid, lId, bId, fId));
			done.add(q);
		}
		private void processAstore(Quad q) {
			setCurrQid(q);
            if (done.contains(q))
                return;
			RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
			Register b = bo.getRegister();
			int bId = domU.indexOf(b);
			assert (bId != -1);
			basePUset.add(new IntPair(currPid, bId));
			if (!((AStore) q.getOperator()).getType().isReferenceType())
				return;
			Operand rx = AStore.getValue(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand ro = (RegisterOperand) rx;
			Register r = ro.getRegister();
			int rId = domU.indexOf(r);
			assert (rId != -1);
			assert (rId != -1);
			int fId = 0;
			putinstSet.add(new IntQuad(currPid, bId, fId, rId));
			done.add(q);
		}
		private void processGetfield(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
			Operand bx = Getfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			setCurrQid(q);
			RegisterOperand bo = (RegisterOperand) bx;
			Register b = bo.getRegister();
			int bId = domU.indexOf(b);
			assert (bId != -1);
			basePUset.add(new IntPair(currPid, bId));
			FieldOperand fo = Getfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType())
				return;
			int fId = domF.indexOf(f);
			if (fId == -1)
				return;
			RegisterOperand lo = Getfield.getDest(q);
			Register l = lo.getRegister();
			int lId = domU.indexOf(l);
			assert (lId != -1);
			getinstSet.add(new IntQuad(currPid, lId, bId, fId));
			done.add(q);
		}
		private void processPutfield(Quad q) {
            if (done.contains(q)) {
                setCurrQid(q);
                return;
            }
			Operand bx = Putfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			setCurrQid(q);
			RegisterOperand bo = (RegisterOperand) bx;
			Register b = bo.getRegister();
			int bId = domU.indexOf(b);
			assert (bId != -1);
			basePUset.add(new IntPair(currPid, bId));
			FieldOperand fo = Putfield.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (!f.getType().isReferenceType())
				return;
			Operand rx = Putfield.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			int fId = domF.indexOf(f);
			if (fId == -1)
				return;
			RegisterOperand ro = (RegisterOperand) rx;
			Register r = ro.getRegister();
			int rId = domU.indexOf(r);
			assert (rId != -1);
			putinstSet.add(new IntQuad(currPid, bId, fId, rId));
			done.add(q);
		}
		private void processInvoke(Quad q) {
			int pId = domP.indexOf(q);
			assert (pId != -1);
			boolean isStartInvk = false;
			for (int i = 0; i < numStartInvks; i++) {
				if (startInvks[i] == pId) {
					isStartInvk = true;
					break;
				}
			}
			if (isStartInvk) {
				setCurrQid(q);
				if (done.contains(q))
					return;
				InvkInfo info = invkToInfo[pId];
				if (info == null) {
					info = getInvkInfo(q, pId);
					invkToInfo[pId] = info;
				}
				List<IntPair> invkArgs = info.invkArgs;
				IntPair thisArg = invkArgs.get(0);
				assert (thisArg.idx0 == 0);
				int vId = thisArg.idx1;
				spawnSet.add(new IntPair(currPid, vId));
				done.add(q);
			} else {
				InvkInfo info = invkToInfo[pId];
				if (info == null) {
					info = getInvkInfo(q, pId);
					invkToInfo[pId] = info;
				}
				top.pendingInvk = info;
			}
		}
		private void processReturn(Quad q) {
			boolean isExplicitlyCalled = top.isExplicitlyCalled;
			if (!isExplicitlyCalled)
				return;
			int invkRet = top.invkRet;
			if (invkRet == -1)
				return;
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				Register v = ro.getRegister();
				int methRet = domU.indexOf(v);
				assert (methRet != -1);
				int currQid = setCurrQid(q);
				copyQset.add(new IntTrio(currQid, invkRet, methRet));
			}
		}
	}
}

