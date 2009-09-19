/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;

/**
 * System properties recognized by Chord:
 * <p>
 * <table border=1>
 * <tr><td colspan=2>Chord I/O properties</td></tr>
 *
 * <tr><td><tt>user.dir</tt></td>
 *     <td>Location of working directory during Chord's execution</td></tr>
 * <tr><td><tt>chord.out.dir</tt></td>
 *     <td>Location of directory to which Chord dumps all files</td></tr>
 * <tr><td><tt>chord.out.file</tt></td>
 *     <td>Location of file to which standard output stream is redirected
 *         (default=${chord.out.dir}/log.txt)</td></tr>
 * <tr><td><tt>chord.err.file</tt></td>
 *     <td>Location of file to which standard error stream is redirected
 *         (default=${chord.out.dir}/log.txt)</td></tr>
 *
 * <tr><td colspan=2>Program properties</td></tr>
 *
 * <tr><td><tt>chord.main.class</tt></td>
 *     <td>Fully-qualified name of main class of program to be analyzed (e.g. "com.example.Main")</td></tr>
 * <tr><td><tt>chord.class.path</tt></td>
 *     <td>Class path of program to be analyzed (${sun.boot.class.path} is implicitly appended)</td></tr>
 * <tr><td><tt>chord.src.path</tt></td>
 *     <td>Java source path of program to be analyzed</td></tr>
 *
 * <tr><td colspan=2>Program scope properties</td></tr>
 *
 * <tr><td><tt>chord.scope.kind</tt></td>
 *     <td>Algorithm to compute program scope; possible values are "rta" and "dynamic" (default="rta")</td></tr>
 * <tr><td><tt>chord.reuse.scope</tt></td>
 *     <td>Use program scope specified by files ${chord.boot.classes.file}, ${chord.classes.file},
 *         and ${chord.methods.file} (default=false)</td></tr>
 * <tr><td><tt>chord.boot.classes.file</tt></td>
 *     <td>Location of file from/to which list of system classes (e.g. sun.* and java.*)
 *         deemed reachable is read/written (default=${chord.out.dir}/boot_classes.txt)</td></tr>
 * <tr><td><tt>chord.classes.file</tt></td>
 *     <td>Location of file from/to which list of non-system classes deemed reachable is read/written
 *         (default=${chord.out.dir}/boot_classes.txt)</td></tr>
 * <tr><td><tt>chord.methods.file</tt></td>
 *     <td>Location of file from/to which list of all methods deemed reachable is read/written
 *         (default=${chord.out.dir}/methods.txt)</td></tr>
 *
 * <tr><td colspan=2>Program analysis properties</td></tr>
 *
 * <tr><td><tt>chord.java.analysis.path</tt></td>
 *     <td>Classpath containing program analyses expressed in Java 
 *         (i.e. @Chord-annotated classes) to be included in the project</td></tr>
 * <tr><td><tt>chord.dlog.analysis.path</tt></td>
 *     <td>Path of dirs containing program analyses expressed in Datalog
 *         (i.e. .datalog and .dlog files) to be included in the project</td></tr>
 * <tr><td><tt>chord.analyses</tt></td>
 *     <td>List of names of program analyses to be run in order;
 *         separator = <tt>' |,|:|;'</tt> (default=empty list)</td></tr>
 *
 * <tr><td colspan=2>BDD-based Datalog solver properties</td></tr>
 * 
 * <tr><td><tt>chord.bddbddb.work.dir</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.bddbddb.max.heap</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.bddbddb.noisy</tt></td>
 *     <td></td></tr>
 *
 * <tr><td colspan=2>Program instrumentation properties</td></tr>
 *
 * <tr><td><tt>chord.instr.exclude</tt></td>
 *     <td>List of prefixes of names of classes and packages to exclude from instrumentation;
 *         separator = <tt>' |,|:|;'</tt> (default="java.,sun.,com.")</td></tr>
 * <tr><td><tt>chord.run.ids</tt></td>
 *     <td>List of IDs to identify program runs; separator = <tt>' |,|:|;'</tt> (default="0")</td></tr>
 * <tr><td><tt>chord.args.XXX</tt></td>
 *     <td>Command line arguments to be passed to run having ID XXX (default="")</td></tr>
 * <tr><td><tt>chord.calls.bound</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.iters.bound</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.boot.classes.dir</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.user.classes.dir</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.instr.scheme.file</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.crude.trace.file</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.final.trace.file</tt></td>
 *     <td></td></tr>
 *
 * <tr><td colspan=2>Program transformation properties</td></tr>
 *
 * <tr><td><tt>chord.ssa</tt></td>
 *     <td>Do SSA transformation for all methods deemed reachable (default=false)</td></tr>
 *
 * <tr><td colspan=2>Chord resource properties</td></tr>
 *
 * <tr><td><tt>chord.home.dir</tt></td>
 *     <td>Location of root directory of Chord's installation</td></tr>
 * <tr><td><tt>chord.main.class.path</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.bddbddb.class.path</tt></td>
 *     <td></td></tr>
 * <tr><td><tt>chord.instr.agent.file</tt></td>
 *     <td></td></tr>
 * </table>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Properties {
	private Properties() { }

	public final static String LIST_SEPARATOR = " |,|:|;";

	// Chord I/O properties

	public final static String workDirName = System.getProperty("user.dir");
	public final static String outDirName = System.getProperty("chord.out.dir");
	static {
		assert(outDirName != null);
	}
	public final static String outFileName = build("chord.out.file", "log.txt");
	public final static String errFileName = build("chord.err.file", "log.txt");

    // Program properties

	public final static String mainClassName = System.getProperty("chord.main.class");
	public final static String classPathName = System.getProperty("chord.class.path");
	public final static String srcPathName = System.getProperty("chord.src.path");
	
    // Program scope properties

	public final static String scopeKind = System.getProperty("chord.scope.kind", "rta");
	static {
		assert (scopeKind.equals("rta") || scopeKind.equals("dynamic"));
	}
	public final static boolean reuseScope = buildBoolProp("chord.reuse.scope", false);
	public final static String classesFileName = build("chord.classes.file", "classes.txt");
	public final static String methodsFileName = build("chord.methods.file", "methods.txt");

    // Program analysis properties

	public final static String javaAnalysisPathName = System.getProperty("chord.java.analysis.path");
	public final static String dlogAnalysisPathName = System.getProperty("chord.dlog.analysis.path");
	public final static String analyses = System.getProperty("chord.analyses");

    // BDD-based Datalog solver properties

	public final static String bddbddbWorkDirName = System.getProperty("chord.bddbddb.work.dir");
	public final static String bddbddbMaxHeap = System.getProperty("chord.bddbddb.max.heap");
	public final static String bddbddbNoisy = System.getProperty("chord.bddbddb.noisy");

	// Program instrumentation properties

	public final static String instrExcludedPckgs = System.getProperty("chord.instr.exclude", "java.,sun.,com.");
	public final static String runIDs = System.getProperty("chord.run.ids", "0");
	public final static int callsBound = Integer.getInteger("chord.calls.bound", 0);
	public final static int itersBound = Integer.getInteger("chord.iters.bound", 0);
	public final static String bootClassesDirName = build("chord.boot.classes.dir", "boot_classes");
	public final static String userClassesDirName = build("chord.user.classes.dir", "user_classes");

	public final static String instrSchemeFileName = build("chord.instr.scheme.file", "scheme.ser");
	public final static String crudeTraceFileName = build("chord.crude.trace.file", "crude_trace.txt");
	public final static String finalTraceFileName = build("chord.final.trace.file", "final_trace.txt");

    // Program transformation properties
 
	public final static boolean doSSA = buildBoolProp("chord.ssa", true);

    // Chord resource properties
 
	public final static String homeDirName = System.getProperty("chord.home.dir");
	static {
		assert(homeDirName != null);
	}
	public final static String mainClassPathName = System.getProperty("chord.main.class.path");
	public final static String bddbddbClassPathName = System.getProperty("chord.bddbddb.class.path");
	public final static String bddLibDirName = System.getProperty("chord.bdd.lib.dir");
	public final static String instrAgentFileName = System.getProperty("chord.instr.agent.file");

	public static void print() {
		System.out.println("******************************");
		System.out.println("chord.work.dir: " + workDirName);
		System.out.println("chord.out.dir: " + outDirName);
		System.out.println("chord.out.file: " + outFileName);
		System.out.println("chord.err.file: " + errFileName);
		System.out.println();
		System.out.println("chord.main.class: " + mainClassName);
		System.out.println("chord.class.path: " + classPathName);
		System.out.println("chord.src.path: " + srcPathName);
		System.out.println();
		System.out.println("chord.scope.kind: " + scopeKind);
		System.out.println("chord.reuse.scope: " + reuseScope);
		System.out.println("chord.classes.file: " + classesFileName);
		System.out.println("chord.methods.file: " + methodsFileName);
		System.out.println();
		System.out.println("chord.java.analysis.path: " + javaAnalysisPathName);
		System.out.println("chord.dlog.analysis.path: " + dlogAnalysisPathName);
		System.out.println("chord.analyses: " + analyses);
		System.out.println();
		System.out.println("chord.bddbddb.work.dir: " + bddbddbWorkDirName);
		System.out.println("chord.bddbddb.max.heap: " + bddbddbMaxHeap);
		System.out.println("chord.bddbddb.noisy: " + bddbddbNoisy);
		System.out.println();
		System.out.println("chord.instr.exclude: " + instrExcludedPckgs);
		System.out.println("chord.run.ids: " + runIDs);
		System.out.println("chord.calls.bound: " + callsBound);
		System.out.println("chord.iters.bound: " + itersBound);
		System.out.println("chord.boot.classes.dir: " + bootClassesDirName);
		System.out.println("chord.user.classes.dir: " + userClassesDirName);
		System.out.println();
		System.out.println("chord.ssa: " + doSSA);
		System.out.println();
		System.out.println("chord.home.dir: " + homeDirName);
		System.out.println("chord.main.class.path: " + mainClassPathName);
		System.out.println("chord.bddbddb.class.path: " + bddbddbClassPathName);
		System.out.println("chord.bdd.lib.dir: " + bddLibDirName);
		System.out.println("chord.instr.agent.file: " + instrAgentFileName);
		System.out.println("******************************");
	}
	public static String build(String propName, String fileName) {
		String val = System.getProperty(propName);
		return (val != null) ? val :
			(new File(outDirName, fileName)).getAbsolutePath();
	}
	public static boolean buildBoolProp(String propName, boolean defaultVal) {
		return System.getProperty(propName, Boolean.toString(defaultVal)).equals("true"); 
	}
}
