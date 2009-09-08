package net.javacoding.jspider.core.util;

import junit.framework.TestCase;

import java.net.URL;

/**
 * $Id: URLUtilTest.java,v 1.10 2003/04/29 17:53:49 vanrogu Exp $
 */
public class URLUtilTest extends TestCase {

    public URLUtilTest ( ) {
        super ( "URLUtilTest" );
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNormalizeNullURL ( ) throws Exception {
        URL normalized = URLUtil.normalize(null);
        assertNull ( "null URL normalization didn't return null", normalized);
    }

    public void testNormalizeSimpleURL ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net");
        URL expected = original;
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "simple URL normalization didn't return the same url", equals);
    }

    public void testNormalizeURLWithBackSlashes ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/folder\\subfolder\\test/index.html");
        URL expected = new URL("http://j-spider.sourceforge.net/folder/subfolder/test/index.html");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "simple URL normalization didn't return the same url", equals);
    }

    public void testNormalizeURLWithParams ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/index.html?param1=value1&param2=value2");
        URL expected = new URL("http://j-spider.sourceforge.net/index.html");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "simple URL normalization didn't return the same url", equals);
    }

    public void testNormalizeURLWithFolderAndParams ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/manual?param1=value1&param2=value2");
        URL expected = new URL("http://j-spider.sourceforge.net/manual");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "simple URL normalization didn't return the same url", equals);
    }

    public void testNormalizeURLWithFolderAndTrailingSlashAndParams ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/manual/?param1=value1&param2=value2");
        URL expected = new URL("http://j-spider.sourceforge.net/manual/");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "simple URL normalization didn't return the same url", equals);
    }

    public void testNormalizeSingleDotFolder ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/folder/./subfolder");
        URL expected = new URL("http://j-spider.sourceforge.net/folder/subfolder");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "URL with single dot folder normalization failed", equals);
    }

    public void testNormalizeMultipleDotFolder ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/./folder/./subfolder/./a");
        URL expected = new URL("http://j-spider.sourceforge.net/folder/subfolder/a");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "URL with single dot folder normalization failed", equals);
    }

    public void testNormalizeTrailingSlash ( ) throws Exception {
        URL original = new URL("http://j-spider.sourceforge.net/");
        URL expected = new URL("http://j-spider.sourceforge.net/");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "URL with trailing slash did not normalize well", equals);
    }

    public void testNormalizeCombinedFunctionality ( ) throws Exception {
        // multiple dot folders and a trailing slash !
        URL original = new URL("http://j-spider.sourceforge.net/./folder/./subfolder/./");
        URL expected = new URL("http://j-spider.sourceforge.net/folder/subfolder/");
        URL normalized = URLUtil.normalize(original);

        boolean equals = normalized.equals(expected);

        assertTrue ( "URL with many flows normalize failed", equals);
    }

    public void testSiteURLFromSimpleURL ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/index.html" );
        URL expected = new URL ( "http://j-spider.sourceforge.net" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "simple resource url failed to resolve to site url", equals );
    }


    public void testSiteURLFromNullURL ( ) throws Exception {
        URL actual = URLUtil.getSiteURL(null);
        assertNull ( "null resource url failed to resolve to null site url", actual );
    }


    public void testSiteURLFromSiteURL ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net" );
        URL expected = new URL ( "http://j-spider.sourceforge.net" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "site url failed to resolve to itself as site url", equals );
    }

    public void testSiteURLFromSiteURLWithTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/" );
        URL expected = new URL ( "http://j-spider.sourceforge.net" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "site url failed to resolve to itself as site url", equals );
    }

    public void testSiteURLFromSimpleURLWithPort ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123/index.html" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "simple resource url with port failed to resolve to site url", equals );
    }

    public void testSiteURLFromSiteURLWithPort ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "simple resource url with port failed to resolve to site url", equals );
    }

    public void testSiteURLFromSiteURLWithPortAndTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123/" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "simple resource url with port failed to resolve to site url", equals );
    }

    public void testSiteURLFromFtpURLWithPortAndTrailingSlash ( ) throws Exception {
        URL url = new URL ( "ftp://j-spider.sourceforge.net:123/folder/" );
        URL expected = new URL ( "ftp://j-spider.sourceforge.net:123" );
        URL actual = URLUtil.getSiteURL(url);

        boolean equals = actual.equals ( expected );
        assertTrue ( "ftp resource url with port failed to resolve to site url", equals );
    }

    public void testGetRobotsTXTUrlSimple ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net" );
        URL expected = new URL ( "http://j-spider.sourceforge.net/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testGetRobotsTXTUrlFromNullURL ( ) throws Exception {
        URL actual = URLUtil.getRobotsTXTURL(null);
        assertNull("getRobotsTXTURL (null url) failed", actual);
    }

    public void testGetRobotsTXTUrlTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/" );
        URL expected = new URL ( "http://j-spider.sourceforge.net/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testGetRobotsTXTUrlWithPort ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testGetRobotsTXTUrlWithPortAndTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123/" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testGetRobotsTXTUrlWithFolders ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/folder/folder/resource.html" );
        URL expected = new URL ( "http://j-spider.sourceforge.net/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testGetRobotsTXTUrlWithPortAndFolders ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net:123/folder/folder/resource.html" );
        URL expected = new URL ( "http://j-spider.sourceforge.net:123/robots.txt" );
        URL actual = URLUtil.getRobotsTXTURL(url);
        boolean equals = expected.equals(actual);
        assertTrue("getRobotsTXTURL failed", equals);
    }

    public void testStripResourceNullResource ( ) {
        String path = null;
        String result = URLUtil.stripResource(path);
        String expected = null;
        boolean equals = result == expected;
        assertTrue ( "stripResource failed", equals );
    }

    public void testStripResourceSimple ( ) {
        String path = "/folder/subfolder/resource.html";
        String result = URLUtil.stripResource(path);
        String expected = "/folder/subfolder/";
        boolean equals = expected.equals(result);
        assertTrue ( "stripResource failed", equals );
    }

    public void testStripResourceTrailingSlash ( ) {
        String path = "/folder/subfolder/";
        String result = URLUtil.stripResource(path);
        String expected = "/folder/subfolder/";
        boolean equals = expected.equals(result);
        assertTrue ( "stripResource failed", equals );
    }

    public void testStripResourceOnlySlash ( ) {
        String path = "/";
        String result = URLUtil.stripResource(path);
        String expected = "/";
        boolean equals = expected.equals(result);
        assertTrue ( "stripResource failed", equals );
    }

    public void testStripResourceOnlyFolder ( ) {
        String path = "/folder";
        String result = URLUtil.stripResource(path);
        String expected = "/";
        boolean equals = expected.equals(result);
        assertTrue ( "stripResource failed", equals );
    }

    public void testDepthSimple ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/test/index.html");
        int expected = 1;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthSimpleDeeper ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/test/test2/test3/index.html");
        int expected = 3;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthFileOnRoot ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/index.html");
        int expected = 0;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthRoot ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net");
        int expected = 0;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthRootTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/");
        int expected = 0;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthNullURL ( ) throws Exception {
        URL url = null;
        int expected = 0;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthNoFile ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/test");
        int expected = 1;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testDepthNoFileTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/test/");
        int expected = 1;
        int actual = URLUtil.getDepth(url);
        assertEquals( "depth calculation failed", expected, actual );
    }

    public void testIfFileSpecifiedSimple ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index.html");
        boolean expected = true;
        boolean actual = URLUtil.isFileSpecified(url);
        assertEquals("isFileSpecified took wrong decision", expected, actual );
    }

    public void testIfFileSpecifiedFolder ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index");
        boolean expected = false;
        boolean actual = URLUtil.isFileSpecified(url);
        assertEquals("isFileSpecified took wrong decision", expected, actual );
    }

    public void testIfFileSpecifiedFolderTrailingSlash ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index/");
        boolean expected = false;
        boolean actual = URLUtil.isFileSpecified(url);
        assertEquals("isFileSpecified took wrong decision", expected, actual );
    }

    public void testIfFileSpecifiedFolderAndFolderWithDot ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index.ext/index");
        boolean expected = false;
        boolean actual = URLUtil.isFileSpecified(url);
        assertEquals("isFileSpecified took wrong decision", expected, actual );
    }

    public void testGetFolderNamesSimple ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/folder/subfolder/index.html");
        String[] expected = new String[]{"test", "folder", "subfolder"};
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesURLWithoutFile ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/folder/subfolder");
        String[] expected = new String[]{"test", "folder", "subfolder"};
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesURLWithoutFileTrailingSlash ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/folder/subfolder/");
        String[] expected = new String[]{"test", "folder", "subfolder"};
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesOnRoot ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net");
        String[] expected = new String[0];
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesOnRootTrailingSlash ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/");
        String[] expected = new String[0];
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesFileOnRoot ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/index.html");
        String[] expected = new String[0];
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFolderNamesFolderOnRoot ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test");
        String[] expected = new String[]{"test"};
        String[] actual = URLUtil.getFolderNames(url) ;

        assertEquals("wrong number of folderNames returned", expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            String s = actual[i];
            assertEquals("folderName " + i + " is wrong", expected[i], s );
        }
    }

    public void testGetFileNameSimple ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test.html");
        String expected = "test.html";
        String actual = URLUtil.getFileName(url) ;
        assertEquals("returned filename is wrong", expected, actual);
    }

    public void testGetFileNameInSubfolders ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index/test.html");
        String expected = "test.html";
        String actual = URLUtil.getFileName(url) ;
        assertEquals("returned filename is wrong", expected, actual);
    }

    public void testGetFileNameInTrickySubfolders ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index.folder/test.html");
        String expected = "test.html";
        String actual = URLUtil.getFileName(url) ;
        assertEquals("returned filename is wrong", expected, actual);
    }

    public void testGetFileNameNoFile ( ) throws Exception {
        URL url = new URL("http://j-spider.sourceforge.net/test/index.folder/");
        String expected = "";
        String actual = URLUtil.getFileName(url) ;
        assertEquals("returned filename is wrong", expected, actual);
    }

}
