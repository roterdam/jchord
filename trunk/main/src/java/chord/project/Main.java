/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.io.File;
import java.io.PrintStream;

import chord.program.Program;
import chord.util.Timer;

/**
 * Main entry point of Chord.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Main {
	public static void main(String[] args) throws Exception {
        PrintStream outStream = null;
        PrintStream errStream = null;
        final String outFileName = Config.outFileName;
		final String errFileName = Config.errFileName;
		System.out.println("Redirecting stdout to file: " + outFileName);
		System.out.println("Redirecting stderr to file: " + errFileName);
		final File outFile = new File(outFileName);
		outStream = new PrintStream(outFile);
		System.setOut(outStream);
		final File errFile = new File(errFileName);
		if (errFile.equals(outFile))
			errStream = outStream;
		else
			errStream = new PrintStream(errFile);
		System.setErr(errStream);
		run();
		outStream.close();
		if (errStream != outStream)
			errStream.close();
	}
	private static void run() {
		if (Config.verbose > 0)
			Config.print();
		Timer timer = new Timer("chord");
		timer.init();
		String initTime = timer.getInitTimeStr();
		Program program = Program.g();
		Project project = Project.g();
		if (Config.buildProgram) {
			program.build();
		}
		if (Config.printAllClasses)
			program.printAllClasses();
		String[] printClasses = Config.toArray(Config.printClasses);
		if (printClasses.length > 0) {
			for (String className : printClasses)
				program.printClass(className);
		}
		String[] printMethods = Config.toArray(Config.printMethods);
		if (printMethods.length > 0) {
			for (String methodSign : printMethods)
				program.printMethod(methodSign);
		}
		String[] analysisNames = Config.toArray(Config.runAnalyses);
		if (analysisNames.length > 0) {
			project.run(analysisNames);
		}
        String[] relNames = Config.toArray(Config.printRels);
        if (relNames.length > 0) {
        	project.printRels(relNames);
        }
		if (Config.printProject) {
			project.print();
		}
		timer.done();
		String doneTime = timer.getDoneTimeStr();
		if (Config.verbose > 0) {
			System.out.println("Chord run initiated at: " + initTime);
			System.out.println("Chord run completed at: " + doneTime);
			System.out.println("Total time: " + timer.getInclusiveTimeStr());
		}
	}
}
