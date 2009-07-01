/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;
import java.io.PrintStream;

import chord.util.Assertions;
import chord.util.FileUtils;

/**
 * Chord's main class.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Main {
	public static void main(String[] a) {
		PrintStream outStream = null;
		PrintStream errStream = null;
		try {
			String outDirName = Properties.outDirName;
			Assertions.Assert(outDirName != null);
			String outFileName = Properties.outFileName;
			Assertions.Assert(outFileName != null);
			String errFileName = Properties.outFileName;
			Assertions.Assert(errFileName != null);

			outFileName = FileUtils.getAbsolutePath(outFileName, outDirName);
			errFileName = FileUtils.getAbsolutePath(errFileName, outDirName);
			File outFile = new File(outFileName);
			outStream = new PrintStream(outFile);
			System.setOut(outStream);
			File errFile = new File(errFileName);
			if (errFile.equals(outFile))
				errStream = outStream;
			else
				errStream = new PrintStream(errFile);
			System.setErr(errStream);

			Properties.print();
/*
			if (classPathName != null) {
				classPathName += File.pathSeparator +
					System.getProperty("sun.boot.class.path");
			}
*/

			Project.init();
			Program.init();

			String analyses = Properties.analyses;
			if (analyses != null) {
				String[] analysesAry = analyses.split(" |,|:|;");
				for (String name : analysesAry)
					Project.runTask(name);
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			if (outStream != null)
				outStream.close();
			if (errStream != null && errStream != outStream)
				errStream.close();
			System.exit(1);
		}
	}
}
