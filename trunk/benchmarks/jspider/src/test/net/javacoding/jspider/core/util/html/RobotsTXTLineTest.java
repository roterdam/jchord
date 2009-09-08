package net.javacoding.jspider.core.util.html;

import junit.framework.TestCase;

import java.net.URL;

/**
 * $Id: RobotsTXTLineTest.java,v 1.1 2003/02/11 17:33:04 vanrogu Exp $
 */
public class RobotsTXTLineTest extends TestCase {

    public RobotsTXTLineTest ( ) {
        super ( "RobotsTXTLineTest" );
    }

    public void testSimpleAllow ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("allow: index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_ALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testSimpleAllowCaseSensitivity ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("AlLoW: index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_ALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testSimpleDisallow ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("disallow: index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testSimpleDisallowCaseSensitivity ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("dIsAlLoW: index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testSimpleAllowWithoutSpace ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("allow:index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_ALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testSimpleDisallowWithoutSpace ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("disallow:index.html");

        assertNotNull("line returned for correct info is null", l);

        String resource = l.getResourceURI();
        int type = l.getType ( );

        assertEquals("type parsed incorrect", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW,  type);
        assertEquals("resourceURI parsed incorrect", "index.html", resource);

    }

    public void testErroneousAllowDisallow ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("alow:index.html");
        assertNull("line returned for incorrect info is not null", l);
    }

    public void testEmptyString ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("");
        assertNull("line returned for empty string is not null", l);
    }

    public void testOnlyAllow ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("allow:");
        assertNull("line returned for 'allow:' string is not null", l);
    }

    public void testOnlyDisAllow ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse("disallow:");
        assertNull("line returned for 'disallow:' string is not null", l);
    }

    public void testNullString ( ) {
        RobotsTXTLine l = RobotsTXTLine.parse(null);
        assertNull("line returned for null string is not null", l);
    }

    public void testSimpleMatch ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/index.html" );
        RobotsTXTLine line = new RobotsTXTLine("/index.html", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertTrue ( "simple match didn't work", matches);
    }

    public void testSimpleMatchWithFolder ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/manual/index.html" );
        RobotsTXTLine line = new RobotsTXTLine("/manual", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertTrue ( "simple match didn't work", matches);
    }

    public void testSimpleMatchCaseSensitivity ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/index.HTML" );
        RobotsTXTLine line = new RobotsTXTLine("/index.html", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertFalse ( "cases were different, yet a match was given", matches);
    }

    public void testSimpleNoMatch ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/index.htm" );
        RobotsTXTLine line = new RobotsTXTLine("/index.html", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertFalse ( "simple nomatch didn't work", matches);
    }

    public void testRootMatch ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net" );
        RobotsTXTLine line = new RobotsTXTLine("/", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertTrue ( "root match didn't work", matches);
    }

    public void testRootMatchWithTrailingSlash ( ) throws Exception {
        URL url = new URL ( "http://j-spider.sourceforge.net/" );
        RobotsTXTLine line = new RobotsTXTLine("/", RobotsTXTLine.ROBOTSTXT_RULE_DISALLOW);
        boolean matches = line.matches(url);

        assertTrue ( "root match didn't work", matches);
    }

}
