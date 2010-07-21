/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project.analyses;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.util.StringUtils;
import chord.doms.DomB;
import chord.doms.DomM;
import chord.instr.EventKind;
import chord.instr.InstrScheme;
import chord.instr.Instrumentor;
import chord.instr.OfflineTransformer;
import chord.instr.TracePrinter;
import chord.instr.TraceTransformer;
import chord.instr.InstrScheme.EventFormat;
import chord.program.CFGLoopFinder;
import chord.project.Messages;
import chord.project.Project;
import chord.project.ChordProperties;
import chord.project.OutDirUtils;
import chord.runtime.BufferedRuntime;
import chord.util.ByteBufferedFile;
import chord.util.ChordRuntimeException;
import chord.util.Executor;
import chord.util.FileUtils;
import chord.util.ReadException;

/**
 * Generic implementation of a dynamic program analysis
 * (a specialized kind of Java task).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author omertripp (omertrip@post.tau.ac.il)
 */
public class DynamicAnalysis extends JavaAnalysis {
	private static final String STARTING_RUN = "INFO: Dynamic analysis: Starting Run ID %s.";
	private static final String FINISHED_RUN = "INFO: Dynamic analysis: Finished Run ID %s.";
	private static final String FINISHED_PROCESSING_TRACE = "INFO: Dynamic analysis: Finished processing trace with %d events.";
	private static final String EVENT_NOT_HANDLED =
		"ERROR: Dynamic analysis '%s' must either override method '%s' or omit the corresponding event from its instrumentation scheme.";
	private static final String NO_INSTR_SCHEME = "ERROR: Dynamic analysis %s must override method 'InstrScheme getInstrScheme()'.";

	public final static boolean DEBUG = false;
	/**
	 * The instrumentation scheme for this dynamic analysis.
	 */
	protected InstrScheme scheme;
	
	/*
	 * data structures for loop entry/iter/leave events
	 */

	private static abstract class Record {
		// we must not implement state-dependent versions of
		// hashCode and equals
	}
	private static class LoopRecord extends Record {
		public final int b;
		public LoopRecord(int b) {
			this.b = b;
		}
	}
	private static class MethRecord extends Record {
		public final int m;
		public MethRecord(int m) {
			this.m = m;
		}
	}
	private DomM domM;
	private DomB domB;
	private boolean isUserReqEnterMethodEvent;
	private boolean isUserReqLeaveMethodEvent;
	private boolean isUserReqBasicBlockEvent;
	private boolean hasEnterAndLeaveLoopEvent;
	private final TIntObjectHashMap<Stack<Record>> stacks =
		new TIntObjectHashMap<Stack<Record>>(1);
	private final TIntObjectHashMap<TIntHashSet> loopHead2body =
		new TIntObjectHashMap<TIntHashSet>(16);
	private TIntHashSet visited4loops = new TIntHashSet();

	public void initPass() {
		// signals beginning of parsing of a trace
		// do nothing by default; subclasses can override
	}
	public void donePass() {
		// signals end of parsing of a trace
		// do nothing by default; subclasses can override
	}
	public void initAllPasses() {
		// do nothing by default; subclasses can override
	}
	public void doneAllPasses() {
		// do nothing by default; subclasses can override
	}

	// subclasses MUST override unless this dynamic analysis
	// is performed using an instrumentation scheme (and traces)
	// stored on disk from a previous run of Chord
	public InstrScheme getInstrScheme() {
		Messages.fatal(NO_INSTR_SCHEME, getName());
		return null;
	}

	// provides name of regular (i.e. non-pipe) file to store entire trace
	private static String getNameOfFullFile(String runID, String fileName) {
		return fileName + "_" + runID + "_full.txt";
	}
	// provides name of POSIX pipe file to store streaming trace as it is
	// written/read by event generating/processing JVMs
	private static String getNameOfPipeFile(String runID, String fileName) {
		return fileName + "_" + runID + "_pipe.txt";
	}

