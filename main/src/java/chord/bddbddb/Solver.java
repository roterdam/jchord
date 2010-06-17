/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.bddbddb;

import chord.project.Properties;
import chord.project.OutDirUtils;

/**
 * Interface to bddbddb's BDD-based Datalog solver.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Solver {
	/**
	 * Runs bddbddb's BDD-based Datalog solver on the specified
	 * Datalog program.
	 * <p>
	 * The maximum amount of memory available to the solver at
	 * run-time can be specified by the user via system property
	 * <tt>bddbddb.max.heap.size</tt> (default is 1024m).
	 * 
	 * @param	fileName	A file containing a Datalog program.
	 */
	public static void run(String fileName) {
		String cmd = "java -Xmx" + Properties.bddbddbMaxHeap +
			" -cp " + Properties.bddbddbClassPathName +
			" -Dnoisy=" + (Properties.bddbddbVerbose ? "yes" : "no") +
			" -Djava.library.path=" + Properties.libDirName +
			" -Dbasedir=" + Properties.bddbddbWorkDirName +
			" net.sf.bddbddb.Solver " + fileName;
		OutDirUtils.executeWithFailOnError(cmd);
	}
}
