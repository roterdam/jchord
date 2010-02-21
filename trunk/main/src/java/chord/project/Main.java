package chord.project;

import java.io.File;
import java.io.PrintStream;

import chord.util.ChordRuntimeException;

public class Main {
	public static void main(String[] args) {
        PrintStream outStream = null;
        PrintStream errStream = null;
        try {
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

            Project.run();

            outStream.close();
			if (errStream != outStream)
				errStream.close();
        } catch (Throwable ex) {
			throw new ChordRuntimeException(ex);
		}
	}
}
