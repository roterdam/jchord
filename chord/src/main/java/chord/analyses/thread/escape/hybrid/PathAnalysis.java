package chord.analyses.thread.escape.hybrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Collections;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

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
import chord.doms.DomB;
import chord.doms.DomM;
import chord.doms.DomH;
import chord.doms.DomF;
import chord.doms.DomV;
import chord.doms.DomP;
import chord.project.Program;
import chord.project.Project;
import chord.project.ProgramDom;
import chord.project.ProgramRel;
import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.instr.InstrScheme;

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
    name = "path-java"
)
public class PathAnalysis extends DynamicAnalysis {
	private final static boolean DEBUG = true;
    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
        if (instrScheme != null)
            return instrScheme;
        instrScheme = new InstrScheme();
        instrScheme.setInstrMethodAndLoopBound(1);
        return instrScheme;
    }
    // data structures set once and for all
	private DomV domV;
	private DomF domF;
	private DomH domH;
	private DomM domM;
	private DomP domP;
	private DomB domB;
	private List/*IntPair*/[] methToArgs;
    // set of heap insts deemed escaping in some run so far
	private Set<Quad> escHeapInsts;
	private Map<Quad, Set<Quad>> heapInstToAllocs;
	
	// data structures set for each run
	private TIntObjectHashMap<Handler> thrToHandlerMap =
		new TIntObjectHashMap<Handler>();
	private int[] methToNumCalls;
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
	private Set<IntPair> PQset;

    public void initAllPasses() {
		domH = (DomH) Project.getTrgt("H");
		domV = (DomV) Project.getTrgt("V");
		domF = (DomF) Project.getTrgt("F");
		domM = (DomM) Project.getTrgt("M");
		domP = (DomP) Project.getTrgt("P");
		domB = (DomB) Project.getTrgt("B");
		domQ = (ProgramDom) Project.getTrgt("Q");
        Project.runTask("M");
        Project.runTask("H");
        Project.runTask("V");
        Project.runTask("F");
        Project.runTask("P");
        Project.runTask("B");
    	methToArgs = new List[domM.size()];
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
		PQset = new HashSet<IntPair>();
	}

	public void processEnterMethod(int mIdx, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		if (handler == null) {
			handler = new Handler(tId);
			thrToHandlerMap.put(tId, handler);
		}
		handler.processEnterMethod(mIdx);
	}
    public void processEnterBasicBlock(int bIdx, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		assert (handler != null);
		handler.processEnterBasicBlock(bIdx);
	}
    public void processLeaveMethod(int mIdx, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		assert (handler != null);
		handler.processLeaveMethod(mIdx);
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
			// writer.println("getstat('q" + q + "', 'v" + v + "').");
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
            if (escHeapInsts.add(e)) {
                // may have been deemed thread local
                // in an earlier path program
                if (heapInstToAllocs.remove(e) != null)
                    System.out.println("Deemed loc in earlier path: " + e);
            }
        }
        relHybridEscE.close();
	}

	private List<IntPair> processMethArgs(jq_Method m) {
		List<IntPair> args = null;
		ControlFlowGraph cfg = m.getCFG();
		assert (cfg != null);
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = m.getParamTypes().length;
		for (int zIdx = 0; zIdx < numArgs; zIdx++) {
			Register v = rf.get(zIdx);
			if (v.getType().isReferenceType()) {
				int vIdx = domV.indexOf(v);
				assert (vIdx != -1);
				if (args == null)
					args = new ArrayList<IntPair>();
				args.add(new IntPair(zIdx, vIdx));
			}
		}
		if (args == null)
			args = Collections.emptyList();
		return args;
	}

	class Frame {
		final int mIdx;
		final int cIdx;
		final List<IntPair> invkArgs;
		final int invkRet;
		private BasicBlock prevBB;
		public Frame(int mIdx, int cIdx,
				List<IntPair> invkArgs, int invkRet) {
			this.mIdx = mIdx;
			this.cIdx = cIdx;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
		}
	}
	class InvkInfo {
		final String sign;
		final List<IntPair> invkArgs;
		final int invkRet;
		final int invkQidx;
		public InvkInfo(String sign, List<IntPair> invkArgs,
				int invkRet, int invkQidx) {
			this.sign = sign;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
			this.invkQidx = invkQidx;
		}
	}
	class Handler {
		private Stack<InvkInfo> pendingInvks = new Stack<InvkInfo>();
		private Stack<Frame> frames = new Stack<Frame>();
		private Frame top;
		private boolean foundThreadRoot;
		private int ignoredMethIdx;
		private int ignoredMethNumFrames;  // > 0 means currently ignoring code
		private int prevQidx, currQidx;
		private int tId; // just for debugging
		public Handler(int tId) {
			this.tId = tId;
			prevQidx = -1;
		}
		public void beginIgnoredMeth(int mIdx) {
			ignoredMethIdx = mIdx;
			ignoredMethNumFrames = 1;
		}
		private void setCurrQidx(BasicBlock bb) {
			currQidx = domQ.getOrAdd(new IntTrio(-1, top.mIdx, top.cIdx));
			if (prevQidx != -1)
				succSet.add(new IntPair(prevQidx, currQidx));
			prevQidx = currQidx;
			if (currQidx == domQ.size() - 1) {
				int p = domP.indexOf(bb);
				assert (p != -1);
				PQset.add(new IntPair(p, currQidx));
			}
		}
		private void setCurrQidx(Quad q) {
			int i = q.getID();
			currQidx = domQ.getOrAdd(new IntTrio(i, top.mIdx, top.cIdx));
			if (prevQidx != -1)
				succSet.add(new IntPair(prevQidx, currQidx));
			prevQidx = currQidx;
			if (currQidx == domQ.size() - 1) {
				int p = domP.indexOf(q);
				assert (p != -1);
				PQset.add(new IntPair(p, currQidx));
			}
		}
		public void processEnterBasicBlock(int bIdx) {
			System.out.println("BB tId: " + tId + " bIdx: " + bIdx);
			assert (bIdx >= 0);
			if (top == null) {
				System.out.println("Ignoring 1");
				return;
			}
			if (ignoredMethNumFrames > 0) {
				System.out.println("Ignoring 2");
				return;
			}
			BasicBlock bb = domB.get(bIdx);
			int n = bb.size();
			assert (n > 0);
			for (int x = 0; x < n; x++) {
				Quad q = bb.getQuad(x);
				System.out.println("\tQuad: " + q);
				Operator op = q.getOperator();
				if (op instanceof Invoke)
					processInvoke(q);
				else if (op instanceof Return) {
					assert (!(op instanceof RETURN_P));
					if (op instanceof RETURN_A)
						processReturn(q);
				} else if (op instanceof Move)
					processMove(q);
				else if (op instanceof Getfield)
					processGetfield(q);
				else if (op instanceof ALoad) {
					if (((ALoad) op).getType().isReferenceType())
						processAload(q);
				} else if (op instanceof Putfield)
					processPutfield(q);
				else if (op instanceof AStore) {
					if (((AStore) op).getType().isReferenceType())
						processAstore(q);
				} else if (op instanceof Getstatic)
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
			top.prevBB = bb;
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
			for (; i < n; i++) {
				BasicBlock bb = bo.get(i);
				if (bb == top.prevBB)
					break;
			}
			assert (i < n);
			RegisterOperand ro = Phi.getSrc(q, i);
			Register r = ro.getRegister();
			int rIdx = domV.indexOf(r);
			assert (rIdx != -1);
			Register l = lo.getRegister();
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			setCurrQidx(q);
			copySet.add(new IntTrio(currQidx, lIdx, rIdx));
		}
		private void processNewOrNewArray(Quad q, boolean isNew) {
			RegisterOperand vo = isNew ? New.getDest(q) : NewArray.getDest(q);
			Register v = vo.getRegister();
			int vIdx = domV.indexOf(v);
			assert (vIdx != -1);
			int hIdx = domH.indexOf(q);
			assert (hIdx != -1);
			setCurrQidx(q);
			allocSet.add(new IntTrio(currQidx, vIdx, hIdx));
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
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			setCurrQidx(q);
			if (ro != null) {
				Register r = ro.getRegister();
				int rIdx = domV.indexOf(r);
				assert (rIdx != -1);
				copySet.add(new IntTrio(currQidx, lIdx, rIdx));
			} else
				asgnSet.add(new IntPair(currQidx, lIdx));
		}
		private void processGetstatic(Quad q) {
        	jq_Field f = Getstatic.getField(q).getField();
        	if (!f.getType().isReferenceType())
        		return;
			RegisterOperand lo = Getstatic.getDest(q);
			Register l = lo.getRegister();
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			setCurrQidx(q);
			getstatSet.add(new IntPair(currQidx, lIdx));
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
			int rIdx = domV.indexOf(r);
			assert (rIdx != -1);
			setCurrQidx(q);
			putstatSet.add(new IntPair(currQidx, rIdx));
		}
		private void processAload(Quad q) {
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			RegisterOperand lo = ALoad.getDest(q);
			Register b = bo.getRegister();
			Register l = lo.getRegister();
			int bIdx = domV.indexOf(b);
			assert (bIdx != -1);
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			int fIdx = 0;
			setCurrQidx(q);
			getinstSet.add(new IntQuad(currQidx, lIdx, bIdx, fIdx));
		}
		private void processAstore(Quad q) {
			Operand rx = AStore.getValue(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand ro = (RegisterOperand) rx;
			RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
			Register r = ro.getRegister();
			Register b = bo.getRegister();
			int rIdx = domV.indexOf(r);
			assert (rIdx != -1);
			int bIdx = domV.indexOf(b);
			assert (bIdx != -1);
			int fIdx = 0;
			setCurrQidx(q);
			putinstSet.add(new IntQuad(currQidx, bIdx, fIdx, rIdx));
		}
		private void processGetfield(Quad q) {
			jq_Field f = Getfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			Operand bx = Getfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			RegisterOperand lo = Getfield.getDest(q);
			Register b = bo.getRegister();
			Register l = lo.getRegister();
			int bIdx = domV.indexOf(b);
			assert (bIdx != -1);
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			int fIdx = domF.indexOf(f);
			assert (fIdx != -1);
			setCurrQidx(q);
			getinstSet.add(new IntQuad(currQidx, lIdx, bIdx, fIdx));
		}
		private void processPutfield(Quad q) {
			jq_Field f = Putfield.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			Operand bx = Putfield.getBase(q);
			if (!(bx instanceof RegisterOperand))
				return;
			Operand rx = Putfield.getSrc(q);
			if (!(rx instanceof RegisterOperand))
				return;
			RegisterOperand bo = (RegisterOperand) bx;
			RegisterOperand ro = (RegisterOperand) rx;
			Register b = bo.getRegister();
			Register r = ro.getRegister();
			int bIdx = domV.indexOf(b);
			assert (bIdx != -1);
			int rIdx = domV.indexOf(r);
			assert (rIdx != -1);
			int fIdx = domF.indexOf(f);
			assert (fIdx != -1);
			setCurrQidx(q);
			putinstSet.add(new IntQuad(currQidx, bIdx, fIdx, rIdx));
		}
		private void processInvoke(Quad q) {
			List<IntPair> invkArgs = null;
			ParamListOperand lo = Invoke.getParamList(q);
			int numArgs = lo.length();
			for (int i = 0; i < numArgs; i++) {
				RegisterOperand vo = lo.get(i);
				Register v = vo.getRegister();
				if (v.getType().isReferenceType()) {
					int vIdx = domV.indexOf(v);
					assert (vIdx != -1);
					if (invkArgs == null)
						invkArgs = new ArrayList<IntPair>();
					invkArgs.add(new IntPair(i, vIdx));
				}
			}
			if (invkArgs == null)
				invkArgs = Collections.emptyList();
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
			setCurrQidx(q);
			if (mSign.equals("start()V") &&
					m.getDeclaringClass().getName().equals("java.lang.Thread")) {
				IntPair thisArg = invkArgs.get(0);
				assert (thisArg.idx0 == 0);
				spawnSet.add(new IntPair(currQidx, thisArg.idx1));
			} else {
				InvkInfo invkInfo =
					new InvkInfo(mSign, invkArgs, invkRet, currQidx);
				pendingInvks.push(invkInfo);
			}
		}
		private void processReturn(Quad q) {
			int invkRet = top.invkRet;
			if (invkRet == -1)
				return;
			Operand rx = Return.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				Register v = ro.getRegister();
				int methRet = domV.indexOf(v);
				assert (methRet != -1);
				setCurrQidx(q);
				copySet.add(new IntTrio(currQidx, invkRet, methRet));
			}
		}
		public void processEnterMethod(int mIdx) {
			assert (mIdx >= 0);
			System.out.println("EM tId: " + tId + " mIdx: " + mIdx);
			if (ignoredMethNumFrames > 0) {
				System.out.println("Ignoring");
				if (mIdx == ignoredMethIdx)
					ignoredMethNumFrames++;
				return;
			}
			jq_Method m = domM.get(mIdx);
			String cName = m.getDeclaringClass().getName();
			jq_Class cls = Program.v().getPreparedClass(cName);
			if (cls == null) {
				System.out.println("Missing class: " + cName);
				beginIgnoredMeth(mIdx);
				return;
			}
			String mName = m.getName().toString();
			String mDesc = m.getDesc().toString();
			String mSign = mName + mDesc;
			List<IntPair> invkArgs = null;
			int invkRet = -1;
			boolean found = false;
			if (!pendingInvks.isEmpty()) {
				InvkInfo invkInfo = pendingInvks.peek();
				if (invkInfo.sign.equals(mSign)) {
					found = true;
					pendingInvks.pop();
					invkArgs = invkInfo.invkArgs;
					invkRet = invkInfo.invkRet;
					int invkQidx = invkInfo.invkQidx;
					List<IntPair> methArgs = methToArgs[mIdx];
					if (methArgs == null) {
						methArgs = processMethArgs(m);
						methToArgs[mIdx] = methArgs;
					}
					int numArgs = methArgs.size();
					assert (numArgs == invkArgs.size());
					for (int i = 0; i < numArgs; i++) {
						IntPair zv = methArgs.get(i);
						int zIdx = zv.idx0;
						int vIdx = zv.idx1;
						IntPair zu = invkArgs.get(i);
						assert (zu.idx0 == zIdx);
						int uIdx = zu.idx1;
						copySet.add(new IntTrio(invkQidx, vIdx, uIdx));
					}
				}
			}
			if (top != null)
				frames.push(top);
			int cIdx = methToNumCalls[mIdx]++;
			System.out.println("XXX: " + m);
			top = new Frame(mIdx, cIdx, invkArgs, invkRet);
			if (!found && !foundThreadRoot) {
				if (mSign.equals("main([Ljava/lang/String;)V") ||
						mSign.equals("run()V")) {
					foundThreadRoot = true;
					System.out.println("Treating method '" + m +
						"' as thread root of thread# " + tId);
					List<IntPair> methArgs = methToArgs[mIdx];
					if (methArgs == null) {
						methArgs = processMethArgs(m);
						methToArgs[mIdx] = methArgs;
					}
					IntPair thisArg = methArgs.get(0);
					assert (thisArg.idx0 == 0);
					setCurrQidx(m.getCFG().entry());
					startSet.add(new IntPair(currQidx, thisArg.idx1));
				}
			}
		}
		public void processLeaveMethod(int mIdx) {
			System.out.println("LM tId: " + tId + " mIdx: " + mIdx);
			assert (mIdx >= 0);
			if (top == null) {
				System.out.println("Ignoring 1");
				return;
			}
			if (ignoredMethNumFrames > 0) {
				System.out.println("Ignoring 2");
				if (mIdx == ignoredMethIdx)
					ignoredMethNumFrames--;
				return;
			}
			assert (mIdx == top.mIdx);
			if (frames.isEmpty())
				top = null;
			else
				top = frames.pop();
		}
	}
}

