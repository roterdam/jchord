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
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.util.StringUtils;
import chord.analyses.basicblock.DomB;
import chord.analyses.method.DomM;
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

	private static final String STARTING_RUN = "INFO: CoreDynamicAnalysis: Starting Run ID %s in %s mode.";
	private static final String FINISHED_RUN = "INFO: CoreDynamicAnalysis: Finished Run ID %s in %s mode.";
	private static final String FINISHED_PROCESSING_TRACE =
		"INFO: CoreDynamicAnalysis: Finished processing trace with %d events.";
	private static final String REUSE_ONLY_FULL_TRACES =
		"ERROR: CoreDynamicAnalysis: Can only reuse full traces.";
    private final static String INSTRUMENTOR_ARGS = "INFO: CoreDynamicAnalysis: Instrumentor arguments: %s";
    private final static String EVENTHANDLER_ARGS = "INFO: CoreDynamicAnalysis: Event handler arguments: %s";

	public final static boolean DEBUG = false;

	protected Map<String, String> eventHandlerArgs;
	protected Map<String, String> instrumentorArgs;

	// subclasses cannot override
	public final Map<String, String> getEventHandlerArgs() {
		if (eventHandlerArgs == null) {
			eventHandlerArgs = getImplicitEventHandlerArgs();
			Map<String, String> explicitArgs = getExplicitEventHandlerArgs();
			if (explicitArgs != null)
				eventHandlerArgs.putAll(explicitArgs);
			if (Config.verbose > 2) {
				String argsStr = "";
				for (Map.Entry<String, String> e : eventHandlerArgs.entrySet())
					argsStr += "\n\t[" + e.getKey() + " = " + e.getValue() + "]";
				Messages.log(EVENTHANDLER_ARGS, argsStr);
			}
		}
		return eventHandlerArgs;
	}

	// subclasses cannot override
	public final Map<String, String> getInstrumentorArgs() {
		if (instrumentorArgs == null) {
			instrumentorArgs = getImplicitInstrumentorArgs();
			Map<String, String> explicitArgs = getExplicitInstrumentorArgs();
			if (explicitArgs != null)
				instrumentorArgs.putAll(explicitArgs);
			if (Config.verbose > 2) {
				String argsStr = "";
				for (Map.Entry<String, String> e : instrumentorArgs.entrySet())
					argsStr += "\n\t[" + e.getKey() + " = " + e.getValue() + "]";
				Messages.log(INSTRUMENTOR_ARGS, argsStr);
			}
		}
		return instrumentorArgs;
	}

	// must return non-null map; if subclass overrides then must call super
	// and add its args to that map and return it
	protected Map<String, String> getImplicitInstrumentorArgs() {
		Map<String, String> args = new HashMap<String, String>();
		if (!useJvmti()) {
			args.put(CoreInstrumentor.USE_JVMTI_KEY, "false");
			String c = getEventHandlerClass().getName();
			Map<String, String> ehArgs = getEventHandlerArgs();
			String a = mapToStr(ehArgs, '@');
			if (a.length() > 0) a = a.substring(1);
			args.put(CoreInstrumentor.EVENT_HANDLER_CLASS_KEY, c);
			args.put(CoreInstrumentor.EVENT_HANDLER_ARGS_KEY, a);
		}
		return args;
	}

	protected Map<String, String> getImplicitEventHandlerArgs() {
		Map<String, String> args = new HashMap<String, String>();
		if (!getTraceKind().equals("none")) {
			int traceBlockSize = getTraceBlockSize();
			String traceFileName = getTraceFileName(getTraceTransformers().size());
 			args.put(CoreEventHandler.TRACE_BLOCK_SIZE_KEY, Integer.toString(traceBlockSize));
			args.put(CoreEventHandler.TRACE_FILE_NAME_KEY, traceFileName);
		}
		return args;
	}

	// subclasses can override
	public Map<String, String> getExplicitInstrumentorArgs() {
		return null;
	}

	// subclasses can override
	public Map<String, String> getExplicitEventHandlerArgs() {
		return null;
	}

	// subclasses can override
	public Class getInstrumentorClass() {
		return CoreInstrumentor.class;
	}

	// subclasses can override
	public Class getEventHandlerClass() {
		return CoreEventHandler.class;
	}

	// subclasses can override
	public boolean useJvmti() {
		return Config.useJvmti;
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
				if (Config.verbose > 1) Messages.log(STARTING_RUN, runID, "reuse");
				String s = getTraceFileName(0, runID);
				processTrace(s);
				if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID, "reuse");
			}
			doneAllPasses();
			return;
		}
		boolean offline = getInstrKind().equals("offline");
		boolean useJvmti = useJvmti();
		if (offline)
			doOfflineInstrumentation();
		if (traceKind.equals("none")) {
			String msg = "single-JVM " + (offline ? "offline" : "online") + "-instrumentation " +
				(useJvmti ? "JVMTI-based" : "non-JVMTI");
			List<String> basecmd = getBaseCmd(!offline, useJvmti, false, 0);
			initAllPasses();
			for (String runID : runIDs) {
				String args = System.getProperty("chord.args." + runID, "");
				List<String> fullcmd = new ArrayList<String>(basecmd);
				fullcmd.addAll(StringUtils.tokenize(args));
				if (Config.verbose > 1) Messages.log(STARTING_RUN, runID, msg);
				initPass();
				runInstrProgram(fullcmd);
				donePass();
				if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID, msg);
			}
			doneAllPasses();
			return;
		}
		boolean pipeTraces = traceKind.equals("pipe");
		List<Runnable> transformers = getTraceTransformers();
		int numTransformers = transformers == null ? 0 : transformers.size();
		List<String> basecmd = getBaseCmd(!offline, useJvmti, true, numTransformers);
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
			Executor executor = new Executor(!pipeTraces);
			String args = System.getProperty("chord.args." + runID, "");
			final List<String> fullcmd = new ArrayList<String>(basecmd);
			fullcmd.addAll(StringUtils.tokenize(args));
			Runnable instrProgram = new Runnable() {
				public void run() {
					runInstrProgram(fullcmd);
				}
			};
			String msg = "multi-JVM " + (pipeTraces ? "POSIX-pipe " : "regular-file ") +
				(offline ? "offline" : "online") + "-instrumentation " +
				(useJvmti ? "JVMTI-based" : "non-JVMTI");
			if (Config.verbose > 1) Messages.log(STARTING_RUN, runID, msg);
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
			if (Config.verbose > 1) Messages.log(FINISHED_RUN, runID, msg);
		}
		doneAllPasses();
	}

	private void doOfflineInstrumentation() {
		Class instrClass = getInstrumentorClass();
		Map<String, String> instrArgs = getInstrumentorArgs();
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

	private List<String> getBaseCmd(boolean isOnline, boolean useJvmti,
			boolean isWithTrace, int numTransformers) {
		String mainClassName = Config.mainClassName;
		assert (mainClassName != null);
		String classPathName = Config.classPathName;
		assert (classPathName != null);
		List<String> basecmd = new ArrayList<String>();
		basecmd.add("java");
		String jvmArgs = Config.runtimeJvmargs;
		basecmd.addAll(StringUtils.tokenize(jvmArgs));
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
			basecmd.add("-Xbootclasspath/p:" + Config.mainClassPathName +
				File.pathSeparator + bootClassesDirName);
			basecmd.add("-cp");
			basecmd.add(userClassesDirName + File.pathSeparator + classPathName);
		}
		if (useJvmti) {
			String name = getEventHandlerClass().getName().replace('.', '/');
			String args = mapToStr(getEventHandlerArgs(), '=');
			String cAgentArgs = "=" + CoreInstrumentor.EVENT_HANDLER_CLASS_KEY +
				"=" + name + args;
			basecmd.add("-agentpath:" + Config.cInstrAgentFileName + cAgentArgs);
		}
		if (isOnline) {
			String name = getInstrumentorClass().getName().replace('.', '/');
			String args = mapToStr(getInstrumentorArgs(), '=');
			String jAgentArgs = "=" + CoreInstrumentor.INSTRUMENTOR_CLASS_KEY +
				"=" + name + args;
			basecmd.add("-javaagent:" + Config.jInstrAgentFileName + jAgentArgs);
		}
		basecmd.add(mainClassName);
		return basecmd;
	}

	private static String mapToStr(Map<String, String> map, char sep) {
		String s = "";
		for (Map.Entry<String, String> e : map.entrySet()) {
			s += sep + e.getKey() + sep + e.getValue();
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