	private static String getNameOfFullCrudeTraceFile(String runID) {
		return getNameOfFullFile(runID, ChordProperties.crudeTraceFileName);
	}
	private static String getNameOfFullFinalTraceFile(String runID) {
		return getNameOfFullFile(runID, ChordProperties.finalTraceFileName);
	}
	private static String getNameOfPipeCrudeTraceFile(String runID) {
		return getNameOfPipeFile(runID, ChordProperties.crudeTraceFileName);
	}
	private static String getNameOfPipeFinalTraceFile(String runID) {
		return getNameOfPipeFile(runID, ChordProperties.finalTraceFileName);
	}

	public void run() {
		final String[] runIDs =
			ChordProperties.runIDs.split(ChordProperties.LIST_SEPARATOR);
		final String instrSchemeFileName = ChordProperties.instrSchemeFileName;
		boolean doReuse = false;
		if (ChordProperties.reuseTrace) {
			// check if instrumentation scheme file exists and
			// all trace files from a previous run of Chord exist;
			// only then can those files be reused
			if (FileUtils.exists(instrSchemeFileName)) {
				boolean failed = false;
				for (String runID : runIDs) {
					String s = getNameOfFullFinalTraceFile(runID);
					if (!FileUtils.exists(s)) {
						failed = true;
						break;
					}
				}
				if (!failed)
					doReuse = true;
			}
		}
		if (doReuse)
			scheme = InstrScheme.load(instrSchemeFileName);
		else
			scheme = getInstrScheme();
		isUserReqEnterMethodEvent =
			scheme.getEvent(InstrScheme.ENTER_METHOD).present(); 
		isUserReqLeaveMethodEvent =
			scheme.getEvent(InstrScheme.LEAVE_METHOD).present(); 
		isUserReqBasicBlockEvent = scheme.hasBasicBlockEvent(); 
		hasEnterAndLeaveLoopEvent = scheme.hasEnterAndLeaveLoopEvent();
		if (scheme.hasEnterAndLeaveLoopEvent()) {
			// below events are mandatory for consistent handling of
			// loop enter and leave events
			if (doReuse) {
				// TODO: assert that scheme has below events
			} else {
				scheme.setEnterMethodEvent(true, true);
				scheme.setLeaveMethodEvent(true, true);
				scheme.setBasicBlockEvent();
			}
			init4loopConsistency();
		}
		if (doReuse) {
			initAllPasses();
			for (String runID : runIDs) {
				Messages.log(STARTING_RUN, runID);
				String s = getNameOfFullFinalTraceFile(runID);
				processTrace(s);
				Messages.log(FINISHED_RUN, runID);
			}
			doneAllPasses();
			return;
		}
		scheme.save(instrSchemeFileName);
		final Instrumentor instrumentor = new Instrumentor(null, scheme);
		final OfflineTransformer transformer = new OfflineTransformer(instrumentor);
		try {
			transformer.run();
		} catch (Throwable ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		final String mainClassName = ChordProperties.mainClassName;
		assert (mainClassName != null);
		final String classPathName = ChordProperties.classPathName;
		assert (classPathName != null);
		final String bootClassesDirName = ChordProperties.bootClassesDirName;
		final String userClassesDirName = ChordProperties.userClassesDirName;
		final String runtimeClassName = ChordProperties.runtimeClassName;
		List<String> cmdList = new ArrayList<String>();
		cmdList.add("java");
		cmdList.addAll(StringUtils.tokenize(ChordProperties.runtimeJvmargs));
		cmdList.add("-Xbootclasspath/p:" + ChordProperties.mainClassPathName +
			File.pathSeparator + bootClassesDirName);
		cmdList.add("-Xverify:none");
		cmdList.add("-cp");
		cmdList.add(userClassesDirName + File.pathSeparator + classPathName);
		String agentCmd = "-agentpath:" + ChordProperties.cInstrAgentFileName +
			"=instr_scheme_file_name=" + instrSchemeFileName +
			"=runtime_class_name=" + runtimeClassName.replace('.', '/');
		cmdList.add(agentCmd);
		boolean runInSameJVM = !runtimeClassName.equals(BufferedRuntime.class.getName());
		if (runInSameJVM) {
			cmdList.add(mainClassName);
			initAllPasses();
			for (String runID : runIDs) {
				Messages.log(STARTING_RUN, runID);
				String args = System.getProperty("chord.args." + runID, "");
				List<String> fullCmdList = new ArrayList<String>(cmdList);
				fullCmdList.addAll(StringUtils.tokenize(args));
				initPass();
				int timeout = ChordProperties.dynamicTimeoutMs;
				if (ChordProperties.dynamicContinueOnError)
					OutDirUtils.executeWithWarnOnError(fullCmdList, timeout);
				else
					OutDirUtils.executeWithFailOnError(fullCmdList);
				donePass();
				Messages.log(FINISHED_RUN, runID);
			}
			doneAllPasses();
			return;
		}
		boolean usePipe = ChordProperties.doTracePipe;
		boolean doTransform = scheme.needsTraceTransform();
		agentCmd += "=trace_block_size=" + ChordProperties.traceBlockSize + "=trace_file_name=";
		initAllPasses();
		for (String runID : runIDs) {
			final String crudeTraceFileName = usePipe ?
				getNameOfPipeCrudeTraceFile(runID) :
				getNameOfFullCrudeTraceFile(runID);
			final String finalTraceFileName = usePipe ?
				getNameOfPipeFinalTraceFile(runID) :
				getNameOfFullFinalTraceFile(runID);
			final String traceFileName = doTransform ?
				crudeTraceFileName : finalTraceFileName;
			FileUtils.deleteFile(crudeTraceFileName);
			FileUtils.deleteFile(finalTraceFileName);
			if (usePipe) {
				String[] cmdArray1 = new String[] { "mkfifo", crudeTraceFileName };
				OutDirUtils.executeWithFailOnError(cmdArray1);
				String[] cmdArray2 = new String[] { "mkfifo", finalTraceFileName };
				OutDirUtils.executeWithFailOnError(cmdArray2);
			}
			Runnable traceTransformer = !doTransform ? null : new Runnable() {
				public void run() {
					if (DEBUG) {
						System.out.println("ENTER TRACE_TRANSFORMER");
						(new TracePrinter(crudeTraceFileName, instrumentor)).run();
						System.out.println("LEAVE TRACE_TRANSFORMER");
					}
					(new TraceTransformer(crudeTraceFileName,
						 finalTraceFileName, scheme)).run();
				}
			};
			Runnable traceProcessor = new Runnable() {
				public void run() {
					if (DEBUG) {
						System.out.println("ENTER TRACE_PROCESSOR");
						(new TracePrinter(finalTraceFileName, instrumentor)).run();
						System.out.println("LEAVE TRACE_PROCESSOR");
					}
					processTrace(finalTraceFileName);
				}
			};
			boolean serial = usePipe ? false : true;
			Executor executor = new Executor(serial);
			String args = System.getProperty("chord.args." + runID, "");
			final List<String> fullCmdList = new ArrayList<String>(cmdList);
			fullCmdList.set(cmdList.size() - 1, agentCmd + traceFileName);
			fullCmdList.add(mainClassName);
			fullCmdList.addAll(StringUtils.tokenize(args));
			Runnable instrProgram = new Runnable() {
				public void run() {
					OutDirUtils.executeWithFailOnError(fullCmdList);
				}
			};
			Messages.log(STARTING_RUN, runID);
			executor.execute(instrProgram);
			if (doTransform)
				executor.execute(traceTransformer);
			executor.execute(traceProcessor);
			try {
				executor.waitForCompletion();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				System.exit(1);
			}
			Messages.log(FINISHED_RUN, runID);
		}
		doneAllPasses();
	}

	private void init4loopConsistency() {
		domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);
		domB = (DomB) Project.getTrgt("B");
		Project.runTask(domB);
	}
	
