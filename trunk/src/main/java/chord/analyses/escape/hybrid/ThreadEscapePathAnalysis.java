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

import chord.project.Properties;
import chord.project.ChordRuntimeException;
import chord.util.ArraySet;
import chord.util.FileUtils;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.util.ProcessExecutor;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntQuad;
import chord.util.tuple.object.Pair;
import chord.bddbddb.Rel.PairIterable;
import chord.doms.DomH;
import chord.doms.DomE;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomF;
import chord.doms.DomV;
import chord.doms.DomP;
import chord.doms.DomB;
import chord.project.Program;
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

@Chord(
    name = "thresc-path-java"
)
public class ThreadEscapePathAnalysis extends DynamicAnalysis {
	private final static boolean DEBUG = false;
	private final static TIntArrayList emptyTmpsList = new TIntArrayList(0);
	private final static List<IntPair> emptyArgsList = Collections.emptyList();
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
    // data structures set once and for all
	private List/*IntPair*/[] methToArgs;
	private TIntArrayList[] methToTmps;
    // set of heap insts deemed escaping in some run so far
	private Set<Quad> escHeapInsts;
	private Map<Quad, Set<Quad>> heapInstToAllocs;

	// data structures set for each run
	private TIntObjectHashMap<Handler> thrToHandlerMap =
		new TIntObjectHashMap<Handler>();
	private int[] methToNumCalls;
	private DomF domF;
	private DomM domM;
	private DomH domH;
	private DomP domP;
	private DomB domB;
	private DomV domV;
	private ProgramDom<IntTrio> domQ;
	private Set<IntPair> succSet;
	private Set<IntTrio> allocSet;
	private Set<IntPair> asgnSet;
	private Set<IntTrio> copySet;
	private Set<IntQuad> getinstSet;
	private Set<IntQuad> putinstSet;
	private Set<IntPair> getstatSet;
	private Set<IntPair> putstatSet;
	private Set<IntPair> spawnSet;
	private Set<IntPair> startSet;
	private Set<IntPair> baseQVset;
	private Set<IntPair> lockQVset;
	private Set<IntPair> PQset;

	public Map<Quad, Set<Quad>> getHeapInstToAllocsMap() {
		return heapInstToAllocs;
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
		domB = (DomB) Project.getTrgt("B");
		Project.runTask(domB);
		domV = (DomV) Project.getTrgt("V");
		Project.runTask(domV);
        domQ = (ProgramDom) Project.getTrgt("Q");
		int numM = domM.size();
    	methToArgs = new List[numM];
		methToTmps = new TIntArrayList[numM];
    	escHeapInsts = new HashSet<Quad>();
    	heapInstToAllocs = new HashMap<Quad, Set<Quad>>();
    }

	public void initPass() {
		thrToHandlerMap.clear();
		methToNumCalls = new int[domM.size()];
		domQ.clear();
		succSet = new HashSet<IntPair>();
		allocSet = new HashSet<IntTrio>();
		asgnSet = new HashSet<IntPair>();
		copySet = new HashSet<IntTrio>();
		getinstSet = new HashSet<IntQuad>();
		putinstSet = new HashSet<IntQuad>();
		getstatSet = new HashSet<IntPair>();
		putstatSet = new HashSet<IntPair>();
		spawnSet = new HashSet<IntPair>();
		startSet = new HashSet<IntPair>();
		baseQVset = new HashSet<IntPair>();
		PQset = new HashSet<IntPair>();
	}

