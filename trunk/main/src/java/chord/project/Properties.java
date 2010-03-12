/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import chord.util.FileUtils;
import chord.util.ChordRuntimeException;

/**
 * System properties recognized by Chord.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Properties {
	private Properties() { }

	public final static String LIST_SEPARATOR = " |,|:|;";
	// added com.ibm.misc.SignalDispatcher because BASIC_BLOCK event in its run() method is reached without reaching ENTER_METHOD event
	public final static String DEFAULT_SCOPE_EXCLUDES =
		"sun.,com.sun.,com.ibm.jvm.,com.ibm.oti.,org.apache.harmony.,joeq.,jwutil.,com.ibm.misc.SignalDispatcher";
	public final static String DEFAULT_CHECK_EXCLUDES =
		"sun.,com.sun.,com.ibm.jvm.,com.ibm.oti.,org.apache.harmony.,joeq.,jwutil.,com.ibm.misc.SignalDispatcher,java.,javax.";

	// Chord resource properties

	public final static String mainDirName = System.getProperty("chord.main.dir");
	static {
		assert(mainDirName != null);
	}
	public static String libDirName = System.getProperty("chord.lib.dir");
	static {
		if (libDirName == null)
			libDirName = FileUtils.getAbsolutePath(mainDirName, "lib");
	}
	public final static String mainClassPathName = System.getProperty("chord.main.class.path");
	public final static String bddbddbClassPathName = System.getProperty("chord.bddbddb.class.path");
	public static String instrAgentFileName = System.getProperty("chord.instr.agent.file");
	static {
		if (instrAgentFileName == null)
			instrAgentFileName = FileUtils.getAbsolutePath(mainDirName, "lib/libchord_instr_agent.so");
	}
	public final static String javadocURL = System.getProperty("chord.javadoc.url",
		"http://chord.stanford.edu/javadoc_2_0/");

	// Chord boot properties

	public static String workDirName = System.getProperty("chord.work.dir");
	static {
		if (workDirName == null)
			workDirName = System.getProperty("user.dir");
	}
	public final static String propsFileName = System.getProperty("chord.props.file");
	public final static String maxHeap = System.getProperty("chord.max.heap");
	public final static String maxStack = System.getProperty("chord.max.stack");
	public final static String jvmargs = System.getProperty("chord.jvmargs");
	public final static String bddbddbMaxHeap = System.getProperty("chord.bddbddb.max.heap", "1024m");

	// Chord task properties

	public final static boolean buildScope = buildBoolProp("chord.build.scope", false);
	public final static String runAnalyses = System.getProperty("chord.run.analyses", "");
	public final static String printRels = System.getProperty("chord.print.rels", "");
	public final static boolean publishTargets = buildBoolProp("chord.publish.targets", false);

	// Basic program properties

	public final static String mainClassName = System.getProperty("chord.main.class");
	public final static String classPathName = System.getProperty("chord.class.path");
	public final static String srcPathName = System.getProperty("chord.src.path");
	public final static String runIDs = System.getProperty("chord.run.ids", "0");
	public final static String runtimeJvmargs =
		System.getProperty("chord.runtime.jvmargs", "-ea -Xmx1024m");

	// Program scope properties

	public final static String scopeKind = System.getProperty("chord.scope.kind", "rta");
	static {
		assert (scopeKind.equals("rta") || scopeKind.equals("dynamic"));
	}
	public final static boolean reuseScope = buildBoolProp("chord.reuse.scope", false);
	public final static String scopeExcludeStr = System.getProperty("chord.scope.exclude", DEFAULT_SCOPE_EXCLUDES);
	public final static String checkExcludeStr = System.getProperty("chord.check.exclude", DEFAULT_CHECK_EXCLUDES);

	// Program analysis properties

	public static String javaAnalysisPathName = System.getProperty("chord.java.analysis.path");
	static {
		if (javaAnalysisPathName == null)
			javaAnalysisPathName = FileUtils.getAbsolutePath(mainDirName, "classes");
	}
	public static String dlogAnalysisPathName =
		System.getProperty("chord.dlog.analysis.path");
	static {
		if (dlogAnalysisPathName == null) {
			dlogAnalysisPathName = FileUtils.getAbsolutePath(mainDirName, "src/dlog");
		}
	}
	public final static boolean reuseRels = buildBoolProp("chord.reuse.rels", false);
	public final static boolean publishResults = buildBoolProp("chord.publish.results", true);

    // Program transformation properties
 
	public final static boolean doSSA = buildBoolProp("chord.ssa", true);

    // Chord debug properites

	public final static boolean bddbddbNoisy = buildBoolProp("chord.bddbddb.noisy", false);
	public final static boolean saveDomMaps = buildBoolProp("chord.save.maps", true);
	public final static int verboseLevel = Integer.getInteger("chord.verbose.level", 0);
	public final static int instrVerboseLevel = Integer.getInteger("chord.instr.verbose.level", 0);

	// Chord instrumentation properties

	public final static boolean doTracePipe = buildBoolProp("chord.trace.pipe", true);
	public final static int traceBlockSize = Integer.getInteger("chord.trace.block.size", 4096);
	public final static String runtimeClassName =
		System.getProperty("chord.runtime.class", chord.runtime.BufferedRuntime.class.getName());

	// Chord output properties

	public static String outDirName = System.getProperty("chord.out.dir");
	static {
    // Automatically find a free subdirectory
    String outPoolPath = System.getProperty("chord.out.pooldir");
    // System.out.println("OUT "+outPoolPath);
    if (outPoolPath != null) {
      for (int i = 0; ; i++) {
        outDirName = outPoolPath+"/"+i+".exec";
        if (!new File(outDirName).exists()) break;
      }
    }
		if (outDirName == null)
			outDirName = FileUtils.getAbsolutePath(workDirName, "chord_output");
		FileUtils.mkdirs(outDirName);
	}
	public final static String outFileName = build("chord.out.file", "log.txt");
	public final static String errFileName = build("chord.err.file", "log.txt");
	public final static String classesFileName = build("chord.classes.file", "classes.txt");
	public final static String methodsFileName = build("chord.methods.file", "methods.txt");
	public static String bddbddbWorkDirName = System.getProperty("chord.bddbddb.work.dir");
	static {
		if (bddbddbWorkDirName == null)
			bddbddbWorkDirName = FileUtils.getAbsolutePath(outDirName, "bddbddb");
		FileUtils.mkdirs(bddbddbWorkDirName);
	}
	public final static String bootClassesDirName = build("chord.boot.classes.dir", "boot_classes");
	public final static String userClassesDirName = build("chord.user.classes.dir", "user_classes");
	public final static String instrSchemeFileName = build("chord.instr.scheme.file", "scheme.ser");
	public final static String crudeTraceFileName = build("chord.crude.trace.file", "crude_trace.txt");
	public final static String finalTraceFileName = build("chord.final.trace.file", "final_trace.txt");

	public final static String propsDebugFileName = build("chord.props.debug.file", "props_debug.txt");
	public final static String projectDebugFileName = build("chord.project.debug.file", "project_debug.txt");
	public final static String scopeDebugFileName = build("chord.scope.debug.file", "scope_debug.txt");
	public final static String instrDebugFileName = build("chord.instr.debug.file", "instr_debug.txt");

	public static void print() {
		System.out.println("*** Chord resource properties:");
		System.out.println("chord.main.dir: " + mainDirName);
		System.out.println("chord.lib.dir: " + libDirName);
		System.out.println("chord.main.class.path: " + mainClassPathName);
		System.out.println("chord.bddbddb.class.path: " + bddbddbClassPathName);
		System.out.println("chord.instr.agent.file: " + instrAgentFileName);
		System.out.println("chord.javadoc.url: " + javadocURL);

		System.out.println("*** Chord boot properties:");
		System.out.println("chord.work.dir: " + workDirName);
		System.out.println("chord.props.file: " + propsFileName);
		System.out.println("chord.max.heap: " + maxHeap);
		System.out.println("chord.max.stack: " + maxStack);
		System.out.println("chord.jvmargs: " + jvmargs);
		System.out.println("chord.bddbddb.max.heap: " + bddbddbMaxHeap);

		System.out.println("*** Chord task properties:");
		System.out.println("chord.build.scope: " + buildScope);
		System.out.println("chord.run.analyses: " + runAnalyses);
		System.out.println("chord.print.rels: " + printRels);
		System.out.println("chord.publish.targets: " + publishTargets);

		System.out.println("*** Basic program properties:");
		System.out.println("chord.main.class: " + mainClassName);
		System.out.println("chord.class.path: " + classPathName);
		System.out.println("chord.src.path: " + srcPathName);
		System.out.println("chord.run.ids: " + runIDs);
		// TODO: args.XXX
		System.out.println("chord.runtime.jvmargs: " + runtimeJvmargs);

		System.out.println("*** Program scope properties:");
		System.out.println("chord.scope.kind: " + scopeKind);
		System.out.println("chord.reuse.scope: " + reuseScope);
		System.out.println("chord.scope.exclude: " + scopeExcludeStr);
		System.out.println("chord.check.exclude: " + checkExcludeStr);

		System.out.println("*** Program analysis properties:");
		System.out.println("chord.java.analysis.path: " + javaAnalysisPathName);
		System.out.println("chord.dlog.analysis.path: " + dlogAnalysisPathName);
		System.out.println("chord.reuse.rels: " + reuseRels);
		System.out.println("chord.publish.results: " + publishResults);

		System.out.println("*** Program transformation properties:");
		System.out.println("chord.ssa: " + doSSA);

		System.out.println("*** Chord debug properties:");
		System.out.println("chord.verbose.level: " + verboseLevel);
		System.out.println("chord.bddbddb.noisy: " + bddbddbNoisy);
		System.out.println("chord.save.maps: " + saveDomMaps);

		System.out.println("*** Chord instrumentation properties:");
		System.out.println("chord.trace.pipe: " + doTracePipe);
		System.out.println("chord.trace.block.size: " + traceBlockSize);
		System.out.println("chord.runtime.class: " + runtimeClassName);

		System.out.println("*** Chord output properties:");
		System.out.println("chord.out.dir: " + outDirName);
		System.out.println("chord.out.file: " + outFileName);
		System.out.println("chord.err.file: " + errFileName);
		System.out.println("chord.classes.file: " + classesFileName);
		System.out.println("chord.methods.file: " + methodsFileName);
		System.out.println("chord.bddbddb.work.dir: " + bddbddbWorkDirName);
		System.out.println("chord.boot.classes.dir: " + bootClassesDirName);
		System.out.println("chord.user.classes.dir: " + userClassesDirName);
		System.out.println("chord.instr.scheme.file: " + instrSchemeFileName);
		System.out.println("chord.crude.trace.file: " + crudeTraceFileName);
		System.out.println("chord.final.trace.file: " + finalTraceFileName);
	}
	private static String build(String propName, String fileName) {
		String val = System.getProperty(propName);
		return (val != null) ? val :
			(new File(outDirName, fileName)).getAbsolutePath();
	}
	private static boolean buildBoolProp(String propName, boolean defaultVal) {
		return System.getProperty(propName, Boolean.toString(defaultVal)).equals("true"); 
	}
	public static String[] toArray(String str) {
        return str.equals("") ? new String[0] : str.split(LIST_SEPARATOR);
	}
}
