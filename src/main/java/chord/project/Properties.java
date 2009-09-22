/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;

/**
 * System properties recognized by Chord:
 *
 * Chord I/O properties
 *
 * user.dir
 *     Location of working directory during Chord's execution
 * chord.out.dir
 *     Location of directory to which Chord dumps all files
 * chord.out.file
 *     Location of file to which standard output stream is redirected
 *         (default=${chord.out.dir}/log.txt)
 * chord.err.file
 *     Location of file to which standard error stream is redirected
 *         (default=${chord.out.dir}/log.txt)
 *
 * Program properties
 *
 * chord.main.class
 *     Fully-qualified name of main class of program to be analyzed (e.g. "com.example.Main")
 * chord.class.path
 *     Class path of program to be analyzed (${sun.boot.class.path} is implicitly appended)
 * chord.src.path
 *     Java source path of program to be analyzed
 *
 * Program scope properties
 *
 * chord.scope.kind
 *     Algorithm to compute program scope; possible values are "rta" and "dynamic" (default="rta")
 * chord.reuse.scope
 *     Use program scope specified by files ${chord.boot.classes.file}, ${chord.classes.file},
 *         and ${chord.methods.file} (default=false)
 * chord.classes.file
 *     Location of file from/to which list of classes deemed reachable is read/written
 *         (default=${chord.out.dir}/classes.txt)
 * chord.methods.file
 *     Location of file from/to which list of methods deemed reachable is read/written
 *         (default=${chord.out.dir}/methods.txt)
 *
 * Program analysis properties
 *
 * chord.java.analysis.path
 *     Classpath containing program analyses expressed in Java 
 *         (i.e. @Chord-annotated classes) to be included in the project
 * chord.dlog.analysis.path
 *     Path of dirs containing program analyses expressed in Datalog
 *         (i.e. .datalog and .dlog files) to be included in the project
 * chord.analyses
 *     List of names of program analyses to be run in order;
 *         separator = ' |,|:|;' (default=empty list)
 *
 * BDD-based Datalog solver properties
 * 
 * chord.bddbddb.work.dir
 *     
 * chord.bddbddb.max.heap
 *     
 * chord.bddbddb.noisy
 *     
 * Program instrumentation properties
 *
 * chord.instr.exclude
 *     List of prefixes of names of classes and packages to exclude from instrumentation;
 *         separator = ' |,|:|;' (default="java.,sun.,com.")
 * chord.run.ids
 *     List of IDs to identify program runs; separator = ' |,|:|;' (default="0")
 * chord.args.XXX
 *     Command line arguments to be passed to run having ID XXX (default="")
 * chord.boot.classes.dir
 *    
 * chord.user.classes.dir
 *     
 * chord.runtime.max.heap
 *
 * chord.instr.scheme.file
 *     
 * chord.crude.trace.file
 *     
 * chord.final.trace.file
 *     
 * chord.trace.block.size
 *
 * Program transformation properties
 *
 * chord.ssa
 *     Do SSA transformation for all methods deemed reachable (default=false)
 *
 * Chord resource properties
 *
 * chord.home.dir
 *     Location of root directory of Chord's installation
 * chord.main.class.path
 *     
 * chord.bddbddb.class.path
 *     
 * chord.instr.agent.file
 *     
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

	public final static String bddbddbWorkDirName = System.getProperty("chord.bddbddb.work.dir", outDirName);
	public final static String bddbddbMaxHeap = System.getProperty("chord.bddbddb.max.heap", "1024m");
	public final static String bddbddbNoisy = System.getProperty("chord.bddbddb.noisy", "no");

	// Program instrumentation properties

	public final static String instrExcludeNames =
		System.getProperty("chord.instr.exclude", "java.,sun.,com.");
	public final static String runIDs = System.getProperty("chord.run.ids", "0");
	public final static String bootClassesDirName = build("chord.boot.classes.dir", "boot_classes");
	public final static String userClassesDirName = build("chord.user.classes.dir", "user_classes");
	public final static String runtimeMaxHeap = System.getProperty("chord.runtime.max.heap", "1024m");
	public final static String instrSchemeFileName = build("chord.instr.scheme.file", "scheme.ser");
	public final static String crudeTraceFileName = build("chord.crude.trace.file", "crude_trace.txt");
	public final static String finalTraceFileName = build("chord.final.trace.file", "final_trace.txt");
	public final static int traceBlockSize = Integer.getInteger("chord.trace.block.size", 4096);

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
		System.out.println("chord.instr.exclude: " + instrExcludeNames);
		System.out.println("chord.run.ids: " + runIDs);
		System.out.println("chord.boot.classes.dir: " + bootClassesDirName);
		System.out.println("chord.user.classes.dir: " + userClassesDirName);
		System.out.println("chord.runtime.max.heap: " + runtimeMaxHeap);
		System.out.println("chord.instr.scheme.file: " + instrSchemeFileName);
		System.out.println("chord.crude.trace.file: " + crudeTraceFileName);
		System.out.println("chord.final.trace.file: " + finalTraceFileName);
		System.out.println("chord.trace.block.size: " + traceBlockSize);
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
