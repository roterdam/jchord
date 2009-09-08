package net.javacoding.jspider.core.util;

import junit.framework.TestCase;

import java.net.URL;

/**
 * $Id: EMailAddressUtilTest.java,v 1.1 2003/04/08 15:50:40 vanrogu Exp $
 */
public class EMailAddressUtilTest extends TestCase {

    public EMailAddressUtilTest ( ) {
        super ( "EMailAddressUtilTest" );
    }

    public void testIsEmailAddressSimple ( ) throws Exception {
        String url = "mailto:test@j-spider.sourceforge.net";
        boolean expected = true;
        applyTestIsEMailAddress ( url, expected );
    }

    public void testIsEmailAddressSimpleNoHost ( ) throws Exception {
        String url = "mailto:test@";
        boolean expected = false;
        applyTestIsEMailAddress ( url, expected );
    }

    public void testIsEmailAddressSimpleBadHost ( ) throws Exception {
        String url = "mailto:test@a.aa";
        boolean expected = false;
        applyTestIsEMailAddress ( url, expected );
    }

    public void testIsEmailAddressError ( ) throws Exception {
        String url = "mailto:testj-spider.sourceforge.net";
        boolean expected = false;
        applyTestIsEMailAddress ( url, expected );
    }

    public void testIsEmailAddressErrorDoubleAt ( ) throws Exception {
        String url = "mailto:test@j-spider@sourceforge.net";
        boolean expected = false;
        applyTestIsEMailAddress ( url, expected );
    }




    public void testGetEmailAddressSimple ( ) throws Exception {
        String url = "mailto:test@j-spider.sourceforge.net";
        String expected = "test@j-spider.sourceforge.net";
        String actual = EMailAddressUtil.getEMailAddress(new URL(url));
        assertEquals("mail address extracted is not as expected", expected, actual);
    }




    public void testIsFixableEmailAddressSimple ( ) throws Exception {
        String url = "mailto:test@j-spider.sourceforge.net";
        boolean expected = true;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressSimpleNoHost ( ) throws Exception {
        String url = "mailto:test@";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressSimpleBadHost ( ) throws Exception {
        String url = "mailto:test@a.aa";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressError ( ) throws Exception {
        String url = "mailto:testj-spider.sourceforge.net";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressErrorDoubleAt ( ) throws Exception {
        String url = "mailto:test@j-spider@sourceforge.net";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressSimpleNoProto ( ) throws Exception {
        String url = "test@j-spider.sourceforge.net";
        boolean expected = true;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressSimpleNoHostNoProto ( ) throws Exception {
        String url = "test@";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressSimpleBadHostNoProto ( ) throws Exception {
        String url = "test@a.aa";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressErrorNoProto ( ) throws Exception {
        String url = "testj-spider.sourceforge.net";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }

    public void testIsFixableEmailAddressErrorDoubleAtNoProto ( ) throws Exception {
        String url = "test@j-spider@sourceforge.net";
        boolean expected = false;
        applyTestIsFixableEMailAddress ( url, expected );
    }



    public void testFixEmailAddressSimple ( ) throws Exception {
        String url = "mailto:test@j-spider.sourceforge.net";
        String expected = "mailto:test@j-spider.sourceforge.net";
        String actual = EMailAddressUtil.fixEMailAddress(url).toString();
        assertEquals("fixed email address not as expected", expected, actual);
    }

    public void testFixEmailAddressSimpleNoProto ( ) throws Exception {
        String url = "test@j-spider.sourceforge.net";
        String expected = "mailto:test@j-spider.sourceforge.net";
        String actual = EMailAddressUtil.fixEMailAddress(url).toString();
        assertEquals("fixed email address not as expected", expected, actual);
    }




    public void applyTestIsEMailAddress ( String urlString, boolean expected ) throws Exception {
        URL url = new URL(urlString);
        boolean actual = EMailAddressUtil.isEMailAddress(url);
        assertEquals("test for mail address result not as expected", expected, actual);
    }

    public void applyTestIsFixableEMailAddress ( String urlString, boolean expected ) throws Exception {
        boolean actual = EMailAddressUtil.canBeEMailAddress(urlString);
        assertEquals("test for fixable mail address result not as expected", expected, actual);
    }

}