	public void processEnterMethod(int m, int t) {
		// if (DEBUG) System.out.println("T" + t + " ENTER_METHOD " + domM.get(m));
		Handler handler = thrToHandlerMap.get(t);
		if (handler == null) {
			handler = new Handler(t);
			thrToHandlerMap.put(t, handler);
		}
		handler.processEnterMethod(m);
	}
    public void processLeaveMethod(int m, int t) {
		// if (DEBUG) System.out.println("T" + t + " LEAVE_METHOD " + domM.get(m));
		Handler handler = thrToHandlerMap.get(t);
		if (handler != null)
			handler.processLeaveMethod(m);
	}
	public void processBasicBlock(int b, int t) {
		// if (DEBUG) System.out.println("T" + t + " BB " + b);
		Handler handler = thrToHandlerMap.get(t);
		if (handler != null)
			handler.processBasicBlock(b);
	}
	public void processQuad(int p, int t) {
		// if (DEBUG) System.out.println("T" + t + " QUAD " + p);
		Handler handler = thrToHandlerMap.get(t);
		if (handler != null)
			handler.processQuad(p);
	}

	public void donePass() {
		domQ.save();

		ProgramRel relSucc = (ProgramRel) Project.getTrgt("succ");
		relSucc.zero();
		for (IntPair p : succSet) {
			int q1 = p.idx0;
			int q2 = p.idx1;
			relSucc.add(q1, q2);
		}
		relSucc.save();

		ProgramRel relAsgn = (ProgramRel) Project.getTrgt("asgn");
		relAsgn.zero();
		for (IntPair p : asgnSet) {
			int q = p.idx0;
			int v = p.idx1;
			relAsgn.add(q, v);
		}
		relAsgn.save();

		ProgramRel relCopy = (ProgramRel) Project.getTrgt("copy");
		relCopy.zero();
		for (IntTrio p : copySet) {
			int q = p.idx0;
			int l = p.idx1;
			int r = p.idx2;
			relCopy.add(q, l, r);
		}
		relCopy.save();

		ProgramRel relAlloc = (ProgramRel) Project.getTrgt("alloc");
		relAlloc.zero();
		for (IntTrio p : allocSet) {
			int q = p.idx0;
			int v = p.idx1;
			int h = p.idx2;
			relAlloc.add(q, v, h);
		}
		relAlloc.save();

		ProgramRel relGetinst = (ProgramRel) Project.getTrgt("getinst");
		relGetinst.zero();
		for (IntQuad p : getinstSet) {
			int q = p.idx0;
			int l = p.idx1;
			int b = p.idx2;
			int f = p.idx3;
			relGetinst.add(q, l, b, f);
		}
		relGetinst.save();

		ProgramRel relPutinst = (ProgramRel) Project.getTrgt("putinst");
		relPutinst.zero();
		for (IntQuad p : putinstSet) {
			int q = p.idx0;
			int b = p.idx1;
			int f = p.idx2;
			int r = p.idx3;
			relPutinst.add(q, b, f, r);
		}
		relPutinst.save();

		ProgramRel relGetstat = (ProgramRel) Project.getTrgt("getstat");
		relGetstat.zero();
		for (IntPair p : getstatSet) {
			int q = p.idx0;
			int v = p.idx1;
			relGetstat.add(q, v);
		}
		relGetstat.save();

		ProgramRel relPutstat = (ProgramRel) Project.getTrgt("putstat");
		relPutstat.zero();
		for (IntPair p : putstatSet) {
			int q = p.idx0;
			int v = p.idx1;
			relPutstat.add(q, v);
		}
		relPutstat.save();

		ProgramRel relSpawn = (ProgramRel) Project.getTrgt("spawn");
		relSpawn.zero();
		for (IntPair p : spawnSet) {
			int q = p.idx0;
			int v = p.idx1;
			relSpawn.add(q, v);
		}
		relSpawn.save();

		ProgramRel relStart = (ProgramRel) Project.getTrgt("start");
		relStart.zero();
		for (IntPair p : startSet) {
			int q = p.idx0;
			int v = p.idx1;
			relStart.add(q, v);
		}
		relStart.save();

		ProgramRel relBaseQV = (ProgramRel) Project.getTrgt("baseQV");
		relBaseQV.zero();
		for (IntPair qv : baseQVset) {
			relBaseQV.add(qv.idx0, qv.idx1);
		}
		relBaseQV.save();

		ProgramRel relPQ = (ProgramRel) Project.getTrgt("PQ");
		relPQ.zero();
		for (IntPair pq : PQset) {
			relPQ.add(pq.idx0, pq.idx1);
		}
		relPQ.save();

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
            Set<Quad> allocs = heapInstToAllocs.get(e);
            if (allocs == null) {
                allocs = new ArraySet<Quad>();
                heapInstToAllocs.put(e, allocs);
            }
            allocs.add(h);
        }
        relRelevantEH.close();

