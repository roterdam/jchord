package chord.analyses.thread.escape.hybrid;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

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
    private TIntObjectHashMap[] methToCode;
	private List/*IntPair*/[] methToArgs;
    // set of heap insts deemed escaping in some run so far
	private Set<Quad> esc1HeapInsts = new HashSet<Quad>();
	private Map<Quad, Set<Quad>> heapInstToAllocs =
		new HashMap<Quad, Set<Quad>>();

	// data structures set for each run
	private int currQidx;
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
        int numM = domM.size();
    	methToCode = new TIntObjectHashMap[numM];
    	methToArgs = new List[numM];
    }

	public void initPass() {
		thrToHandlerMap.clear();
		methToNumCalls = new int[domM.size()];
		currQidx = 0;
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
		PQset = new HashSet<IntPair>();
	}

	public void processEnterMethod(int mId, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		if (handler == null) {
			handler = new Handler();
			thrToHandlerMap.put(tId, handler);
		}
		handler.processEnterMethod(mId);
	}
    public void processEnterBasicBlock(int pId, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		assert (handler != null);
		handler.processEnterBasicBlock(pId);
	}
    public void processLeaveMethod(int mId, int tId) {
		Handler handler = thrToHandlerMap.get(tId);
		assert (handler != null);
		handler.processLeaveMethod(mId);
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
            if (esc1HeapInsts.contains(e)) {
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
            if (esc1HeapInsts.add(e)) {
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
				if (args == null)
					args = new ArrayList<IntPair>();
				args.add(new IntPair(zIdx, vIdx));
			}
		}
		return args;
	}

	class Frame {
		int mId;
		int mIdx;
		int cIdx;
		List<IntPair> invkArgs;
		int invkRet;
		public Frame(int mId, int mIdx, int cIdx,
				List<IntPair> invkArgs, int invkRet) {
			this.mId = mId;
			this.mIdx = mIdx;
			this.cIdx = cIdx;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
		}
	}
	class InvkInfo {
		String sign;
		List<IntPair> invkArgs;
		int invkRet;
		public InvkInfo(String sign, List<IntPair> invkArgs, int invkRet) {
			this.sign = sign;
			this.invkArgs = invkArgs;
			this.invkRet = invkRet;
		}
	}
	class Handler {
		private Stack<InvkInfo> pendingInvks = new Stack<InvkInfo>();
		private Stack<Frame> frames = new Stack<Frame>();
		private Frame top;
		// if badMid != -1 then in eating mode until
		// leaveMethod badMid encountered
		private int badMid = -1; 
		private int numCalls;
		public void beginBad(int mId) {
			badMid = mId;
			numCalls = 0;
		}
		public void processEnterBasicBlock(int bId) {
			if (badMid != -1) {
				return;
			}
			int bIdx = getBidx(bId);
			assert (bIdx != -1);
			BasicBlock bb = domB.get(bIdx);
			int n = bb.size();
			// succSet.add(new IntPair(prevQidx, currQidx));
			for (int x = 0; x < n; x++) {
				Quad q = bb.getQuad(x);
				System.out.println("\tQuad: " + q);
				Operator op = q.getOperator();
				int currQidx = domQ.getOrAdd(new IntTrio(q.getID(), top.mIdx, top.cIdx));
				if (currQidx == domQ.size() - 1) {
					int p = domP.indexOf(q);
					PQset.add(new IntPair(p, currQidx));
				}
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
					throw new RuntimeException("TODO");
				else
					throw new RuntimeException("Invalid quad: " + q);
			}
		}
		private void processNewOrNewArray(Quad q, boolean isNew) {
			RegisterOperand vo = isNew ? New.getDest(q) : NewArray.getDest(q);
			Register v = vo.getRegister();
			int vIdx = domV.indexOf(v);
			int hIdx = domH.indexOf(q);
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
			putstatSet.add(new IntPair(currQidx, rIdx));
		}
		private void processAload(Quad q) {
			RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
			RegisterOperand lo = ALoad.getDest(q);
			Register b = bo.getRegister();
			Register l = lo.getRegister();
			int bIdx = domV.indexOf(b);
			int lIdx = domV.indexOf(l);
			int fIdx = 0;
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
			int bIdx = domV.indexOf(b);
			int fIdx = 0;
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
			int lIdx = domV.indexOf(l);
			int fIdx = domF.indexOf(f);
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
			int rIdx = domV.indexOf(r);
			int fIdx = domF.indexOf(f);
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
					if (invkArgs == null)
						invkArgs = new ArrayList<IntPair>();
					invkArgs.add(new IntPair(i, vIdx));
				}
			}
			int invkRet = -1;
			RegisterOperand vo = Invoke.getDest(q);
			if (vo != null) {
				Register v = vo.getRegister();
				if (v.getType().isReferenceType()) {
					invkRet = domV.indexOf(v);
				}
			}
			jq_Method m = Invoke.getMethod(q).getMethod();
			String mName = m.getName().toString();
			String mDesc = m.getDesc().toString();
			String mSign = mName + mDesc;
			if (mSign.equals("start()V") &&
					m.getDeclaringClass().getName().equals("java.lang.Thread")) {
				IntPair thisArg = invkArgs.get(0);
				assert (thisArg.idx0 == 0);
				spawnSet.add(new IntPair(currQidx, thisArg.idx1));
			} else {
				InvkInfo invkInfo = new InvkInfo(mSign, invkArgs, invkRet);
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
				copySet.add(new IntTrio(currQidx, invkRet, methRet));
			}
		}
		public void processEnterMethod(int mId) {
			if (badMid != -1) {
				if (mId == badMid)
					numCalls++;
				return;
			}
			int mIdx = getMidx(mId);
			if (mIdx == -1) {
				System.out.println("MISSING method: "); // TODO
				beginBad(mId);
				return;
			}
			jq_Method m = domM.get(mIdx);
			String cName = m.getDeclaringClass().getName();
			jq_Class cls;
			if (cName.startsWith("java.lang.") || cName.startsWith("com.ibm."))
				cls = null;
			else
				cls = Program.v().getPreparedClass(cName);
			if (cls == null) {
				System.out.println("MISSING class: " + cName);
				beginBad(mId);
				return;
			}
			int cIdx = methToNumCalls[mIdx]++;
			List<IntPair> methArgs = methToArgs[mIdx];
			if (methArgs == null) {
				methArgs = processMethArgs(m);
				methToArgs[mIdx] = methArgs;
			}
			if (top != null)
				frames.push(top);
			List<IntPair> invkArgs = null;
			int invkRet = -1;
			if (!pendingInvks.isEmpty()) {
				InvkInfo invkInfo = pendingInvks.peek();
				String mName = m.getName().toString();
				String mDesc = m.getDesc().toString();
				String mSign = mName + mDesc;
				if (invkInfo.sign.equals(mSign)) {
					pendingInvks.pop();
					invkArgs = invkInfo.invkArgs;
					invkRet = invkInfo.invkRet;
					int numArgs = methArgs.size();
					assert (numArgs == invkArgs.size());
					for (int i = 0; i < numArgs; i++) {
						IntPair zv = methArgs.get(i);
						int zIdx = zv.idx0;
						int vIdx = zv.idx1;
						IntPair zu = invkArgs.get(i);
						assert (zu.idx0 == zIdx);
						int uIdx = zu.idx1;
						copySet.add(new IntTrio(currQidx, vIdx, uIdx));
					}
				}
			}
			top = new Frame(mId, mIdx, cIdx, invkArgs, invkRet);
		}
		public void processLeaveMethod(int mId) {
			if (badMid != -1) {
				if (mId == badMid) {
					if (numCalls == 0)
						badMid = -1;
					else
						numCalls--;
				}
				return;
			}
			assert (mId == top.mId);
			if (frames.isEmpty())
				top = null;
			else
				top = frames.pop();
		}
	}
}