	private void onLoopStart(int b, int t) {
		Stack<Record> stack = stacks.get(t);
		assert (stack != null);
		stack.add(new LoopRecord(b));
		processEnterLoop(b, t);
	}
	
	private void processBasicBlock4loopConsistency(int b, int t) {
		Stack<Record> stack = stacks.get(t);
		assert (stack != null);
		// Remove dead loop records from the stack.
		boolean hasRemoved;
		do {
			hasRemoved = false;
			Record r = stack.peek();
			if (r instanceof LoopRecord) {
				LoopRecord lr = (LoopRecord) r;
				TIntHashSet loopBody = loopHead2body.get(lr.b);
				assert (loopBody != null);
				if (!loopBody.contains(b)) {
					stack.pop();
					processLeaveLoop(lr.b, t);
						hasRemoved = true;
				}
			}
		} while (hasRemoved);
		boolean isLoopHead = loopHead2body.containsKey(b);
		if (isLoopHead) {
			Record r = stack.peek();
			if (r instanceof MethRecord) {
				onLoopStart(b, t);
			} else {
				assert (r instanceof LoopRecord);
				LoopRecord lr = (LoopRecord) r;
				if (lr.b == b) {
					processLoopIteration(lr.b, t);
				} else {
					onLoopStart(b, t);
				}
			}
		}
	}
	
