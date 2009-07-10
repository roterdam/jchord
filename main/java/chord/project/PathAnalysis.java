package chord.project;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
import java.util.HashSet;
import java.util.Set;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

import chord.util.IntBuffer;
import chord.util.FileUtils;
import chord.util.Assertions;
import chord.util.IndexMap;
import chord.util.PropertyUtils;
import chord.util.ProcessExecutor;
import chord.util.tuple.object.Pair;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntQuad;

import chord.doms.DomM;
import chord.doms.DomH;
import chord.doms.DomF;
import chord.doms.DomV;
import chord.doms.DomP;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Class.jq_Type;
import joeq.Class.jq_Class;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.*;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.*;
import joeq.Compiler.Quad.Operator.Return.*;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

@Chord(
    name = "path-java"
)
public class PathAnalysis implements ITask {
	private final static boolean DEBUG = false;

    protected String name;
	private int numThreads;
	private List<List<String>> threads;
	private List<String> currThread;
	private int currLineIdx;
	private TIntObjectHashMap[] methToCode;
	private List/*IntPair*/[] methToArgs;
	private int[] methToNumCalls;

	private ProgramDom<IntTrio> domQ;
	private DomV domV;
	private DomF domF;
	private DomH domH;
	private DomM domM;
	private DomP domP;
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

	private PrintWriter currThreadFullOut;
	private PrintWriter currThreadAbbrOut;

    public void setName(String name) {
        Assertions.Assert(name != null);
        Assertions.Assert(this.name == null);
        this.name = name;
    }
    public String getName() {
        return name;
    }

