package net.javacoding.jspider.core.util.html;

import net.javacoding.jspider.core.util.URLUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * $Id: URLFinder.java,v 1.9 2003/04/10 16:19:17 vanrogu Exp $
 */
public class URLFinder {

    public static final String basePattern = "<base href=";

    public static final String[] patterns = {
      "href=",
      "src=",
      "background="
    };

    public static void findURLs(URLFinderCallback callback, String line) {
        findBase(callback, line, basePattern);
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            findURLs(callback, line, pattern);
        }
    }

    protected static void findBase(URLFinderCallback callback, String line, String pattern) {
        String lineLowerCase = line.toLowerCase();
        int pos = lineLowerCase.indexOf(pattern);
        if ( pos != -1 ) {
            String url = "";
            try {
                url = extractURL(line, pos + pattern.length());
                URL baseURL = URLUtil.normalize(new URL(url));
                callback.setContextURL(baseURL);
            } catch (MalformedURLException e) {
                callback.malformedContextURLFound(url);
            }
        }
    }

    protected static void findURLs(URLFinderCallback callback, String line, String pattern) {
        String lineLowerCase = line.toLowerCase();
        int pos = lineLowerCase.indexOf(pattern);
        while (pos != -1) {
            String uri = "";
            try {
                uri = extractURL(line, pos + pattern.length());
                URL baseURL = callback.getContextURL();
                if ( ! URLUtil.isFileSpecified(baseURL)) {
                // Force a slash in case of a folder (to avoid buggy relative refs)
                   baseURL = new URL(baseURL.toString() + "/");
                }
                URL foundURL = URLUtil.normalize(new URL(baseURL, uri));
                callback.urlFound(foundURL);
            } catch (MalformedURLException e) {
                callback.malformedUrlFound(uri);
            }
            pos = lineLowerCase.indexOf(pattern, pos + pattern.length());
        }
    }

    protected static String extractURL(String string, int pos) {
        char c = string.charAt(pos);
        String ret = "";
        if (c == '\'' || c == '"') {
            string = string.substring(pos + 1);
        } else {
            string = string.substring(pos);
        }
        if (string.length() > 0) {
            c = string.charAt(0);
            if (c == '\'' || c == '\"' || c == '>') {
                ret = "";
            } else {
                StringTokenizer st = new StringTokenizer(string, " \"\'>");
                ret = st.nextToken();
            }
        }
        int p = ret.indexOf('#');
        if (p > -1) {
            return ret.substring(0, p);
        } else {
            return ret;
        }
    }

}
