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
import chord.util.IndexMap;
import chord.util.IndexHashMap;
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
	private IndexMap<String> sHmap = new IndexHashMap<String>();
	private IndexMap<String> dHmap = new IndexHashMap<String>();
	private IndexMap<String> sEmap = new IndexHashMap<String>();
	private IndexMap<String> dEmap = new IndexHashMap<String>();
	private IndexMap<String> sFmap = new IndexHashMap<String>();
	private IndexMap<String> dFmap = new IndexHashMap<String>();
	private IndexMap<String> sMmap = new IndexHashMap<String>();
	private IndexMap<String> dMmap = new IndexHashMap<String>();
	private IndexMap<String> sPmap = new IndexHashMap<String>();
	private IndexMap<String> dPmap = new IndexHashMap<String>();
	private IndexMap<String> sBmap = new IndexHashMap<String>();
	private IndexMap<String> dBmap = new IndexHashMap<String>();
	protected IndexMap<String> Hmap;
	protected IndexMap<String> Emap;
	protected IndexMap<String> Fmap;
	protected IndexMap<String> Mmap;
	protected IndexMap<String> Pmap;
	protected IndexMap<String> Bmap;
	protected InstrScheme scheme;
	protected boolean convert;

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
		Instrumentor instrumentor = new Instrumentor();
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
		final String bootClassesFileName = Properties.bootClassesFileName;
		final String classesFileName = Properties.classesFileName;
		convert = System.getProperty(
			"chord.convert", "false").equals("true");
		boolean doTracePipe = System.getProperty(
			"chord.trace.pipe", "false").equals("true");
		if (scheme.needsHmap())
			processDom("H", sHmap, dHmap);
		if (scheme.needsEmap())
			processDom("E", sEmap, dEmap);
		if (scheme.needsPmap())
			processDom("P", sPmap, dPmap);
		if (scheme.needsFmap())
			processDom("F", sFmap, dFmap);
		if (scheme.needsMmap())
			processDom("M", sMmap, dMmap);
		if (scheme.needsBmap())
			processDom("B", sBmap, dBmap);
		if (convert) {
			Hmap = sHmap;
			Emap = sEmap;
			Pmap = sPmap;
			Fmap = sFmap;
			Mmap = sMmap;
			Bmap = sBmap;
		} else {
			Hmap = dHmap;
			Emap = dEmap;
			Pmap = dPmap;
			Fmap = dFmap;
			Mmap = dMmap;
			Bmap = sBmap;
		}
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
	
	private void processDom(String domName, IndexMap<String> sMap,
			IndexMap<String> dMap) {
		if (convert) {
			ProgramDom dom = (ProgramDom) Project.getTrgt(domName);
			Project.runTask(dom);
			dom.saveToIdFile();
			for (int i = 0; i < dom.size(); i++) {
				String s = dom.toUniqueIdString(dom.get(i));
				if (sMap.contains(s))
					throw new RuntimeException(domName +
						"smap already contains: " + s);
				sMap.getOrAdd(s);
			}
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
				new File(Properties.outDirName, domName + ".dynamic.txt")));
			String s;
			while ((s = reader.readLine()) != null) {
				assert (!dMap.contains(s));
				dMap.getOrAdd(s);
			}
			reader.close();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	
	public int getHidx(int h) {
		String s = dHmap.get(h);
		int hIdx = sHmap.indexOf(s);
		if (hIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sHmap");
		}
		return hIdx;
	}
	public int getEidx(int e) {
		String s = dEmap.get(e);
		int eIdx = sEmap.indexOf(s);
		if (eIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sEmap");
		}
		return eIdx;
	}
	public int getPidx(int p) {
		String s = dPmap.get(p);
		int pIdx = sPmap.indexOf(s);
		if (pIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sPmap");
		}
		return pIdx;
	}
	public int getFidx(int f) {
		String s = dFmap.get(f);
		int fIdx = sFmap.indexOf(s);
		if (fIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sFmap");
		}
		return fIdx;
	}
	public int getMidx(int m) {
		String s = dMmap.get(m);
		int mIdx = sMmap.indexOf(s);
		if (mIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sMmap");
		}
		return mIdx;
	}
	public int getBidx(int b) {
		String s = dBmap.get(b);
		int bIdx = sBmap.indexOf(s);
		if (bIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sBmap");
		}
		return bIdx;
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
				case EventKind.ENTER_BB:
				{
					int b = buffer.getInt();
					if (convert)
						b = getBidx(b);
					int t = buffer.getInt();
					processEnterBasicBlock(b, t);
					break;
				}
				case EventKind.ENTER_METHOD:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					int m;
					if (lef.hasMid()) {
						m = buffer.getInt();
						if (convert)
							m = getMidx(m);
					} else {
						m = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					processEnterMethod(m, t);
					break;
				}
				case EventKind.LEAVE_METHOD:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					int m;
					if (lef.hasMid()) {
						m = buffer.getInt();
						if (convert)
							m = getMidx(m);
					} else {
						m = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					processLeaveMethod(m, t);
					break;
				}
				case EventKind.NEW:
				case EventKind.NEW_ARRAY:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
					int h;
					if (lef.hasPid()) {
						h = buffer.getInt();
						if (convert)
							h = getHidx(h);
					} else {
						h = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processNewOrNewArray(h, t, o);
					break;
				}
				case EventKind.GETSTATIC_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					processGetstaticPrimitive(e, t, f);
					break;
				}
				case EventKind.GETSTATIC_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processGetstaticReference(e, t, f, o);
					break;
				}
				case EventKind.PUTSTATIC_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					processPutstaticPrimitive(e, t, f);
					break;
				}
				case EventKind.PUTSTATIC_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processPutstaticReference(e, t, f, o);
					break;
				}
				case EventKind.GETFIELD_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					processGetfieldPrimitive(e, t, b, f);
					break;
				}
				case EventKind.GETFIELD_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processGetfieldReference(e, t, b, f, o);
					break;
				}
				case EventKind.PUTFIELD_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					processPutfieldPrimitive(e, t, b, f);
					break;
				}
				case EventKind.PUTFIELD_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int f;
					if (lef.hasFid()) {
						f = buffer.getInt();
						if (convert)
							f = getFidx(f);
					} else {
						f = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processPutfieldReference(e, t, b, f, o);
					break;
				}
				case EventKind.ALOAD_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int i;
					if (lef.hasIid()) {
						i = buffer.getInt();
					} else {
						i = -1;
					}
					processAloadPrimitive(e, t, b, i);
					break;
				}
				case EventKind.ALOAD_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int i;
					if (lef.hasIid()) {
						i = buffer.getInt();
					} else {
						i = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processAloadReference(e, t, b, i, o);
					break;
				}
				case EventKind.ASTORE_PRIMITIVE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int i;
					if (lef.hasIid()) {
						i = buffer.getInt();
					} else {
						i = -1;
					}
					processAstorePrimitive(e, t, b, i);
					break;
				}
				case EventKind.ASTORE_REFERENCE:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
					int e;
					if (lef.hasPid()) {
						e = buffer.getInt();
						if (convert)
							e = getEidx(e);
					} else {
						e = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int b;
					if (lef.hasBid()) {
						b = buffer.getInt();
					} else {
						b = -1;
					}
					int i;
					if (lef.hasIid()) {
						i = buffer.getInt();
					} else {
						i = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processAstoreReference(e, t, b, i, o);
					break;
				}
				case EventKind.THREAD_START:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.THREAD_START);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
					} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processThreadStart(p, t, o);
					break;
				}
				case EventKind.THREAD_JOIN:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.THREAD_JOIN);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
					} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int o;
					if (lef.hasOid()) {
						o = buffer.getInt();
					} else {
						o = -1;
					}
					processThreadJoin(p, t, o);
					break;
				}
				case EventKind.ACQUIRE_LOCK:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
					} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int l;
					if (lef.hasLid()) {
						l = buffer.getInt();
					} else {
						l = -1;
					}
					processAcquireLock(p, t, l);
					break;
				}
				case EventKind.RELEASE_LOCK:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
						} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
						} else {
						t = -1;
					}
					int l;
					if (lef.hasLid()) {
							l = buffer.getInt();
					} else {
						l = -1;
					}
					processReleaseLock(p, t, l);
					break;
				}
				case EventKind.WAIT:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.WAIT);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
					} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int l;
					if (lef.hasLid()) {
						l = buffer.getInt();
					} else {
						l = -1;
					}
					processWait(p, t, l);
					break;
				}
				case EventKind.NOTIFY:
				{
					EventFormat lef = scheme.getEvent(InstrScheme.NOTIFY);
					int p;
					if (lef.hasPid()) {
						p = buffer.getInt();
						if (convert)
							p = getPidx(p);
					} else {
						p = -1;
					}
					int t;
					if (lef.hasTid()) {
						t = buffer.getInt();
					} else {
						t = -1;
					}
					int l;
					if (lef.hasLid()) {
						l = buffer.getInt();
					} else {
						l = -1;
					}
					processNotify(p, t, l);
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
	public void processEnterBasicBlock(int b, int t) { }
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
}