        ProgramRel relHybridEscE =
            (ProgramRel) Project.getTrgt("hybridEscE");
        relHybridEscE.load();
        Iterable<Quad> tuples2 = relHybridEscE.getAry1ValTuples();
        for (Quad e : tuples2) {
            if (escHeapInsts.add(e) && heapInstToAllocs.remove(e) != null) {
                // was deemed thread local in an earlier path program
				System.out.println("Deemed loc in earlier path: " + e);
            }
        }
        relHybridEscE.close();

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
   		     	for (Quad e : heapInstToAllocs.keySet()) {
					writer.println(Program.v().toPosStr(e));
					for (Quad h : heapInstToAllocs.get(e)) {
						writer.println("\t" + Program.v().toPosStr(h));
					}
				}
				writer.close();
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
		
	}

	private List<IntPair> processMethVars(jq_Method m, int mId) {
		List<IntPair> args = null;
		TIntArrayList tmps = null;
		ControlFlowGraph cfg = m.getCFG();
		assert (cfg != null);
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = m.getParamTypes().length;
		int numVars = rf.size();
		for (int zId = 0; zId < numArgs; zId++) {
			Register v = rf.get(zId);
			if (v.getType().isReferenceType()) {
				int vId = domV.indexOf(v);
				assert (vId != -1);
				if (args == null)
					args = new ArrayList<IntPair>();
				args.add(new IntPair(zId, vId));
			}
		}
		for (int zId = numArgs; zId < numVars; zId++) {
			Register v = rf.get(zId);
			if (v.getType().isReferenceType()) {
				int vId = domV.indexOf(v);
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
		methToArgs[mId] = args;
		methToTmps[mId] = tmps;
		return args;
	}

	class Frame {
		final int mId;
		// context of this method, i.e., number of times this method has
		// been called until now in the current run, across all threads
		final int cId;
		// isExplicit is true iff method was explicitly called
		// if isExplicit is true then invkRet may be -1 meaning that either
		// method doesn't return a value of reference type or call site
		// ignores returned value
		// if isExplicit is false then invkRet is undefined
		boolean isExplicit;
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
		final String sign;
		final List<IntPair> invkArgs;
		final int invkRet;
		public InvkInfo(String sign, List<IntPair> invkArgs, int invkRet) {
			this.sign = sign;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
		}
		public String toString() {
			return sign + " " + invkRet;
		}
	}

	class Handler {
		private Stack<Frame> frames = new Stack<Frame>();
		private Frame top;
		private boolean foundThreadRoot;
		// ignoredMethNumFrames > 0 means currently ignoring code
		// reachable from method with id ignoredMethId
		private int ignoredMethId;
		private int ignoredMethNumFrames;
		private int prevQid;
		private int tId; // just for debugging
		public Handler(int tId) {
			this.tId = tId;
			prevQid = -1;
		}
		public void beginIgnoredMeth(int mId) {
			ignoredMethId = mId;
			ignoredMethNumFrames = 1;
		}
		private int setCurrQid(BasicBlock bb) {
			int currQid = domQ.getOrAdd(new IntTrio(-1, top.mId, top.cId));
			if (prevQid != -1) {
				succSet.add(new IntPair(prevQid, currQid));
			}
			prevQid = currQid;
			if (currQid == domQ.size() - 1) {
				int p = domP.indexOf(bb);
				assert (p != -1);
				PQset.add(new IntPair(p, currQid));
			}
			return currQid;
		}
		private int setCurrQid(Quad q) {
			int i = q.getID();
			int currQid = domQ.getOrAdd(new IntTrio(i, top.mId, top.cId));
			if (prevQid != -1) {
				succSet.add(new IntPair(prevQid, currQid));
			}
			prevQid = currQid;
			if (currQid == domQ.size() - 1) {
				int p = domP.indexOf(q);
				assert (p != -1);
				PQset.add(new IntPair(p, currQid));
			}
			return currQid;
		}
		public void processEnterMethod(int mId) {
			assert (mId >= 0);
			if (ignoredMethNumFrames > 0) {
				if (mId == ignoredMethId)
					ignoredMethNumFrames++;
				return;
			}
			jq_Method m = domM.get(mId);
			String cName = m.getDeclaringClass().getName();
			jq_Class cls = Program.v().getPreparedClass(cName);
			if (cls == null) {
				System.out.println("WARNING: Missing class: " + cName);
				beginIgnoredMeth(mId);
				return;
			}
			if (DEBUG) System.out.println("T" + tId + " ENTER: " + m);
			String mName = m.getName().toString();
			String mDesc = m.getDesc().toString();
			String mSign = mName + mDesc;
			InvkInfo pendingInvk;
			if (top != null) {
 				pendingInvk = top.pendingInvk;
				frames.push(top);
			} else
				pendingInvk = null;
			int cId = methToNumCalls[mId]++;
			top = new Frame(mId, cId);
			int currQid = setCurrQid(m.getCFG().entry());
			List<IntPair> methArgs = methToArgs[mId];
			if (methArgs == null)
				methArgs = processMethVars(m, mId);
			TIntArrayList methTmps = methToTmps[mId];
			if (DEBUG) System.out.println("\tPending Invk: " + pendingInvk);
			if (pendingInvk != null && pendingInvk.sign.equals(mSign)) {
				if (DEBUG) System.out.println("\tMATCHES");
				List<IntPair> invkArgs = pendingInvk.invkArgs;
				int numArgs = methArgs.size();
				top.invkRet = pendingInvk.invkRet;
				top.isExplicit = true;
				assert (numArgs == invkArgs.size());
				for (int i = 0; i < numArgs; i++) {
					IntPair zv = methArgs.get(i);
					int zId = zv.idx0;
					int vId = zv.idx1;
					IntPair zu = invkArgs.get(i);
					assert (zu.idx0 == zId);
					int uId = zu.idx1;
					copySet.add(new IntTrio(currQid, vId, uId));
				}
			} else if (!foundThreadRoot &&
					(mSign.equals("main([Ljava/lang/String;)V") ||
						mSign.equals("run()V"))) {
				foundThreadRoot = true;
				System.out.println("Treating method " + m +
					" as thread root of thread# " + tId);
				IntPair thisArg = methArgs.get(0);
				assert (thisArg.idx0 == 0);
					int vId = thisArg.idx1;
				if (mSign.equals("run()V"))
					startSet.add(new IntPair(currQid, vId));
				else
					asgnSet.add(new IntPair(currQid, vId));
			} else {
				for (IntPair p : methArgs) {
					int vId = p.idx1;
					asgnSet.add(new IntPair(currQid, vId));
				}
			}
			int numTmps = methTmps.size();
			for (int i = 0; i < numTmps; i++) {
				int vId = methTmps.get(i);
				asgnSet.add(new IntPair(currQid, vId));
			}
		}
		public void processLeaveMethod(int mId) {
			assert (mId >= 0);
			if (top == null) {
				return;
			}
			if (ignoredMethNumFrames > 0) {
				if (mId == ignoredMethId)
					ignoredMethNumFrames--;
				return;
			}
			if (DEBUG) System.out.println("T" + tId + " LEAVE: " + domM.get(mId));
			assert (mId == top.mId);
			if (frames.isEmpty())
				top = null;
			else
				top = frames.pop();
		}
		public void processBasicBlock(int bId) {
			if (top == null) {
				return;
			}
			if (ignoredMethNumFrames > 0) {
				return;
			}
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
			if (top == null) {
				return;
			}
			if (ignoredMethNumFrames > 0) {
				return;
			}
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
			if (DEBUG) System.out.println("Quad: " + q);
			top.pendingInvk = null;
			Operator op = q.getOperator();
			if (op instanceof Invoke)
				processInvoke(q);
			else if (op instanceof RETURN_A)
				processReturn(q);
			else if (op instanceof Move || op instanceof CheckCast)
				processMove(q);
			else if (op instanceof Getfield)
				processGetfield(q);
			else if (op instanceof ALoad)
				processAload(q);
			else if (op instanceof Putfield)
				processPutfield(q);
				else if (op instanceof AStore)
				processAstore(q);
			else if (op instanceof Getstatic)
				processGetstatic(q);
			else if (op instanceof Putstatic)
				processPutstatic(q);
			else if (op instanceof New)
				processNewOrNewArray(q, true);
			else if (op instanceof NewArray)
				processNewOrNewArray(q, false);
			else if (op instanceof Phi)
				processPhi(q);
		}
		private void processPhi(Quad q) {
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			assert (t != null);
			if (!t.isReferenceType())
				return;
			BasicBlockTableOperand bo = Phi.getPreds(q);
			int n = bo.size();
			int i = 0;
			assert (top.prevBB != null);
			for (; i < n; i++) {
				BasicBlock bb = bo.get(i);
				if (bb == top.prevBB)
					break;
			}
			assert (i < n);
			RegisterOperand ro = Phi.getSrc(q, i);
			Register r = ro.getRegister();
			int rId = domV.indexOf(r);
			assert (rId != -1);
			Register l = lo.getRegister();
			int lId = domV.indexOf(l);
			assert (lId != -1);
			int currQid = setCurrQid(q);
			copySet.add(new IntTrio(currQid, lId, rId));
		}
		private void processNewOrNewArray(Quad q, boolean isNew) {
			RegisterOperand vo = isNew ? New.getDest(q) : NewArray.getDest(q);
			Register v = vo.getRegister();
			int vId = domV.indexOf(v);
			assert (vId != -1);
			int hId = domH.indexOf(q);
			assert (hId != -1);
			int currQid = setCurrQid(q);
			allocSet.add(new IntTrio(currQid, vId, hId));
		}
		private void processMove(Quad q) {
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
			RegisterOperand lo = Move.getDest(q);
			Register l = lo.getRegister();
			int lId = domV.indexOf(l);
			assert (lId != -1);
			int currQid = setCurrQid(q);
			if (ro != null) {
				Register r = ro.getRegister();
				int rId = domV.indexOf(r);
				assert (rId != -1);
				copySet.add(new IntTrio(currQid, lId, rId));
			} else {
				asgnSet.add(new IntPair(currQid, lId));
			}
		}
		private void processGetstatic(Quad q) {
        	jq_Field f = Getstatic.getField(q).getField();
        	if (!f.getType().isReferenceType())
        		return;
			RegisterOperand lo = Getstatic.getDest(q);
			Register l = lo.getRegister();
			int lId = domV.indexOf(l);
			assert (lId != -1);
			int currQid = setCurrQid(q);
			getstatSet.add(new IntPair(currQid, lId));
		}
		private void processPutstatic(Quad q) {
        	jq_Field f = Putstatic.getField(q).getField();
        	if (!f.getType().isReferenceType())
        		return;
        	Operand rx = Putstatic.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand ro = (RegisterOperand) rx;
			Register r = ro.getRegister();
			int rId = domV.indexOf(r);
			assert (rId != -1);
			int currQid = setCurrQid(q);
			putstatSet.add(new IntPair(currQid, rId));
		}
		private void processAload(Quad q) {
			int currQid = setCurrQid(q);
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			Register b = bo.getRegister();
			int bId = domV.indexOf(b);
			assert (bId != -1);
			baseQVset.add(new IntPair(currQid, bId));
			if (!((ALoad) q.getOperator()).getType().isReferenceType())
				return;
			RegisterOperand lo = ALoad.getDest(q);
			Register l = lo.getRegister();
			int lId = domV.indexOf(l);
			assert (lId != -1);
			int fId = 0;
			getinstSet.add(new IntQuad(currQid, lId, bId, fId));
		}
		private void processAstore(Quad q) {
			int currQid = setCurrQid(q);
			RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
			Register b = bo.getRegister();
			int bId = domV.indexOf(b);
			assert (bId != -1);
			baseQVset.add(new IntPair(currQid, bId));
			if (!((AStore) q.getOperator()).getType().isReferenceType())
				return;
			Operand rx = AStore.getValue(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand ro = (RegisterOperand) rx;
			Register r = ro.getRegister();
			int rId = domV.indexOf(r);
			assert (rId != -1);
			int fId = 0;
			putinstSet.add(new IntQuad(currQid, bId, fId, rId));
		}
		private void processGetfield(Quad q) {
			int currQid = setCurrQid(q);
			Operand bx = Getfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			Register b = bo.getRegister();
			int bId = domV.indexOf(b);
			assert (bId != -1);
			baseQVset.add(new IntPair(currQid, bId));
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
			int lId = domV.indexOf(l);
			assert (lId != -1);
			getinstSet.add(new IntQuad(currQid, lId, bId, fId));
		}
		private void processPutfield(Quad q) {
			int currQid = setCurrQid(q);
			Operand bx = Putfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			Register b = bo.getRegister();
			int bId = domV.indexOf(b);
			assert (bId != -1);
			baseQVset.add(new IntPair(currQid, bId));
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
			int rId = domV.indexOf(r);
			assert (rId != -1);
			putinstSet.add(new IntQuad(currQid, bId, fId, rId));
		}
		private void processInvoke(Quad q) {
			List<IntPair> invkArgs = null;
			ParamListOperand lo = Invoke.getParamList(q);
			int numArgs = lo.length();
			for (int i = 0; i < numArgs; i++) {
				RegisterOperand vo = lo.get(i);
				Register v = vo.getRegister();
				if (v.getType().isReferenceType()) {
					int vId = domV.indexOf(v);
					assert (vId != -1);
					if (invkArgs == null)
						invkArgs = new ArrayList<IntPair>();
					invkArgs.add(new IntPair(i, vId));
				}
			}
			if (invkArgs == null)
				invkArgs = emptyArgsList;
			int invkRet = -1;
			RegisterOperand vo = Invoke.getDest(q);
			if (vo != null) {
				Register v = vo.getRegister();
				if (v.getType().isReferenceType()) {
					invkRet = domV.indexOf(v);
					assert (invkRet != -1);
				}
			}
			jq_Method m = Invoke.getMethod(q).getMethod();
			String mName = m.getName().toString();
			String mDesc = m.getDesc().toString();
			String mSign = mName + mDesc;
			if (mSign.equals("start()V") &&
					m.getDeclaringClass().getName().equals("java.lang.Thread")) {
				int currQid = setCurrQid(q);
				IntPair thisArg = invkArgs.get(0);
				assert (thisArg.idx0 == 0);
				int vId = thisArg.idx1;
				spawnSet.add(new IntPair(currQid, vId));
			} else {
				if (DEBUG) System.out.println("Setting top.pendingInvk: " +
					mSign + " " + invkRet);
				InvkInfo invkInfo = new InvkInfo(mSign, invkArgs, invkRet);
				top.pendingInvk = invkInfo;
			}
		}
		private void processReturn(Quad q) {
			boolean isExplicit = top.isExplicit;
			if (DEBUG) System.out.println("Return: " + domM.get(top.mId) + " " +
				isExplicit + " " + top.invkRet);
			if (!isExplicit)
				return;
			int invkRet = top.invkRet;
			if (invkRet == -1)
				return;
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				Register v = ro.getRegister();
				int methRet = domV.indexOf(v);
				assert (methRet != -1);
				int currQid = setCurrQid(q);
				copySet.add(new IntTrio(currQid, invkRet, methRet));
			}
		}
	}
}

