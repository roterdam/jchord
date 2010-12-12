/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
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
import java.util.Properties;
import java.util.Map;
import java.util.Collections;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.util.StringUtils;
import chord.doms.DomB;
import chord.doms.DomM;
import chord.instr.EventKind;
import chord.instr.CoreInstrumentor;
import chord.instr.OfflineTransformer;
import chord.instr.TracePrinter;
import chord.instr.TraceTransformer;
import chord.project.Messages;
import chord.project.Project;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.runtime.CoreEventHandler;
import chord.util.ByteBufferedFile;
import chord.util.ChordRuntimeException;
import chord.util.Executor;
import chord.util.FileUtils;
import chord.util.ProcessExecutor;
import chord.util.ReadException;
import chord.util.tuple.object.Pair;

/**
 * Generic implementation of a dynamic program analysis
 * (a specialized kind of Java task).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CoreDynamicAnalysis extends JavaAnalysis {
	///// Shorthands for error/warning messages in this class

	private static final String STARTING_RUN = "INFO: Dynamic analysis: Starting Run ID %s.";
	private static final String FINISHED_RUN = "INFO: Dynamic analysis: Finished Run ID %s.";
	private static final String FINISHED_PROCESSING_TRACE =
		"INFO: Dynamic analysis: Finished processing trace with %d events.";
	private static final String REUSE_ONLY_FULL_TRACES =
		"ERROR: Dynamic analysis: Can only reuse full traces.";

	public final static boolean DEBUG = false;
	
	// subclasses can override
	public Pair<Class, Map<String, String>> getInstrumentor() {
		return new Pair<Class, Map<String, String>>(CoreInstrumentor.class, Collections.EMPTY_MAP);
	}

	// subclasses can override
	public Pair<Class, Map<String, String>> getEventHandler() {
		return new Pair<Class, Map<String, String>>(CoreEventHandler.class, Collections.EMPTY_MAP);
	}

	// subclasses can override
	public List<Runnable> getTraceTransformers() {
		return Collections.EMPTY_LIST;
	}

	// subclasses can override
	public String getInstrKind() {
		return Config.instrKind;
	}

	// subclasses can override
	public String getTraceKind() {
		return Config.traceKind;
	}

	// subclasses may override
	public boolean haltOnErr() {
		return Config.dynamicHaltOnErr;
	}

	// subclasses may override
	public int getTimeout() {
		return Config.dynamicTimeout;
	}

	// subclasses can override
	public int getTraceBlockSize() {
		return Config.traceBlockSize;
	}

	// subclasses may override
	public boolean reuseTraces() {
		return Config.reuseTraces;
	}

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

	// provides name of regular (i.e. non-pipe) file to store entire trace
	// provides name of POSIX pipe file to store streaming trace as it is
	// written/read by event generating/processing JVMs

	protected String getTraceFileName(String base, int version, String runID) {
		return base + "_ver" + version + "_run" + runID + ".txt";
	}

	protected String getTraceFileName(String base, int version) {
		return base + "_ver" + version + ".txt";
	}

	// version == 0 means final trace file
	protected String getTraceFileName(int version) {
		return getTraceKind().equals("pipe") ?
			getTraceFileName(Config.traceFileName + "_pipe", version) :
			getTraceFileName(Config.traceFileName + "_full", version);
	}

	protected String getTraceFileName(int version, String runID) {
		return getTraceKind().equals("pipe") ?
			getTraceFileName(Config.traceFileName + "_pipe", version, runID) :
			getTraceFileName(Config.traceFileName + "_full", version, runID);
	}

	protected String[] runIDs = Config.runIDs.split(Config.LIST_SEPARATOR);

	public boolean canReuseTraces() {
		boolean reuse = false;
		if (reuseTraces()) {
			// check if all trace files from a previous run of
			// Chord exist; only then can those files be reused
			boolean failed = false;
			for (String runID : runIDs) {
				String s = getTraceFileName(0, runID);
				if (!FileUtils.exists(s)) {
					failed = true;
					break;
				}
			}
			if (!failed)
				reuse = true;
		}
		return reuse;
	}

	public void run() {
		String traceKind = getTraceKind();
		if (reuseTraces() && !traceKind.equals("full"))
			Messages.fatal(REUSE_ONLY_FULL_TRACES);
		if (canReuseTraces()) {
			initAllPasses();
			for (String runID : runIDs) {
				if (Config.verbose > 1) Messages.log(STARTING_RUN, runID);
				String s = getTraceFileName(0, runID);
				processTrace(s);
				if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID);
			}
			doneAllPasses();
			return;
		}
		boolean offline = getInstrKind().equals("offline");
		if (offline)
			doOfflineInstrumentation();
		if (traceKind.equals("none")) {
			List<String> basecmd = getBaseCmd(!offline, false, 0);
			initAllPasses();
			for (String runID : runIDs) {
				String args = System.getProperty("chord.args." + runID, "");
				List<String> fullcmd = new ArrayList<String>(basecmd);
				fullcmd.addAll(StringUtils.tokenize(args));
				if (Config.verbose > 1) Messages.log(STARTING_RUN, runID);
				initPass();
				runInstrProgram(fullcmd);
				donePass();
				if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID);
			}
			doneAllPasses();
			return;
		}
		boolean pipeTraces = traceKind.equals("pipe");
		List<Runnable> transformers = getTraceTransformers();
		int numTransformers = transformers == null ? 0 : transformers.size();
		List<String> basecmd = getBaseCmd(!offline, true, numTransformers);
		initAllPasses();
		for (String runID : runIDs) {
			if (pipeTraces) {
				for (int i = 0; i < numTransformers + 1; i++) {
					FileUtils.deleteFile(getTraceFileName(i));
					String[] cmd = new String[] { "mkfifo", getTraceFileName(i) };
					OutDirUtils.executeWithFailOnError(cmd);
				}
			}
			Runnable traceProcessor = new Runnable() {
				public void run() {
					processTrace(getTraceFileName(0));
				}
			};
			boolean serial = pipeTraces ? false : true;
			Executor executor = new Executor(serial);
			String args = System.getProperty("chord.args." + runID, "");
			final List<String> fullcmd = new ArrayList<String>(basecmd);
			fullcmd.addAll(StringUtils.tokenize(args));
			Runnable instrProgram = new Runnable() {
				public void run() {
					runInstrProgram(fullcmd);
				}
			};
			if (Config.verbose > 1) Messages.log(STARTING_RUN, runID);
			executor.execute(instrProgram);
			if (transformers != null) {
				for (Runnable r : transformers)
					executor.execute(r);
			}
			executor.execute(traceProcessor);
			try {
				executor.waitForCompletion();
			} catch (InterruptedException ex) {
				Messages.fatal(ex);
			}
			if (reuseTraces()) {
				String src = getTraceFileName(0);
				String dst = getTraceFileName(0, runID);
				String[] cmd = new String[] { "mv", src, dst };
				OutDirUtils.executeWithFailOnError(cmd);
			}
			if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID);
		}
		doneAllPasses();
	}

	private void doOfflineInstrumentation() {
		Pair<Class, Map<String, String>> instrKind = getInstrumentor();
		Class instrClass = instrKind.val0;
		Map<String, String> instrArgs = instrKind.val1;
		CoreInstrumentor instr = null;
		Exception ex = null;
        try {
            Constructor c = instrClass.getConstructor(new Class[] { Map.class });
            Object o = c.newInstance(new Object[] { instrArgs });
            instr = (CoreInstrumentor) o;
        } catch (InstantiationException e) {
            ex = e;
        } catch (NoSuchMethodException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        }
        if (ex != null)
            Messages.fatal(ex);
		OfflineTransformer transformer = new OfflineTransformer(instr);
		transformer.run();
	}

	private void runInstrProgram(List<String> cmdList) {
		int timeout = getTimeout();
		boolean haltOnErr = haltOnErr();
		
		String runBefore = System.getProperty("chord.dynamic.runBeforeCmd");
		
		try {
			Process beforeProc = null;
			if(runBefore != null)
				beforeProc = ProcessExecutor.executeAsynch( new String[] {runBefore});
			
			if (haltOnErr)
				OutDirUtils.executeWithFailOnError(cmdList);
			else
				OutDirUtils.executeWithWarnOnError(cmdList, timeout);
			
			if(beforeProc != null)
				beforeProc.destroy();
		} catch(Throwable t) { //just log exceptions
			t.printStackTrace();
		}
	}

	private List<String> getBaseCmd(boolean isOnline, boolean isWithTrace,
			int numTransformers) {
		String mainClassName = Config.mainClassName;
		assert (mainClassName != null);
		String classPathName = Config.classPathName;
		assert (classPathName != null);
		List<String> basecmd = new ArrayList<String>();
		basecmd.add("java");
		String jvmArgs = Config.runtimeJvmargs;
		basecmd.addAll(StringUtils.tokenize(jvmArgs));
		basecmd.add("-verbose");
		basecmd.add("-Xverify:none");
		if (isOnline) {
			Properties props = System.getProperties();
			for (Map.Entry e : props.entrySet()) {
				String key = (String) e.getKey();
				if (key.startsWith("chord.") && !key.equals("chord.out.pooldir"))
					basecmd.add("-D" + key + "=" + e.getValue());
			}
			basecmd.add("-cp");
			basecmd.add(classPathName);
		} else {
			String bootClassesDirName = Config.bootClassesDirName;
			String userClassesDirName = Config.userClassesDirName;
			String extraClasses = Config.extraClasses;
			if(extraClasses.length() > 0) {
			  extraClasses = extraClasses + File.pathSeparator;
			}
			basecmd.add("-Xbootclasspath/p:" + Config.mainClassPathName +
				File.pathSeparator + bootClassesDirName);
			basecmd.add("-cp");
			basecmd.add(extraClasses + userClassesDirName + File.pathSeparator +
				classPathName);
		}
		{
			Pair<Class, Map<String, String>> kind = getEventHandler();
			String name = kind.val0.getName().replace('.', '/');
			String args = mapToStr(kind.val1);
			String cAgentArgs = "=" + CoreEventHandler.EVENT_HANDLER_CLASS_KEY +
				"=" + name + args;
			if (isWithTrace) {
				int traceBlockSize = getTraceBlockSize();
				String traceFileName = getTraceFileName(numTransformers);
				cAgentArgs += "=trace_block_size=" + traceBlockSize +
					"=trace_file_name=" + traceFileName;
			}
			basecmd.add("-agentpath:" + Config.cInstrAgentFileName + cAgentArgs);
		}
		if (isOnline) {
			Pair<Class, Map<String, String>> kind = getInstrumentor();
			String name = kind.val0.getName().replace('.', '/');
			String args = mapToStr(kind.val1);
			String jAgentArgs = "=" + CoreInstrumentor.INSTRUMENTOR_CLASS_KEY +
				"=" + name + args;
			basecmd.add("-javaagent:" + Config.jInstrAgentFileName + jAgentArgs);
		}
		basecmd.add(mainClassName);
		return basecmd;
	}

	private static String mapToStr(Map<String, String> map) {
		String s = "";
		for (Map.Entry<String, String> e : map.entrySet()) {
			s += "=" + e.getKey() + "=" + e.getValue();
		}
		return s;
	}

	public void processTrace(String fileName) {
		try {
			initPass();
			ByteBufferedFile buffer = new ByteBufferedFile(
				getTraceBlockSize(), fileName, true);
			long count = 0;
			while (!buffer.isDone()) {
				handleEvent(buffer);
				++count;
			}
			donePass();
			if (Config.verbose > 1) Messages.log(FINISHED_PROCESSING_TRACE, count);
		} catch (IOException ex) {
			Messages.fatal(ex);
		} catch (ReadException ex) {
			Messages.fatal(ex);
		}
	}

	public void handleEvent(ByteBufferedFile buffer) throws IOException, ReadException {
		throw new RuntimeException();
	}
}
