package net.javacoding.jspider.core.util.html;

import junit.framework.TestCase;

import java.io.*;

/**
 * $Id: RobotsTXTLineSetTest.java,v 1.4 2003/04/29 17:53:50 vanrogu Exp $
 */
public class RobotsTXTLineSetTest extends TestCase {

    public static final String TEST1 =
            "user-agent: *" + "\n" +
            "" + "\n" +
            "disallow: /index.html" +"\n" +
            "allow: /test" + "\n";

    public static final String TEST2 =
            "user-agent: testAgent" + "\n" +
            "" + "\n" +
            "allow: /index.html" +"\n" +
            "allow: /test" + "\n" +
            "" + "\n" +
            "user-agent: test" + "\n" +
            "" + "\n" +
            "allow: /index.html" +"\n" +
            "disallow: /test" + "\n" +
            "" + "\n" +
            "user-agent: *" + "\n" +
            "" + "\n" +
            "disallow: /index.html" +"\n" +
            "disallow: /test" + "\n" +
            "" + "\n";

    public static final String TEST3 =
            "user-agent: testAgent" + "\n" +
            "" + "\n" +
            "allow: /index.html" +"\n" +
            "allow: /test" + "\n" +
            "" + "\n" +
            "user-agent: test" + "\n" +
            "" + "\n" +
            "allow: /index.html" +"\n" +
            "disallow: /test" + "\n" +
            "" + "\n" +
            "user-agent: someAgent" + "\n" +
            "" + "\n" +
            "disallow: /index.html" +"\n" +
            "disallow: /test" + "\n" +
            "" + "\n";

    public static final String TEST4 ="";

    public static final String TEST5 =
            "#user-agent: testAgent" + "\n" +
            "" + "\n" +
            "#allow: /index.html" +"\n" +
            "#allow: /test" + "\n" +
            "" + "\n" +
            "#user-agent: test" + "\n" +
            "" + "\n" +
            "#allow: /index.html" +"\n" +
            "#disallow: /test" + "\n" +
            "" + "\n" +
            "#user-agent: someAgent" + "\n" +
            "" + "\n" +
            "#disallow: /index.html" +"\n" +
            "#disallow: /test" + "\n" +
            "" + "\n";

    public RobotsTXTLineSetTest ( ) {
        super ( "RobotsTXTLineSetTest" );
    }

    protected BufferedReader getReader(String string) {
        return new BufferedReader(new StringReader(string));
    }

    protected InputStream getInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes());
    }

    public void testSimpleRobotsTXT ( ) throws Exception {
        BufferedReader br = getReader(TEST1);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "testUserAgent");

        assertEquals("lineset didn't contain exactly the one disallowing rule", 1, lineset.getLines().length);
        assertEquals("expected to obey user agent '*'", "*", lineset.getUserAgent() );
    }

    public void testSimpleRobotsTXTViaInputStream ( ) throws Exception {
        InputStream is = getInputStream(TEST1);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(is, "testUserAgent");

        assertEquals("lineset didn't contain exactly the one disallowing rule", 1, lineset.getLines().length);
        assertEquals("expected to obey user agent '*'", "*", lineset.getUserAgent() );
    }

    public void testUserAgentSelection1 ( ) throws Exception {
        BufferedReader br = getReader(TEST2);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "testUserAgent");

        assertEquals("lineset didn't contain the right number of disallowing rules", 1, lineset.getLines().length);
        assertEquals("expected to obey user agent 'test'", "test", lineset.getUserAgent() );
    }

    public void testUserAgentSelection2 ( ) throws Exception {
        BufferedReader br = getReader(TEST2);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "testAgent");

        assertEquals("lineset didn't contain the right number of disallowing rules", 0, lineset.getLines().length);
        assertEquals("expected to obey user agent 'testAgent'", "testAgent", lineset.getUserAgent() );
    }

    public void testUserAgentSelection3 ( ) throws Exception {
        BufferedReader br = getReader(TEST2);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "anotherAgent");

        assertEquals("lineset didn't contain the right number of disallowing rules", 2, lineset.getLines().length);
        assertEquals("expected to obey user agent '*'", "*", lineset.getUserAgent() );
    }

    public void testUserAgentSelectionEmptyString ( ) throws Exception {
        BufferedReader br = getReader(TEST2);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "");

        assertEquals("lineset didn't contain the right number of disallowing rules", 2, lineset.getLines().length);
        assertEquals("expected to obey user agent '*'", "*", lineset.getUserAgent() );
    }

    public void testUserAgentSelectionNullString ( ) throws Exception {
        BufferedReader br = getReader(TEST2);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, null);

        assertEquals("lineset didn't contain the right number of disallowing rules", 2, lineset.getLines().length);
        assertEquals("expected to obey user agent '*'", "*", lineset.getUserAgent() );
    }

    public void testUserAgentNotListed ( ) throws Exception {
        BufferedReader br = getReader(TEST3);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "unlistedUA");

        assertNull("lineset wasn't null for non-listed agent", lineset);
    }

    public void testEmptyRobotsTXT ( ) throws Exception {
        BufferedReader br = getReader(TEST4);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "testUserAgent");

        assertNull ("lineset wasn't null for empty robots.txt", lineset );
    }

    public void testCommentEmptyRobotsTXT ( ) throws Exception {
        BufferedReader br = getReader(TEST5);
        RobotsTXTLineSet lineset = RobotsTXTLineSet.findLineSet(br, "testUserAgent");

        assertNull ("lineset wasn't null for empty robots.txt", lineset );
    }

}
