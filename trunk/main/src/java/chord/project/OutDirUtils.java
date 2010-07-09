/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;

import chord.util.IndexMap;
import chord.util.FileUtils;
import chord.util.ChordRuntimeException;
import chord.util.ProcessExecutor;

/**
 * Shorthand for common operations on files in the directory specified
 * by system property <tt>chord.out.dir</tt> to which Chord outputs
 * all files.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class OutDirUtils {
	private static final String PROCESS_STARTING = "Starting command: `%s`";
	private static final String PROCESS_FINISHED = "Finished command: `%s`";
	private static final String PROCESS_FAILED = "Command `%s` terminated abnormally: %s";

	private static final String outDirName = Properties.outDirName;
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
		String mainDirName = Properties.mainDirName;
		copyFile(mainDirName, fileName);
	}
	public static void writeMapToFile(IndexMap<String> map, String fileName) {
		FileUtils.writeMapToFile(map, new File(outDirName, fileName));
	}
	public static void runSaxon(String xmlFileName, String xslFileName) {
		String dummyFileName = (new File(outDirName, "dummy")).getAbsolutePath();
		xmlFileName = (new File(outDirName, xmlFileName)).getAbsolutePath();
		xslFileName = (new File(outDirName, xslFileName)).getAbsolutePath();
		try {
			net.sf.saxon.Transform.main(new String[] {
				"-o", dummyFileName, xmlFileName, xslFileName
			});
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
    public static final void executeWithFailOnError(String cmd) {
        Messages.log(PROCESS_STARTING, cmd);
        try {
            int result = ProcessExecutor.execute(cmd);
            if (result != 0)
                throw new ChordRuntimeException("Return value=" + result);
        } catch (Throwable ex) {
            Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
        }
        Messages.log(PROCESS_FINISHED, cmd);
    }
	public static final void executeWithWarnOnError(String cmd, int timeout) {
		Messages.log(PROCESS_STARTING, cmd);
		try {
			int result = ProcessExecutor.execute(cmd, timeout);
			if (result != 0) {
				Messages.log(PROCESS_FINISHED, cmd);
			}
		} catch (Throwable ex) {
			Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
		}
		Messages.log(PROCESS_FINISHED, cmd);
	}
}
