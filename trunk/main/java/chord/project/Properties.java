package chord.project;

import java.io.File;

public class Properties {
	private Properties() { }

	public static final String homeDirName = System.getProperty("chord.home.dir");
	public static final String outFileName = System.getProperty("chord.out");
	public static final String errFileName = System.getProperty("chord.err");

	public static final String workDirName = System.getProperty("user.dir");
	public static final String outDirName = System.getProperty("chord.out.dir");
	public static final String classesDirName = System.getProperty("chord.classes.dir");

	public static final String mainClassName = System.getProperty("chord.main.class");
	public static final String classPathName = System.getProperty("chord.class.path");
	public static final String srcPathName = System.getProperty("chord.src.path");
	public static final String bootClassPathName =
		System.getProperty("chord.boot.class.path");
	
	public static final String javaAnalysisPathName =
		System.getProperty("chord.java.analysis.path");
	public static final String dlogAnalysisPathName =
		System.getProperty("chord.dlog.analysis.path");
	public static final String analyses =
		System.getProperty("chord.analyses");

	public final static String bddbddbClassPathName =
		System.getProperty("chord.bddbddb.class.path");
	public final static String bddbddbWorkDirName =
		System.getProperty("chord.bddbddb.work.dir");
	public final static String bddbddbMaxHeap =
		System.getProperty("chord.bddbddb.max.heap");
	public final static String bddbddbNoisy =
		System.getProperty("chord.bddbddb.noisy");
	public final static String bddLibDirName =
		System.getProperty("chord.bdd.lib.dir");
	public final static String traceAgentFileName =
		System.getProperty("chord.trace.agent.file");
	public final static String instrAgentFileName =
		System.getProperty("chord.instr.agent.file");
	public final static boolean doInstr = System.getProperty(
		"chord.instr", "false").equals("true");
	public final static String traceFileName =
		System.getProperty("chord.trace.file", "trace.txt");

	public static void print() {
		System.out.println("chord.home.dir: " + homeDirName);
		System.out.println("chord.out: " + outFileName);
		System.out.println("chord.err: " + errFileName);
		System.out.println("chord.work.dir: " + workDirName);
		System.out.println("chord.out.dir: " + outDirName);
		System.out.println("chord.classes.dir: " + classesDirName);
		System.out.println("chord.main.class: " + mainClassName);
		System.out.println("chord.class.path: " + classPathName);
		System.out.println("chord.src.path: " + srcPathName);
		System.out.println("chord.boot.class.path: " + bootClassPathName);
		System.out.println("chord.java.analysis.path: " + javaAnalysisPathName);
		System.out.println("chord.dlog.analysis.path: " + dlogAnalysisPathName);
		System.out.println("chord.analyses: " + analyses);
		System.out.println("chord.bddbddb.class.path: " + bddbddbClassPathName);
		System.out.println("chord.bddbddb.work.dir: " + bddbddbWorkDirName);
		System.out.println("chord.bddbddb.max.heap: " + bddbddbMaxHeap);
		System.out.println("chord.bddbddb.noisy: " + bddbddbNoisy);
		System.out.println("chord.bdd.lib.dir: " + bddLibDirName);
		System.out.println("chord.trace.agent.file: " + traceAgentFileName);
		System.out.println("chord.instr.agent.file: " + instrAgentFileName);
		System.out.println("chord.instr: " + doInstr);
	}
}
