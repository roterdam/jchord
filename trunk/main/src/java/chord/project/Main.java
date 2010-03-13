/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;
import java.io.PrintStream;

/**
 * Entry point of Chord.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Main {
	public static void main(String[] args) throws Exception {
        PrintStream outStream = null;
        PrintStream errStream = null;
		String outFileName = Properties.outFileName;
		String errFileName = Properties.errFileName;
		File outFile = new File(outFileName);
		outStream = new PrintStream(outFile);
		System.setOut(outStream);
		File errFile = new File(errFileName);
		if (errFile.equals(outFile))
			errStream = outStream;
		else
			errStream = new PrintStream(errFile);
		System.setErr(errStream);

		if (Properties.verboseLevel >= 1)
			Properties.print();
		Project.run();

		outStream.close();
		if (errStream != outStream)
			errStream.close();
	}
}
