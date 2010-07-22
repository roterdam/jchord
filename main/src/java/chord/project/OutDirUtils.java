/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

import chord.util.IndexMap;
import chord.util.FileUtils;
import chord.util.ChordRuntimeException;
import chord.util.ProcessExecutor;

/**
 * Common operations on files in the directory specified by system property
 * <tt>chord.out.dir</tt> to which Chord outputs all files.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class OutDirUtils {
	private static final String PROCESS_STARTING = "Starting command: `%s`";
	private static final String PROCESS_FINISHED = "Finished command: `%s`";
	private static final String PROCESS_FAILED = "Command `%s` terminated abnormally: %s";

	private static final String outDirName = Config.outDirName;
	public static PrintWriter newPrintWriter(String fileName) {
		try {
			return new PrintWriter(new File(outDirName, fileName));
		} catch (FileNotFoundException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void copyFile(String baseDirName, String fileName) {
		File srcFile = new File(baseDirName, fileName);
		if (!srcFile.exists()) {
			throw new ChordRuntimeException("File '" + fileName +
				"' does not exist in dir: " + baseDirName);
		}
		File dstFile = new File(outDirName, srcFile.getName());
		FileUtils.copy(srcFile.getAbsolutePath(),
			dstFile.getAbsolutePath());
	}
	public static void copyFileFromMainDir(String fileName) {
		String mainDirName = Config.mainDirName;
		copyFile(mainDirName, fileName);
	}
	public static void writeMapToFile(IndexMap<String> map, String fileName) {
		FileUtils.writeMapToFile(map, new File(outDirName, fileName));
	}
	public static void runSaxon(String xmlFileName, String xslFileName) {
		String dummyFileName = (new File(outDirName, "dummy")).getAbsolutePath();
		xmlFileName = (new File(outDirName, xmlFileName)).getAbsolutePath();
		xslFileName = (new File(outDirName, xslFileName)).getAbsolutePath();
		String[] cmdarray = { "java",
			"-cp", Config.libDirName + File.separator + "saxon9.jar",
			"net.sf.saxon.Transform",
			"-o", dummyFileName, xmlFileName, xslFileName
		};
		executeWithFailOnError(cmdarray);
/*
		// commented out to reduce footprint of chord.main.class.path
		// (i.e. to enable excluding saxon9.jar from it)
		net.sf.saxon.Transform.main(new String[] {
			"-o", dummyFileName, xmlFileName, xslFileName
		});
*/
	}
    public static final void executeWithFailOnError(List<String> cmdlist) {
		String[] cmdarray = new String[cmdlist.size()];
		executeWithFailOnError(cmdlist.toArray(cmdarray));
	}
    public static final void executeWithFailOnError(String[] cmdarray) {
		String cmd = "";
		for (String s : cmdarray)
			cmd += s + " ";
        Messages.log(PROCESS_STARTING, cmd);
        try {
            int result = ProcessExecutor.execute(cmdarray);
            if (result != 0)
                throw new ChordRuntimeException("Return value=" + result);
        } catch (Throwable ex) {
            Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
        }
        Messages.log(PROCESS_FINISHED, cmd);
    }
	public static final void executeWithWarnOnError(List<String> cmdlist, int timeout) {
		String[] cmdarray = new String[cmdlist.size()];
		executeWithWarnOnError(cmdlist.toArray(cmdarray), timeout);
	}
	public static final void executeWithWarnOnError(String[] cmdarray, int timeout) {
		String cmd = "";
		for (String s : cmdarray)
			cmd += s + " ";
		Messages.log(PROCESS_STARTING, cmd);
		try {
			int result = ProcessExecutor.execute(cmdarray, timeout);
			if (result != 0) {
				Messages.log(PROCESS_FINISHED, cmd);
			}
		} catch (Throwable ex) {
			Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
		}
		Messages.log(PROCESS_FINISHED, cmd);
	}
}