	private void processLeaveMethod4loopConsistency(int m, int t) {
		Stack<Record> stack = stacks.get(t);
		assert (stack != null);
		if (!stack.isEmpty()) {
			while (stack.peek() instanceof LoopRecord) {
				LoopRecord top = (LoopRecord) stack.pop();
				processLeaveLoop(top.b, t);
			}
			
			// The present method should be at the stop of the stack.
			Record top = stack.peek();
			assert (top instanceof MethRecord);
			MethRecord mr = (MethRecord) top;
			if (mr.m == m) {
				stack.pop();
			}
		}
	}

	private void processEnterMethod4loopConsistency(int m, int t) {
		Stack<Record> stack = stacks.get(t);
		if (stack == null) {
			stack = new Stack<Record>();
			stacks.put(t, stack);
		}
		stack.add(new MethRecord(m));
		if (!visited4loops.contains(m)) {
			visited4loops.add(m);
			jq_Method mthd = domM.get(m);
			// Perform a slightly eager computation to map each loop header
			// to its body (in terms of <code>DomB</code>).
			ControlFlowGraph cfg = mthd.getCFG();
			CFGLoopFinder finder = new CFGLoopFinder();
			finder.visit(cfg);
			for (BasicBlock head : finder.getLoopHeads()) {
				TIntHashSet S = new TIntHashSet();
				int bh = domB.indexOf(head);
				assert (bh != -1);
				loopHead2body.put(bh, S);
				Set<BasicBlock> loopBody = finder.getLoopBody(head);
				for (BasicBlock bb : loopBody) {
					int b2 = domB.indexOf(bb);
					assert (b2 != -1);
					S.add(b2);
				}
			}
		}
	}
	
