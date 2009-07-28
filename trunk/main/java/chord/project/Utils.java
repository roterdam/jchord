package chord.project;

import java.io.File;

import chord.util.Assertions;
import chord.util.FileUtils;

public class Utils {
	public static void copyFile(String fileName) {
		String homeDirName = Properties.homeDirName;
		Assertions.Assert(homeDirName != null);
		String outDirName = Properties.outDirName;
		Assertions.Assert(outDirName != null);
		File srcFile = new File(homeDirName, fileName);
		if (!srcFile.exists()) {
			throw new RuntimeException(
				"File named '" + fileName +
				"' does not exist under Chord's root directory '" +
				homeDirName + "'.");
		}
		File dstFile = new File(outDirName, srcFile.getName());
		FileUtils.copy(srcFile.getAbsolutePath(),
			dstFile.getAbsolutePath());
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
