package net.javacoding.jspider.core.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Some URL related methods gathered as static methods in this utility class.
 *
 * $Id: URLUtil.java,v 1.13 2003/04/29 17:53:49 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class URLUtil {

    /**
     * Normalizes the given url by replacing '/./' by '/' and removes trailing slashes
     * @param original the original URL to be normalized
     * @return the normalized url
     */
    public static URL normalize(URL original) {
        URL normalized = null;

        if (original != null) {

            String urlString = original.toString ( );

            urlString = normalizeDotFolders(urlString);
            urlString = normalizeBackSlashes(urlString);
            urlString = normalizeDoubleSlashes(urlString);
            urlString = normalizeStripQuery(urlString) ;
            //urlString = normalizeStripTrailingSlash(urlString) ;

            try {
                normalized = new URL(urlString);
            } catch (MalformedURLException e) {
            }
        }
        return normalized;
    }

    /**
     * Replaces all backslashes by front slashes in the given url string
     * @param original the original url string
     * @return the url string with the normalization applied
     */
    protected static String normalizeBackSlashes ( String original ) {
        return StringUtil.replace(original, "\\", "/");
    }

    /**
     * Replaces all double slashes by single slashes in the given url string
     * @param original the original url string
     * @return the url string with the normalization applied
     */
    protected static String normalizeDoubleSlashes ( String original ) {
        return StringUtil.replace(original, "//", "/", 7);
    }

    /**
     * Removes all dot folders ( abc/./def/index.html, ...) from the given
     * url string
     * @param original the original url string
     * @return the url string with the normalization applied
     */
    protected static String normalizeDotFolders ( String original ) {
        return StringUtil.replace(original, "/./", "/");
    }

    /**
     * Strips an eventual query string from the resource (index.html?id=1
     * becomes index.html for instance).
     * @param original the original url string
     * @return the url string with the normalization applied
     */
    protected static String normalizeStripQuery ( String original ) {
        int index = original.indexOf('?');
        if (index >= 0) {
            return original.substring(0, index);
        } else {
            return original;
        }
    }

    /**
     * Removes an evantual trailing slash from the given url string
     * @param original the original url string
     * @return the url string with the normalization applied
     */
    protected static String normalizeStripTrailingSlash ( String original ) {
        if (original.endsWith("/")) {
            return original.substring(0, original.length() - 1);
        } else {
            return original;
        }
    }

    /**
     * Converts any resource URL to the site's url.
     * @param resourceURL the url of the resource to find the url of the site for
     * @return the URL pointing to the site in which the resource is located
     */
    public static URL getSiteURL(URL resourceURL) {
        URL siteURL = null;
        if (resourceURL != null) {
            try {
                siteURL = new URL(resourceURL.getProtocol(), resourceURL.getHost(), resourceURL.getPort(), "");
            } catch (MalformedURLException e) {
                // shouldn't happen, we're only dropping the PATH part of a valid URL ...
            }
        }
        return siteURL;
    }

    /**
     * Reuturns the URL of the robots.txt resource in the site of the given resource.
     * @param resourceURL the URL of the resource to find the site's robots.txt of
     * @return URL pointing to the robots.txt resource of the site in which resourceURL is
     */
    public static URL getRobotsTXTURL(URL resourceURL) {
        URL retVal = null;
        if (resourceURL != null) {
            try {
                retVal = new URL(getSiteURL(resourceURL), "/robots.txt");
            } catch (MalformedURLException e) {
            }
        }
        return retVal;
    }

    /**
     * returns the resource path without the resource.
     * @param path the path to the resource
     * @return path without the resource itself
     */
    public static String stripResource(String path) {
        String result = null;
        if (path != null) {
            int pos = path.lastIndexOf("/");
            result = path.substring(0, pos + 1);
        }
        return result;
    }

    /**
     * Returns the 'depth' of the resource pointed to by the URL
     * @param url the URL to the resource to calculate the depth of
     * @return the depth of this resource in the site
     */
    public static int getDepth(URL url) {
        int depth = 0;

        if (url != null) {
            String path = url.getPath();
            if (!isFileSpecified(url) && !path.endsWith("/")) {
                path = path + "/";
            }
            int pos = path.indexOf('/');
            while (pos != -1) {
                if (pos > 0) {
                    depth++;
                }
                pos = path.indexOf('/', pos + 1);
            }
        }
        return depth;
    }

    /**
     * Determines whether a file is specified in the path part of the url.
     * This is assumed to be the case if the string after the last slash
     * contains a dot (aaaaa/bbbb/cccc.dddd).
     * @param url the url to test
     * @return boolean value indicating whether a file is specified
     */
    public static boolean isFileSpecified(URL url) {
        boolean specified = false;

        String path = url.getPath();
        int posLastSlash = path.lastIndexOf('/');
        int posLastDot = path.lastIndexOf('.');

        specified = posLastDot > posLastSlash;

        return specified;
    }

    /**
     * Returns an array of Strings being the folder names of the folders
     * found in the given URL.
     * @param url the url to parse the folders of
     * @return an array of Strings containing all folder names
     */
    public static String[] getFolderNames(URL url) {
        url = normalize(url);
        ArrayList al = new ArrayList();

        String path = url.getPath();
        if (isFileSpecified(url)) {
            path = stripResource(path);
        }
        StringTokenizer st = new StringTokenizer(path, "/");

        while (st.hasMoreTokens()) {
            al.add(st.nextToken());
        }
        return (String[]) al.toArray(new String[al.size()]);
    }

    /**
     * Returns the file name (without the path) of the resource specified
     * by the given url.
     * @param url the url to get the filename out of
     * @return String containing the name of the file, zero-length if none
     */
    public static String getFileName(URL url) {
        return url.getPath().substring(stripResource(url.getPath()).length());
    }

}