	private void processTrace(String fileName) {
		try {
		initPass();
		ByteBufferedFile buffer = new ByteBufferedFile(
			ChordProperties.traceBlockSize, fileName, true);
		long count = 0;
		while (!buffer.isDone()) {
			byte opcode = buffer.getByte();
			count++;
			switch (opcode) {
			case EventKind.ENTER_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_METHOD);
				int m = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				if (hasEnterAndLeaveLoopEvent) {
					processEnterMethod4loopConsistency(m, t);
				}
				if (isUserReqEnterMethodEvent) {
					processEnterMethod(m, t);
				}
				break;
			}
			case EventKind.LEAVE_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.LEAVE_METHOD);
				int m = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				if (hasEnterAndLeaveLoopEvent) {
					processLeaveMethod4loopConsistency(m, t);
				}
				if (isUserReqLeaveMethodEvent) {
					processLeaveMethod(m, t);
				}
				break;
			}
			case EventKind.NEW:
			case EventKind.NEW_ARRAY:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				int h = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processNewOrNewArray(h, t, o);
				break;
			}
			case EventKind.GETSTATIC_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				processGetstaticPrimitive(e, t, b, f);
				break;
			}
			case EventKind.GETSTATIC_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processGetstaticReference(e, t, b, f, o);
				break;
			}
			case EventKind.PUTSTATIC_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				processPutstaticPrimitive(e, t, b, f);
				break;
			}
			case EventKind.PUTSTATIC_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processPutstaticReference(e, t, b, f, o);
				break;
			}
			case EventKind.GETFIELD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				processGetfieldPrimitive(e, t, b, f);
				break;
			}
			case EventKind.GETFIELD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processGetfieldReference(e, t, b, f, o);
				break;
			}
			case EventKind.PUTFIELD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				processPutfieldPrimitive(e, t, b, f);
				break;
			}
			case EventKind.PUTFIELD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processPutfieldReference(e, t, b, f, o);
				break;
			}
			case EventKind.ALOAD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				processAloadPrimitive(e, t, b, i);
				break;
			}
			case EventKind.ALOAD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processAloadReference(e, t, b, i, o);
				break;
			}
			case EventKind.ASTORE_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				processAstorePrimitive(e, t, b, i);
				break;
			}
			case EventKind.ASTORE_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processAstoreReference(e, t, b, i, o);
				break;
			}
			case EventKind.THREAD_START:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processThreadStart(p, t, o);
				break;
			}
			case EventKind.THREAD_JOIN:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processThreadJoin(p, t, o);
				break;
			}
			case EventKind.ACQUIRE_LOCK:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				processAcquireLock(p, t, l);
				break;
			}
			case EventKind.RELEASE_LOCK:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				processReleaseLock(p, t, l);
				break;
			}
			case EventKind.WAIT:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				processWait(p, t, l);
				break;
			}
			case EventKind.NOTIFY:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				processNotify(p, t, l);
				break;
			}
			case EventKind.METHOD_CALL_BEF:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				int i = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processMethodCallBef(i, t, o);
				break;
			}
			case EventKind.METHOD_CALL_AFT:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				int i = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processMethodCallAft(i, t, o);
				break;
			}
			case EventKind.RETURN_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_PRIMITIVE);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				processReturnPrimitive(p, t);
				break;
			}
			case EventKind.RETURN_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_REFERENCE);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processReturnReference(p, t, o);
				break;
			}
			case EventKind.EXPLICIT_THROW:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.EXPLICIT_THROW);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processExplicitThrow(p, t, o);
				break;
			}
			case EventKind.IMPLICIT_THROW:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.IMPLICIT_THROW);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				processImplicitThrow(p, t, o);
				break;
			}
			case EventKind.QUAD:
			{
				int q = buffer.getInt();
				int t = buffer.getInt();
				processQuad(q, t);
				break;
			}
			case EventKind.BASIC_BLOCK:
			{
				int b = buffer.getInt();
				int t = buffer.getInt();
				if (hasEnterAndLeaveLoopEvent) {
					processBasicBlock4loopConsistency(b, t);
				}
				if (isUserReqBasicBlockEvent) {
					processBasicBlock(b, t);
				}
				break;
			}
			case EventKind.FINALIZE:
			{
				int o = buffer.getInt();
				processFinalize(o);
				break;
			}
			default:
				throw new ChordRuntimeException("Unknown opcode: " + opcode);
			}
		}
		donePass();
		Messages.log(FINISHED_PROCESSING_TRACE, count);
	} catch (IOException ex) {
		ex.printStackTrace();
		System.exit(1);
	} catch (ReadException ex) {
		ex.printStackTrace();
		System.exit(1);
	}
	}
	
	public void processLoopIteration(int w, int t) {
		error("void processLoopIteration(int w, int t)");
	}
	
	public void processEnterMethod(int m, int t) {
		error("void processEnterMethod(int m, int t)");
	}
	public void processLeaveMethod(int m, int t) { 
		error("void processLeaveMethod(int m, int t)");
	}
	public void processEnterLoop(int w, int t) { 
		error("void processEnterLoop(int w, int t)");
	}
	public void processLeaveLoop(int w, int t) { 
		error("void processLeaveLoop(int w, int t)");
	}
	public void processNewOrNewArray(int h, int t, int o) { 
		error("void processNewOrNewArray(int h, int t, int o)");
	}
	public void processGetstaticPrimitive(int e, int t, int b, int f) { 
		error("void processGetstaticPrimitive(int e, int t, int b, int f)");
	}
	public void processGetstaticReference(int e, int t, int b, int f, int o) { 
		error("void processGetstaticReference(int e, int t, int b, int f, int o)");
	}
	public void processPutstaticPrimitive(int e, int t, int b, int f) { 
		error("void processPutstaticPrimitive(int e, int t, int b, int f)");
	}
	public void processPutstaticReference(int e, int t, int b, int f, int o) { 
		error("void processPutstaticReference(int e, int t, int b, int f, int o");
	}
	public void processGetfieldPrimitive(int e, int t, int b, int f) { 
		error("void processGetfieldPrimitive(int e, int t, int b, int f)");
	}
	public void processGetfieldReference(int e, int t, int b, int f, int o) { 
		error("void processGetfieldReference(int e, int t, int b, int f, int o)");
	}
	public void processPutfieldPrimitive(int e, int t, int b, int f) { 
		error("void processPutfieldPrimitive(int e, int t, int b, int f)");
	}
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		error("void processPutfieldReference(int e, int t, int b, int f, int o)");
	}
	public void processAloadPrimitive(int e, int t, int b, int i) { 
		error("void processAloadPrimitive(int e, int t, int b, int i)");
	}
	public void processAloadReference(int e, int t, int b, int i, int o) { 
		error("void processAloadReference(int e, int t, int b, int i, int o)");
	}
	public void processAstorePrimitive(int e, int t, int b, int i) { 
		error("void processAstorePrimitive(int e, int t, int b, int i)");
	}
	public void processAstoreReference(int e, int t, int b, int i, int o) { 
		error("void processAstoreReference(int e, int t, int b, int i, int o)");
	}
	public void processThreadStart(int i, int t, int o) { 
		error("void processThreadStart(int i, int t, int o)");
	}
	public void processThreadJoin(int i, int t, int o) { 
		error("void processThreadJoin(int i, int t, int o) ");
	}
	public void processAcquireLock(int l, int t, int o) { 
		error("void processAcquireLock(int l, int t, int o)");
	}
	public void processReleaseLock(int r, int t, int o) { 
		error("void processReleaseLock(int r, int t, int o)");
	}
	public void processWait(int i, int t, int o) { 
		error("void processWait(int i, int t, int o)");
	}
	public void processNotify(int i, int t, int o) { 
		error("void processNotify(int i, int t, int o)");
	}
	public void processMethodCallBef(int i, int t, int o) { 
		error("void processMethodCallBef(int i, int t, int o)");
	}
	public void processMethodCallAft(int i, int t, int o) { 
		error("void processMethodCallAft(int i, int t, int o)");
	}
	public void processReturnPrimitive(int p, int t) { 
		error("void processReturnPrimitive(int p, int t) ");
	}
	public void processReturnReference(int p, int t, int o) { 
		error("void processReturnReference(int p, int t, int o)");
	}
	public void processExplicitThrow(int p, int t, int o) { 
		error("void processExplicitThrow(int p, int t, int o)");
	}
	public void processImplicitThrow(int p, int t, int o) { 
		error("void processImplicitThrow(int p, int t, int o)");
	}
	public void processQuad(int p, int t) { 
		error("void processQuad(int p, int t)");
	}
	public void processBasicBlock(int b, int t) { 
		error("void processBasicBlock(int b, int t)");
	}
	public void processFinalize(int o) {
		error("void processFinalize(int o)");
	}
	private void error(String mSign) {
		Messages.fatal(EVENT_NOT_HANDLED, getName(), mSign);
	}
}
