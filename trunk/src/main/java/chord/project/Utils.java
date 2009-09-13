/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.File;

import chord.util.FileUtils;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Utils {
	public static void copyFile(String baseDirName, String fileName) {
		String outDirName = Properties.outDirName;
		assert (outDirName != null);
		File srcFile = new File(baseDirName, fileName);
		if (!srcFile.exists()) {
			throw new RuntimeException("File '" + fileName +
				"' does not exist in dir: " + baseDirName);
		}
		File dstFile = new File(outDirName, srcFile.getName());
		FileUtils.copy(srcFile.getAbsolutePath(),
			dstFile.getAbsolutePath());
	}
	public static void copyFile(String fileName) {
		String homeDirName = Properties.homeDirName;
		assert (homeDirName != null);
		copyFile(homeDirName, fileName);
	}
	public static void runSaxon(String xmlFileName, String xslFileName) {
		String outDirName = Properties.outDirName;
		String dummyFileName = (new File(outDirName, "dummy")).getAbsolutePath();
		xmlFileName = (new File(outDirName, xmlFileName)).getAbsolutePath();
		xslFileName = (new File(outDirName, xslFileName)).getAbsolutePath();
		try {
			net.sf.saxon.Transform.main(new String[] {
				"-o", dummyFileName, xmlFileName, xslFileName
			});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
