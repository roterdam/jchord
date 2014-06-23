package chord.util;

import java.io.File;

/** String utilities.
 * Say {@code import static chord.Util.StringUtil.*;} */
public final class StringUtil {
	static public String path(String ... xs) {
		boolean first = true;
		StringBuilder b = new StringBuilder();
		for (String x : xs) {
			if (!first) b.append(File.separator); else first = false;
			b.append(x);
		}
		return b.toString();
	}

	private StringUtil() {}
}

