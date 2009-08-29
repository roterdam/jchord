/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;

import chord.instr.TracePrinter;
import chord.instr.EventKind;
import chord.instr.InstrScheme;
import chord.instr.TraceTransformer;
import chord.instr.InstrScheme.EventFormat;
import chord.util.ByteBufferedFile;
import chord.util.ProcessExecutor;
import chord.util.ReadException;
import chord.util.Executor;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "dyn-java"
)
public class DynamicAnalysis extends JavaAnalysis {
	public final static boolean DEBUG = true;
	protected InstrScheme scheme;
	protected Instrumentor instrumentor;
	
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
	// subclass must override
	public InstrScheme getInstrScheme() {
		throw new ChordRuntimeException();
	}
	public void run() {
		scheme = getInstrScheme();
		final String instrSchemeFileName = Properties.instrSchemeFileName;
		scheme.save(instrSchemeFileName);
		instrumentor = new Instrumentor();
		instrumentor.visit(Program.v(), scheme);
		assert(scheme != null);
		boolean needsTraceTransform = scheme.needsTraceTransform();
		final String mainClassName = Properties.mainClassName;
		assert (mainClassName != null);
		final String classPathName = Properties.classPathName;
		assert (classPathName != null);
		final String bootClassesDirName = Properties.bootClassesDirName;
		final String classesDirName = Properties.classesDirName;
		final String crudeTraceFileName = Properties.crudeTraceFileName;
		final String finalTraceFileName = Properties.finalTraceFileName;
		boolean doTracePipe = System.getProperty(
			"chord.trace.pipe", "false").equals("true");
		ProcessExecutor.execute("rm " + crudeTraceFileName);
		ProcessExecutor.execute("rm " + finalTraceFileName);
		if (doTracePipe) {
			ProcessExecutor.execute("mkfifo " + crudeTraceFileName);
			ProcessExecutor.execute("mkfifo " + finalTraceFileName);
		}
		final int numMeths = getNum("numMeths.txt");
		final int numLoops = getNum("numLoops.txt");
		final String[] runIDs = Properties.runIDs.split(",");
		final String traceFileName = needsTraceTransform ?
			crudeTraceFileName : finalTraceFileName;
		final String instrProgramCmd = "java -ea -Xbootclasspath/p:" +
			bootClassesDirName + File.pathSeparator + Properties.mainClassPathName +
			" -Xverify:none" + " -verbose" + 
			" -cp " + classesDirName + File.pathSeparator + classPathName +
			" -agentpath:" + Properties.instrAgentFileName +
			"=trace_file_name=" + traceFileName +
			"=num_meths=" + numMeths +
			"=num_loops=" + numLoops +
			"=instr_scheme_file_name=" + instrSchemeFileName +
			"=instr_bound=" + Properties.instrBound +
			" " + mainClassName + " ";
		Runnable traceTransformer = new Runnable() {
			public void run() {
				(new TraceTransformer()).run(
					crudeTraceFileName, finalTraceFileName, scheme);
				if (DEBUG) {
					(new TracePrinter()).run(crudeTraceFileName, scheme);
					System.out.println("DONE");
				}
			}
		};
		Runnable traceProcessor = new Runnable() {
			public void run() {
				if (DEBUG) {
					(new TracePrinter()).run(finalTraceFileName, scheme);
					System.out.println("DONE");
				}
				processTrace(finalTraceFileName);
			}
		};
		boolean serial = doTracePipe ? false : true;
		Executor executor = new Executor(serial);
		initAllPasses();
		for (String runID : runIDs) {
			System.out.println("Processing Run ID: " + runID);
			final String args = System.getProperty("chord.args." + runID, "");
			Runnable instrProgram = new Runnable() {
				public void run() {
					ProcessExecutor.execute(instrProgramCmd + args);
				}
			};
			executor.execute(instrProgram);
			if (needsTraceTransform)
				executor.execute(traceTransformer);
			executor.execute(traceProcessor);
			try {
				executor.waitForCompletion();
			} catch (InterruptedException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		doneAllPasses();
	}

	private static int getNum(String fileName) {
		File file = new File(Properties.classesDirName, fileName);
		if (!file.exists())
			return 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String s = reader.readLine();
			assert (s != null);
			reader.close();
			return Integer.parseInt(s);
		} catch (IOException ex) { 
			throw new RuntimeException(ex);
		}
	}
	
	private void processTrace(String fileName) {
		try {
			initPass();
			ByteBufferedFile buffer = new ByteBufferedFile(1024, fileName, true);
			int count = 0;
			while (!buffer.isDone()) {
				byte opcode = buffer.getByte();
				count++;
				switch (opcode) {
				case EventKind.ENTER_METHOD:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					int m = ef.hasMid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					processEnterMethod(m, t);
					break;
				}
				case EventKind.LEAVE_METHOD:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					int m = ef.hasMid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					processLeaveMethod(m, t);
					break;
				}
				case EventKind.NEW:
				case EventKind.NEW_ARRAY:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
					int h = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processNewOrNewArray(h, t, o);
					break;
				}
				case EventKind.GETSTATIC_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					processGetstaticPrimitive(e, t, f);
					break;
				}
				case EventKind.GETSTATIC_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processGetstaticReference(e, t, f, o);
					break;
				}
				case EventKind.PUTSTATIC_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					processPutstaticPrimitive(e, t, f);
					break;
				}
				case EventKind.PUTSTATIC_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processPutstaticReference(e, t, f, o);
					break;
				}
				case EventKind.GETFIELD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					processGetfieldPrimitive(e, t, b, f);
					break;
				}
				case EventKind.GETFIELD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processGetfieldReference(e, t, b, f, o);
					break;
				}
				case EventKind.PUTFIELD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					processPutfieldPrimitive(e, t, b, f);
					break;
				}
				case EventKind.PUTFIELD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int f = ef.hasFid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processPutfieldReference(e, t, b, f, o);
					break;
				}
				case EventKind.ALOAD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int i = ef.hasIid() ? buffer.getInt() : -1;
					processAloadPrimitive(e, t, b, i);
					break;
				}
				case EventKind.ALOAD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int i = ef.hasIid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processAloadReference(e, t, b, i, o);
					break;
				}
				case EventKind.ASTORE_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int i = ef.hasIid() ? buffer.getInt() : -1;
					processAstorePrimitive(e, t, b, i);
					break;
				}
				case EventKind.ASTORE_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
					int e = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int b = ef.hasBid() ? buffer.getInt() : -1;
					int i = ef.hasIid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processAstoreReference(e, t, b, i, o);
					break;
				}
				case EventKind.THREAD_START:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processThreadStart(p, t, o);
					break;
				}
				case EventKind.THREAD_JOIN:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int o = ef.hasOid() ? buffer.getInt() : -1;
					processThreadJoin(p, t, o);
					break;
				}
				case EventKind.ACQUIRE_LOCK:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int l = ef.hasLid() ? buffer.getInt() : -1;
					processAcquireLock(p, t, l);
					break;
				}
				case EventKind.RELEASE_LOCK:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int l = ef.hasLid() ? buffer.getInt() : -1;
					processReleaseLock(p, t, l);
					break;
				}
				case EventKind.WAIT:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int l = ef.hasLid() ? buffer.getInt() : -1;
					processWait(p, t, l);
					break;
				}
				case EventKind.NOTIFY:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					int l = ef.hasLid() ? buffer.getInt() : -1;
					processNotify(p, t, l);
					break;
				}
				case EventKind.METHOD_CALL:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
					int i = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					processMethodCall(i, t);
					break;
				}
				case EventKind.MOVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.MOVE);
					int p = ef.hasPid() ? buffer.getInt() : -1;
					int t = ef.hasTid() ? buffer.getInt() : -1;
					processMove(p, t);
					break;
				}
				default:
					throw new RuntimeException("Unknown opcode: " + opcode);
				}
			}
			donePass();
			System.out.println("PROCESS TRACE: " + count);
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		} catch (ReadException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public void processEnterMethod(int m, int t) { }
	public void processLeaveMethod(int m, int t) { }
	public void processNewOrNewArray(int h, int t, int o) { }
	public void processGetstaticPrimitive(int e, int t, int f) { }
	public void processGetstaticReference(int e, int t, int f, int o) { }
	public void processPutstaticPrimitive(int e, int t, int f) { }
	public void processPutstaticReference(int e, int t, int f, int o) { }
	public void processGetfieldPrimitive(int e, int t, int b, int f) { }
	public void processGetfieldReference(int e, int t, int b, int f, int o) { }
	public void processPutfieldPrimitive(int e, int t, int b, int f) { }
	public void processPutfieldReference(int e, int t, int b, int f, int o) { }
	public void processAloadPrimitive(int e, int t, int b, int i) { }
	public void processAloadReference(int e, int t, int b, int i, int o) { }
	public void processAstorePrimitive(int e, int t, int b, int i) { }
	public void processAstoreReference(int e, int t, int b, int i, int o) { }
	public void processThreadStart(int p, int t, int o) { }
	public void processThreadJoin(int p, int t, int o) { }
	public void processAcquireLock(int p, int t, int l) { }
	public void processReleaseLock(int p, int t, int l) { }
	public void processWait(int p, int t, int l) { }
	public void processNotify(int p, int t, int l) { }
	public void processMethodCall(int i, int t) { }
	public void processMove(int p, int t) { }
}
