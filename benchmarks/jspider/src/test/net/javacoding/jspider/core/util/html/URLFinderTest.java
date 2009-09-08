package net.javacoding.jspider.core.util.html;

import junit.framework.TestCase;

import java.net.URL;

/**
 * $Id: URLFinderTest.java,v 1.6 2003/04/29 17:53:50 vanrogu Exp $
 */
public class URLFinderTest extends TestCase implements URLFinderCallback {

    protected int count;
    protected int malformed;
    protected int malformedContextURL;
    protected URL baseURL;
    protected URL lastURL;
    protected URL contextURL;

    public URLFinderTest ( ) {
        super ( "URLFinderTest" );
    }

    protected void setUp() throws Exception {
        count = 0;
        malformed = 0;
        malformedContextURL = 0;
        baseURL = new URL("http://j-spider.sourceforge.net");
        contextURL = baseURL;
    }

    public void urlFound(URL foundURL) {
        count++;
        lastURL = foundURL;
    }

    public void malformedUrlFound(String malformedURL) {
        malformed++;
    }

    public URL getContextURL() {
        return contextURL;
    }

    public void setContextURL(URL url) {
        this.contextURL = url;
    }

    public void malformedContextURLFound(String malformedURL) {
        malformedContextURL++;
    }

    public void testStringWithNoURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with no url in it");
        int expected = 0;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringWithBaseButNoURL ( ) throws Exception {
        URLFinder.findURLs(this,  "<BaSe HrEf='http://www.somehost.com'> this is a line with no url in it, expect for the base one");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
        assertEquals("contextURL error", "http://www.somehost.com", contextURL.toString() );
        assertEquals("lastURL error", "http://www.somehost.com", lastURL.toString() );
    }

    public void testStringWithBaseButNoURLCaseSensitivity ( ) throws Exception {
        URLFinder.findURLs(this,  "<BaSe HrEf='http://www.SomeHost.com'> this is a line with no url in it, expect for the base one");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
        assertEquals("contextURL error", "http://www.SomeHost.com", contextURL.toString() );
        assertEquals("lastURL error", "http://www.SomeHost.com", lastURL.toString() );
    }

    public void testStringWithBaseInFolderButNoURL ( ) throws Exception {
        URLFinder.findURLs(this,  "<base href='http://www.somehost.com/folder/subfolder'> this is a line with no url in it, expect for the base one");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
        assertEquals("contextURL error", "http://www.somehost.com/folder/subfolder", contextURL.toString() );
        assertEquals("lastURL error", "http://www.somehost.com/folder/subfolder", lastURL.toString() );
    }

    public void testStringWithNoURLNullBaseURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with no url in it");
        int expected = 0;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringWithOneURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='index.html'>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringWithOneURLNullBaseURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='http://index.html'>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringWithOneURLAndBaseURLWithFile ( ) throws Exception {
        URLFinder.findURLs(this,  "this <base href='http://www.somehost.com/folder/subfolder/test.html'> is a line with a <a href='index.html'>url</a> in it");
        int expected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("wrong contextURL reported", "http://www.somehost.com/folder/subfolder/test.html", contextURL.toString());
        assertEquals("wrong last URLs reported","http://www.somehost.com/folder/subfolder/index.html", this.lastURL.toString());
    }

    public void testStringWithOneURLAndBaseURLWithFolder ( ) throws Exception {
        URLFinder.findURLs(this,  "this <base href='http://www.somehost.com/folder/subfolder'> is a line with a <a href='index.html'>url</a> in it");
        int expected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("wrong contextURL reported", "http://www.somehost.com/folder/subfolder", contextURL.toString());
        assertEquals("wrong last URLs reported","http://www.somehost.com/folder/subfolder/index.html", this.lastURL.toString());
    }

    public void testStringWithOneURLAndBaseURLWithFolderAndSlash ( ) throws Exception {
        URLFinder.findURLs(this,  "this <base href='http://www.somehost.com/folder/subfolder/'> is a line with a <a href='index.html'>url</a> in it");
        int expected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("wrong contextURL reported", "http://www.somehost.com/folder/subfolder/", contextURL.toString());
        assertEquals("wrong last URLs reported","http://www.somehost.com/folder/subfolder/index.html", this.lastURL.toString());
    }

    public void testStringWithTwoURLs ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='index.html'>url</a> in it, and a <a href='second.html'>second one</a> also");
        int expected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/second.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringWithTwoURLsNullBaseURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='http://index.html'>url</a> in it, and a  <a href='http://second.html'>second one</a> also");
        int expected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringSingleQuote ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='index.html'>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringDoubleQuote ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=\"index.html\">url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringDoubleNoQuotes ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=index.html>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted1 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=\"index.html>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted2 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='index.html>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted3 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=index.html'>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted4 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=index.html\">url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted5 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href='index.html\">url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testStringBadlyQuoted6 ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=\"index.html'>url</a> in it");
        int expected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("url found wasn't the one expected", new URL("http://j-spider.sourceforge.net/index.html"), lastURL);
        assertEquals("malformed URLs reported", 0, malformed);
    }

    public void testMalformed ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with a <a href=\"someprotocol:index.html'>url</a> in it");
        int expected = 0;
        int malformedExpected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("number of malformed URLs reported differs from expected", malformedExpected, malformed);
    }

    public void testTwoMalformed ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with two malformed <a href=\"someprotocol:index.html'>urls</a><a href='test:'/> in it");
        int expected = 0;
        int malformedExpected = 2;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("number of malformed URLs reported differs from expected", malformedExpected, malformed);
    }

    public void testWellformedAndMalformed ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line with one malformed <a href=\"someprotocol:index.html'>urls</a><a href='index.html'/> in it, and one wellformed");
        int expected = 1;
        int malformedExpected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("number of malformed URLs reported differs from expected", malformedExpected, malformed);
    }

    public void testWellformedAndMalformedWithBaseURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line <base href='http://www.somehost.com/folder/test.html'/>with one malformed <a href=\"someprotocol:index.html'>urls</a><a href='index.html'/> in it, and one wellformed");
        int expected = 2;
        int malformedExpected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("number of malformed URLs reported differs from expected", malformedExpected, malformed);
        assertEquals("baseURL is wrong", "http://www.somehost.com/folder/test.html", contextURL.toString());
        assertEquals("lastURL is wrong", "http://www.somehost.com/folder/index.html", lastURL.toString());
    }

    public void testWithBaseURL ( ) throws Exception {
        URLFinder.findURLs(this,  "this is a line <base href='http://www.somehost.com/folder/subfolder/test.html'/>with one malformed <a href=\"someprotocol:index.html'>urls</a><a href='../index.html'/> in it, and one wellformed");
        int expected = 2;
        int malformedExpected = 1;
        assertEquals("actual nr of urls found differs from expected", expected, count);
        assertEquals("number of malformed URLs reported differs from expected", malformedExpected, malformed);
        assertEquals("baseURL is wrong", "http://www.somehost.com/folder/subfolder/test.html", contextURL.toString());
        assertEquals("lastURL is wrong", "http://www.somehost.com/folder/index.html", lastURL.toString());
    }

}