	public void run() {
		try {
			final String mainClassName = Properties.mainClassName;
			Assertions.Assert(mainClassName != null);
			final String classPathName = Properties.classPathName;
			Assertions.Assert(classPathName != null);
	
			String traceFileName = (new File(Properties.outDirName,
				Properties.traceFileName)).getAbsolutePath();
	
			String runIdsStr = System.getProperty("chord.run.ids", "");
	
			String[] runIds = runIdsStr.split(",");
			String jvmargs = System.getProperty("chord.jvmargs", "");
			String cmd = "java " + jvmargs + " -cp " + classPathName +
   	     	" -agentlib:tracing_agent" +
				"=trace_file_name=" + traceFileName +
				" " + mainClassName + " ";
			for (String runId : runIds) {
				System.out.println("Run ID: " + runId);
				String args = System.getProperty("chord.args." + runId, "");
				ProcessExecutor.execute(cmd + args);
				createThreads(traceFileName);
				processThreads();
				createRels();
				runAnalysis();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void createThreads(String fileName) throws IOException {
		List<String> trace = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		{
			String line;
			while ((line = reader.readLine()) != null)
				trace.add(line);
		}
		reader.close();

		String lastLine = trace.remove(trace.size() - 1);
		numThreads = Integer.parseInt(lastLine);
		threads = new ArrayList<List<String>>(numThreads);
		for (int i = 0; i < numThreads; i++)
			threads.add(new ArrayList<String>());
		for (String line : trace) {
			String[] a = line.split(" ");
			int tid = Integer.parseInt(a[0]) - 1;
			List<String> thread = threads.get(tid);
			if (thread == null) {
				// ignore thread marked bad
				continue;
			}
			if (thread.isEmpty()) {
				// encountering thread for first time;
				// decide whether to mark it bad (i.e. set it to null)
				Assertions.Assert(a.length == 3);
				if (a[1].equals("X")) {
					threads.set(tid, null);
					System.out.println("NULLING THREAD " + (tid + 1));
					continue;
				}
			}
			if (a.length == 2) {
				thread.add(a[1]);
			} else {
				Assertions.Assert(a.length == 3);
				thread.add(a[1] + " " + a[2]);
			}
		}
		System.out.println("NUM THREADS: " + numThreads);
		for (int i = 0; i < numThreads; i++) {
			List<String> thread = threads.get(i);
			System.out.println("THREAD " + (i + 1) + " SIZE: " +
				((thread == null) ? 0 : thread.size()));
		}
	}

	private void processThreads() throws Exception {
		domH = (DomH) Project.getTrgt("H");
		domV = (DomV) Project.getTrgt("V");
		domF = (DomF) Project.getTrgt("F");
		domM = (DomM) Project.getTrgt("M");
		domP = (DomP) Project.getTrgt("P");
		domQ = (ProgramDom) Project.getTrgt("Q");
		domQ.clear();
		Project.runTask("M");
		Project.runTask("H");
		Project.runTask("V");
		Project.runTask("F");
		Project.runTask("P");

		int numM = domM.size();
		methToCode = new TIntObjectHashMap[numM];
		methToArgs = new List[numM];
		methToNumCalls = new int[numM];

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

		for (int t = 0; t < numThreads; t++) {
			currThread = threads.get(t);
			if (currThread == null)
				continue;
			int numLines = currThread.size();
			System.out.println("CURR THREAD: " + (t + 1) + " NUM LINES: " + numLines);
			Assertions.Assert(numLines > 0);
			currLineIdx = 0;
			currThreadFullOut = new PrintWriter(new FileWriter(
				new File(Properties.outDirName, "trace.full." + (t + 1) + ".txt")));
			currThreadAbbrOut = new PrintWriter(new FileWriter(
				new File(Properties.outDirName, "trace.abbr." + (t + 1) + ".txt")));
			do {
				String line = currThread.get(currLineIdx++);
				frame(line, null, -1, -1);
			} while (currLineIdx < numLines);
			currThreadFullOut.close();
			currThreadAbbrOut.close();
		}

		domQ.save();
	}

	private void eatUntil(String mStr) {
		// eat up all lines upto and including matching method exit event
		int numCalls = 0;
		while (true) {
			String line = currThread.get(currLineIdx++);
			currThreadFullOut.println(line);
			if (DEBUG) System.out.println("\tLINE ATE: " + line);
			char c = line.charAt(0);
			if (c == 'E') {
				String mStr2 = line.substring(2);
				if (mStr2.equals(mStr))
					numCalls++;
			} else if (c == 'X') {
				String mStr2 = line.substring(2);
				if (mStr2.equals(mStr)) {
					if (numCalls == 0)
						break;
					numCalls--;
				}
			}	
		}
	}

	private int frame(String methEntryLine, List<IntPair> invkArgs, int invkRet,
			int retQidx) {
		currThreadFullOut.println(methEntryLine);
		if (DEBUG) {
			System.out.print("ENTER frame: " + methEntryLine +
				" retQidx: " + retQidx + " invkRet: " + invkRet + " invKArgs: ");
			if (invkArgs == null) {
				System.out.println("null");
			} else {
				for (IntPair invkArg : invkArgs)
					System.out.print(invkArg + ",");
				System.out.println();
			}
		}
		Assertions.Assert(methEntryLine.charAt(0) == 'E');
		String mStr = methEntryLine.substring(2);
   		Assertions.Assert(mStr.startsWith("L"));
		int semiColon = mStr.indexOf(';');
		String cName = mStr.substring(1, semiColon).replace('/', '.');
		int openParen = mStr.indexOf('(');
		String mName = mStr.substring(semiColon + 1, openParen);
		String mDesc = mStr.substring(openParen);

		jq_Class cls;
/*
		if (cName.startsWith("java.lang.") || cName.startsWith("com.ibm."))
			cls = null;
		else
*/
			cls = Program.getClass(cName);
		if (cls == null) {
			System.out.println(cName + " MISSING");
			eatUntil(mStr);
			if (DEBUG) System.out.println("LEAVE1 frame: " + methEntryLine);
			return retQidx;
		}
		currThreadAbbrOut.println(methEntryLine);

		jq_NameAndDesc nad = new jq_NameAndDesc(mName, mDesc);
		jq_Method m = (jq_Method) cls.getDeclaredMember(nad);
		Assertions.Assert(m != null);
		int mIdx = domM.get(m);
		Assertions.Assert(mIdx != -1);
		int cIdx = methToNumCalls[mIdx]++;
		TIntObjectHashMap methCode = methToCode[mIdx];
		List<IntPair> methArgs = null;
		if (methCode == null) {
			methCode = processMethCode(m);
			if (methCode == null) {
				eatUntil(mStr);
				if (DEBUG) System.out.println("LEAVE2 frame: " + methEntryLine);
				return retQidx;
			}
			methToCode[mIdx] = methCode;
			methArgs = processMethArgs(m);
			methToArgs[mIdx] = methArgs;
		} else
			methArgs = methToArgs[mIdx];

		if (invkArgs != null) {
			Assertions.Assert(retQidx != -1);
			if (methArgs != null) {
				int numArgs = methArgs.size();
				Assertions.Assert(numArgs == invkArgs.size());
				for (int i = 0; i < numArgs; i++) {
					IntPair zv = methArgs.get(i);
					int zIdx = zv.idx0;
					int vIdx = zv.idx1;
					IntPair zu = invkArgs.get(i);
					Assertions.Assert(zu.idx0 == zIdx);
					int uIdx = zu.idx1;
					if (DEBUG) System.out.println("ADDING to copy: " + retQidx + " " + vIdx + " " + uIdx);
					copySet.add(new IntTrio(retQidx, vIdx, uIdx));
				}
			}
		}

		int prevQidx = retQidx;
		while (true) {
			String line = currThread.get(currLineIdx++);
			char c = line.charAt(0);
			if (c == 'E') {
				int currQidx = frame(line, null, -1, prevQidx);
				if (prevQidx != -1) {
					Assertions.Assert(currQidx != -1);
					if (prevQidx != currQidx) {
						if (DEBUG) System.out.println("ADDING to succ1: " + prevQidx + " " + currQidx);
						succSet.add(new IntPair(prevQidx, currQidx));
					}
				}
				prevQidx = currQidx;
			} else if (c == 'X') {
				String mStr2 = line.substring(2);
				Assertions.Assert(mStr2.equals(mStr));
				currThreadFullOut.println(line);
				currThreadAbbrOut.println(line);
				if (DEBUG) System.out.println("LEAVE3 frame: " + methEntryLine);
				return prevQidx;
			} else {
				if (DEBUG) System.out.println("LINE: " + line);
				int bci = Integer.parseInt(line);
				Object o = methToCode[mIdx].get(bci);
				currThreadFullOut.println(line);
				if (o == null) {
					if (DEBUG) System.out.println("\tIgnoring");
					continue;
				}
				currThreadAbbrOut.println(line);
				List<Quad> instList;
				if (o instanceof Quad) {
					instList = new ArrayList<Quad>(1);
					instList.add((Quad) o);
				} else
					instList = (List) o;
				for (Quad q : instList) {
					if (DEBUG) System.out.println("\tQuad: " + q);
					currThreadFullOut.println("\t" + q);
					currThreadAbbrOut.println("\t" + q);
					Operator op = q.getOperator();
					int currQidx = domQ.set(new IntTrio(q.getID(), mIdx, cIdx));
					if (currQidx == domQ.size() - 1) {
						int p = domP.get(q);
						PQset.add(new IntPair(p, currQidx));
					}
					if (prevQidx != -1) {
						if (DEBUG) System.out.println("ADDING to succ2: " + prevQidx + " " + currQidx);
						succSet.add(new IntPair(prevQidx, currQidx));
					}
					else if (mName.equals("run") && mDesc.equals("()V")) {
						System.out.println("WARNING: Treating following quad of method " + m + " as head:\n\t" + q);
						IntPair thisArg = methArgs.get(0);
						Assertions.Assert(thisArg.idx0 == 0);
						if (DEBUG) System.out.println("ADDING to start: " + currQidx + " " + thisArg.idx1);
						startSet.add(new IntPair(currQidx, thisArg.idx1));
					}
					prevQidx = currQidx;
					//////////////////////////////////////// INVOKE
					if (op instanceof Invoke) {
						List<IntPair> invkArgs2 = null;
        			    ParamListOperand lo = Invoke.getParamList(q);
           				int numArgs = lo.length();
			            for (int i = 0; i < numArgs; i++) {
        			        RegisterOperand vo = lo.get(i);
			                Register v = vo.getRegister();
            			    if (v.getType().isReferenceType()) {
		        	            int vIdx = domV.get(v);
								if (invkArgs2 == null)
									invkArgs2 = new ArrayList<IntPair>();
        		    	        invkArgs2.add(new IntPair(i, vIdx));
							}
						}
						int invkRet2 = -1;
						RegisterOperand vo = Invoke.getDest(q);
						if (vo != null) {
							Register v = vo.getRegister();
							if (v.getType().isReferenceType()) {
								invkRet2 = domV.get(v);
							}
						}
						jq_Method m2 = Invoke.getMethod(q).getMethod();
						String m2Name = m2.getName().toString();
						String m2Desc = m2.getDesc().toString();
						String m2Sign = m2Name + m2Desc;
						if (m2Sign.equals("start()V") &&
								m2.getDeclaringClass().getName().equals("java.lang.Thread")) {
							IntPair thisArg = invkArgs2.get(0);
							Assertions.Assert(thisArg.idx0 == 0);
							if (DEBUG) System.out.println("ADDING to spawn: " + currQidx + " " + thisArg.idx1);
							spawnSet.add(new IntPair(currQidx, thisArg.idx1));
						}
						do {
							String line2 = currThread.get(currLineIdx);
							if (line2.charAt(0) != 'E') {
								// happens with ibm jvm if tgt method is a native method
								break;
							}
							currLineIdx++;
							String m3Sign = line2.substring(line2.indexOf(';') + 1);
							if (m2Sign.equals(m3Sign)) {
								prevQidx = frame(line2, invkArgs2, invkRet2, prevQidx);
								break;
							} else
								prevQidx = frame(line2, null, -1, prevQidx);
						} while (true);
						continue;
					}
					//////////////////////////////////////// RETURN
					if (op instanceof Return) {
						if (invkRet != -1) {
							Operand rx = Return.getSrc(q);
							if (rx instanceof RegisterOperand) {
								RegisterOperand ro = (RegisterOperand) rx;
								Register v = ro.getRegister();
								int methRet = domV.get(v);
								if (DEBUG) System.out.println("ADDING to copy: " + currQidx + " " + invkRet + " " + methRet);
								copySet.add(new IntTrio(currQidx, invkRet, methRet));
							}
						}
						continue;
					}
					if (op instanceof Move) {
						RegisterOperand lo = Move.getDest(q);
						Register l = lo.getRegister();
						int lIdx = domV.get(l);
						Assertions.Assert(lIdx != -1);
						Operand rx = Move.getSrc(q);
						if (rx instanceof RegisterOperand) {
							RegisterOperand ro = (RegisterOperand) rx;
							Register r = ro.getRegister();
							int rIdx = domV.get(r);
							Assertions.Assert(rIdx != -1);
							if (DEBUG) System.out.println("ADDING to copy: " + currQidx + " " + lIdx + " " + rIdx);
							copySet.add(new IntTrio(currQidx, lIdx, rIdx));
						} else {
							if (DEBUG) System.out.println("ADDING to asgn: " + currQidx + " " + lIdx);
							asgnSet.add(new IntPair(currQidx, lIdx));
						}
						continue;
					}
					if (op instanceof Getfield) {
						jq_Field f = Getfield.getField(q).getField();
						RegisterOperand bo = (RegisterOperand) Getfield.getBase(q);
						RegisterOperand lo = Getfield.getDest(q);
						Register b = bo.getRegister();
						Register l = lo.getRegister();
						int lIdx = domV.get(l);
						int bIdx = domV.get(b);
						int fIdx = domF.get(f);
						if (DEBUG) System.out.println("ADDING to getinst: " + currQidx + " " + lIdx + " " + bIdx + " " + fIdx);
						getinstSet.add(new IntQuad(currQidx, lIdx, bIdx, fIdx));
						continue;
					}
					if (op instanceof ALoad) {
						RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
						RegisterOperand lo = ALoad.getDest(q);
						Register b = bo.getRegister();
						Register l = lo.getRegister();
						int bIdx = domV.get(b);
						int lIdx = domV.get(l);
						int fIdx = 0;
						if (DEBUG) System.out.println("ADDING to getinst: " + currQidx + " " + lIdx + " " + bIdx + " " + fIdx);
						getinstSet.add(new IntQuad(currQidx, lIdx, bIdx, fIdx));
						continue;
					}
					if (op instanceof Putfield) {
						jq_Field f = Putfield.getField(q).getField();
						RegisterOperand bo = (RegisterOperand) Putfield.getBase(q);
						RegisterOperand ro = (RegisterOperand) Putfield.getSrc(q);
						Register b = bo.getRegister();
						Register r = ro.getRegister();
							int bIdx = domV.get(b);
						int rIdx = domV.get(r);
						int fIdx = domF.get(f);
						if (DEBUG) System.out.println("ADDING to putinst: " + currQidx + " " + bIdx + " " + fIdx + " " + rIdx);
						putinstSet.add(new IntQuad(currQidx, bIdx, fIdx, rIdx));
						continue;
					}
					if (op instanceof AStore) {
						RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
						RegisterOperand ro = (RegisterOperand) AStore.getValue(q);
						Register b = bo.getRegister();
						Register r = ro.getRegister();
						int bIdx = domV.get(b);
						int rIdx = domV.get(r);
						int fIdx = 0;
						if (DEBUG) System.out.println("ADDING to putinst: " + currQidx + " " + bIdx + " " + fIdx + " " + rIdx);
						putinstSet.add(new IntQuad(currQidx, bIdx, fIdx, rIdx));
						continue;
					} 
					if (op instanceof Getstatic) {
						RegisterOperand lo = Getstatic.getDest(q);
						Register l = lo.getRegister();
						int lIdx = domV.get(l);
						if (DEBUG) System.out.println("ADDING to getstat: " + currQidx + " " + lIdx);
						getstatSet.add(new IntPair(currQidx, lIdx));
						continue;
					}
					if (op instanceof Putstatic) {
						RegisterOperand ro = (RegisterOperand) Putstatic.getSrc(q);
						Register r = ro.getRegister();
						int rIdx = domV.get(r);
						if (DEBUG) System.out.println("ADDING to putstat: " + currQidx + " " + rIdx);
						putstatSet.add(new IntPair(currQidx, rIdx));
						continue;
					}
					if (op instanceof New || op instanceof NewArray) {
						RegisterOperand vo = (op instanceof New) ?
							New.getDest(q) : NewArray.getDest(q);
						Register v = vo.getRegister();
						int vIdx = domV.get(v);
						int hIdx = domH.get(q);
						if (DEBUG) System.out.println("ADDING to alloc " + currQidx + " " + vIdx + " " + hIdx);
						allocSet.add(new IntTrio(currQidx, vIdx, hIdx));
						continue;
					}
					if (op instanceof Phi) {
						throw new RuntimeException("TODO");
					}
					throw new RuntimeException("Invalid quad: " + q);
				}
			}
		}
	}

	private List<IntPair> processMethArgs(jq_Method m) {
		List<IntPair> args = null;
		ControlFlowGraph cfg = Program.getCFG(m);
		Assertions.Assert(cfg != null);
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = m.getParamTypes().length;
		for (int zIdx = 0; zIdx < numArgs; zIdx++) {
			Register v = rf.get(zIdx);
			if (v.getType().isReferenceType()) {
				int vIdx = domV.get(v);
				if (args == null)
					args = new ArrayList<IntPair>();
				args.add(new IntPair(zIdx, vIdx));
			}
		}
		return args;
	}

	private TIntObjectHashMap processMethCode(jq_Method m) {
		if (DEBUG) System.out.println("PROCESSING: " + m);
		ControlFlowGraph cfg = Program.getCFG(m);
		if (cfg == null)
			return null;
		if (DEBUG) System.out.println(cfg.fullDump());
		Map<Quad, Integer> bcMap = Program.getBCMap(m);
		if (bcMap == null)
			return null;
		TIntObjectHashMap code = new TIntObjectHashMap();
		for (Map.Entry<Quad, Integer> e : bcMap.entrySet()) {
			Quad q = e.getKey();
			int bci = e.getValue().intValue();
			if (isRelevant(q)) {
				if (DEBUG) System.out.println("\tRELEVANT: " + q);
				Object o = code.get(bci);
				if (o == null)
					code.put(bci, q);
				else {
					List<Quad> l;
					if (o instanceof Quad) {
						l = new ArrayList<Quad>(5);
						l.add((Quad) o);
						code.put(bci, l);
					} else
						l = (List) o;
					if (q.getOperator() instanceof Move)
						l.add(0, q);
					else
						l.add(q);
					System.out.println("CONFLICT (" + bci + "):");
					for (Quad p : l)
						System.out.println("\t" + p);
				}
			} else {
				if (DEBUG) System.out.println("\tIGNORING: " + q);
			}
		}
		return code;
	}

	private boolean isRelevant(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Move) {
			Operand rx = Move.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				return ro.getType().isReferenceType();
			}
			Assertions.Assert(rx instanceof ConstOperand);
			Assertions.Assert(!(rx instanceof PConstOperand));
			return rx instanceof AConstOperand;
		}
		if (op instanceof Invoke)
			return true;
		if (op instanceof Getfield) {
			jq_Field f = Getfield.getField(q).getField();
			return f.getType().isReferenceType() &&
				Getfield.getBase(q) instanceof RegisterOperand;
		}
		if (op instanceof ALoad) {
			// todo: check if base is a var?
			return ((ALoad) op).getType().isReferenceType();
		}
		if (op instanceof Putfield) {
			jq_Field f = Putfield.getField(q).getField();
			return f.getType().isReferenceType() &&
				Putfield.getBase(q) instanceof RegisterOperand &&
				Putfield.getSrc(q) instanceof RegisterOperand;
		}
		if (op instanceof AStore) {
			return (((AStore) op).getType().isReferenceType()) &&
				AStore.getValue(q) instanceof RegisterOperand;
		}
		if (op instanceof Return) {
			Assertions.Assert(!(op instanceof RETURN_P));
			return op instanceof RETURN_A;
		}
		if (op instanceof Getstatic) {
        	jq_Field f = Getstatic.getField(q).getField();
        	return f.getType().isReferenceType();
		}
		if (op instanceof Putstatic) {
        	jq_Field f = Putstatic.getField(q).getField();
        	if (f.getType().isReferenceType()) {
				Operand rx = Putstatic.getSrc(q);
				return (rx instanceof RegisterOperand);
			}
			return false;
		}
		if (op instanceof New || op instanceof NewArray)
			return true;
		if (op instanceof Phi) {
			throw new RuntimeException();
/*
			RegisterOperand lo = Phi.getDest(q);
			jq_Type t = lo.getType();
			return t == null || t.isReferenceType();
*/
		}
		return false;
	}

	private void createRels() throws IOException {
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
	}
	
	public void runAnalysis() {
		// do nothing
	}
}


