import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

import java.util.Set;
import java.util.HashSet;

// usage: -[eq|sub] file1 file2
// returns: 0 if eq/sub holds, 1 if it does not hold, and
// 2 if it crashes (e.g. file1 is not found)
public class FileCmp {
	public static void main(String[] args) {
		assert (args != null && args.length == 3);
		String fileName1 = args[1];
		String fileName2 = args[2];
		int retVal;
		if (args[0].equals("-eq")) {
			retVal = testEquals(fileName1, fileName2);
		} else {
			assert(args[0].equals("-sub"));
			retVal = testSubset(fileName1, fileName2);
		}
		System.exit(retVal);
	}
	private static int testEquals(String fileName1, String fileName2) {
		Set<String> lines1, lines2;
		try {
			lines1 = readFileToSet(fileName1);
			lines2 = readFileToSet(fileName2);
		} catch (IOException ex) {
			ex.printStackTrace();
			return 2;
		}
		boolean isFirst = true;
		for (String s : lines1) {
			if (!lines2.contains(s)) {
				if (isFirst) {
					System.out.println("ERROR: File " + fileName1 +
						" not equal to file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("< " + s);
			}
		}
		if (isFirst && lines2.size() == lines1.size())
			return 0;
		for (String s : lines2) {
			if (!lines1.contains(s)) {
				if (isFirst) {
					System.out.println("ERROR: File " + fileName1 +
						" not equal to file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("> " + s);
			}
		}
		return 1;
	}
	private static int testSubset(String fileName1, String fileName2) {
		Set<String> lines1, lines2;
		try {
			lines1 = readFileToSet(fileName1);
			lines2 = readFileToSet(fileName2);
		} catch (IOException ex) {
			ex.printStackTrace();
			return 2;
		}
		boolean isFirst = true;
		for (String s : lines1) {
			if (!lines2.contains(s)) {
				if (isFirst) {
					System.out.println("ERROR: File " + fileName1 +
						" not a subset of file " + fileName2 + ":");
					isFirst = false;
				}
				System.out.println("< " + s);
			}
		}
		return (isFirst) ? 0 : 1;
	}
	private static Set<String> readFileToSet(String fileName) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		Set<String> set = new HashSet<String>();
		String s;
		while ((s = in.readLine()) != null) {
			set.add(s);
		}
		in.close();
		return set;
	}
}
