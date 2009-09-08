package net.javacoding.jspider.core.util;


/**
 * Utility class that allows for easy string manipulation.  This class will be
 * deprecated once the support for JDK 1.3 will no longer be given, as the
 * functionality delivered by this class is incorporated out-of-the box in
 * Java 1.4 (String class, replace methods, etc ...)
 *
 * $Id: StringUtil.java,v 1.3 2003/05/02 17:36:59 vanrogu Exp $
 *
 * @author Günther Van Roey ( gunther@javacoding.net )
 */
public class StringUtil {

    /**
     * Replaces the occurences of a certain pattern in a string with a replacement
     * String.
     * @param string the string to be inspected
     * @param pattern the string pattern to be replaced
     * @param replacement the string that should go where the pattern was
     * @return the string with the replacements done
     */
    public static String replace ( String string, String pattern, String replacement ) {
        String replaced = null;

        if (string == null) {
            replaced = null;
        } else if (pattern == null || pattern.length() == 0 ) {
            replaced = string;
        } else {

            StringBuffer sb = new StringBuffer();

            int lastIndex = 0;
            int index = string.indexOf(pattern);
            while (index >= 0) {
                sb.append(string.substring(lastIndex, index));
                sb.append(replacement);
                lastIndex = index + pattern.length();
                index = string.indexOf(pattern, lastIndex);
            }
            sb.append(string.substring(lastIndex));
            replaced = sb.toString();
        }
        return replaced;
    }

    /**
     * @todo add Junit tests for this one
     */
    public static String replace  ( String string, String pattern, String replacement, int start ) {
        String begin = string.substring(0, start);
        String end = string.substring(start);
        return begin + replace(end, pattern, replacement );
    }
}
