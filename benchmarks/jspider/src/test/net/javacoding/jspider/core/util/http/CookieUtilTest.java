package net.javacoding.jspider.core.util.http;

import junit.framework.TestCase;
import net.javacoding.jspider.api.model.Cookie;

/**
 * $Id: CookieUtilTest.java,v 1.4 2003/03/08 19:52:03 vanrogu Exp $
 */
public class CookieUtilTest extends TestCase {

    protected CookieUtil cookieUtil;

    public CookieUtilTest (  ) {
        super ( "CookieUtilTest" );
    }

    protected void setUp() throws Exception {
        cookieUtil = new CookieUtil( );
    }

    public void testNullCookieStringArray ( ) {
        String test = null;
        Cookie[] cookies = cookieUtil.getCookies(new String[]{test});

        assertNotNull("Cookies is null after getCookies(null)", cookies);
        assertEquals ( "more than 0 cookies returned for null cookiestring", 0, cookies.length);
    }

    public void testNullCookieString ( ) {
        String test = null;
        Cookie cookie = cookieUtil.getCookie(test);

        assertNull("Cookie is not null after getCookie(null)", cookie);
    }

    public void testEmptyCookieStringArray ( ) {
        Cookie[] cookies = cookieUtil.getCookies(new String[]{""});

        assertNotNull("Cookies is null after getCookies('')", cookies);
        assertEquals ( "more than 0 cookies returned for empty cookiestring", 0, cookies.length);
    }

    public void testEmptyCookieString ( ) {
        Cookie cookie = cookieUtil.getCookie("");

        assertNull("Cookies is not null after getCookie('')", cookie);
    }

    public void testSpacesCookieStringArray ( ) {
        Cookie[] cookies = cookieUtil.getCookies(new String[]{"   "});

        assertNotNull("Cookies is null after getCookies('   ')", cookies);
        assertEquals ( "more than 0 cookies returned for '   ' cookiestring", 0, cookies.length);
    }

    public void testSpacesCookieString ( ) {
        Cookie cookie = cookieUtil.getCookie("   ");

        assertNull("Cookies is not null after getCookie('   ')", cookie);
    }

    public void testSimpleCookie ( ) {
        String cookieString = "user=54347326073a; path=/; domain=j-spider.sourceforge.net; expires=Thursday, 31-Dec-2002 23:50:00 GMT\n";
        Cookie[] cookies = cookieUtil.getCookies(new String[]{cookieString});

        assertNotNull("cookie parser returned null cookie array reference", cookies);
        assertEquals("cookie parser returned a wrong number of cookies", 1, cookies.length);
        Cookie cookie = cookies[0];
        assertEquals("cookie parser returned wrong cookie name", "user", cookie.getName());
        assertEquals("cookie parser returned wrong cookie value", "54347326073a", cookie.getValue());
    }

    public void testFullCookie ( ) {
        String cookieString = "user=54347326073a; path=/path; domain=j-spider.sourceforge.net; expires=Thursday, 31-Dec-2002 23:50:00 GMT\n";
        Cookie[] cookies = cookieUtil.getCookies(new String[]{cookieString});

        assertNotNull("cookie parser returned null cookie array reference", cookies);
        assertEquals("cookie parser returned a wrong number of cookies", 1, cookies.length);
        Cookie cookie = cookies[0];
        assertEquals("cookie parser returned wrong cookie name", "user", cookie.getName());
        assertEquals("cookie parser returned wrong cookie value", "54347326073a", cookie.getValue());
        assertEquals("cookie parser returned wrong domain value", "j-spider.sourceforge.net", cookie.getDomain());
        assertEquals("cookie parser returned wrong path value", "/path", cookie.getPath());
        assertEquals("cookie parser returned wrong expires value", "Thursday, 31-Dec-2002 23:50:00 GMT", cookie.getExpires());
    }

    public void testMultipleCookies ( ) {
        String cookieString1 = "user=54347326073a; path=/; domain=j-spider.sourceforge.net; expires=Thursday, 31-Dec-2002 23:50:00 GMT\n";
        String cookieString2 = "name=jspider; path=/; domain=j-spider.sourceforge.net; expires=Thursday, 31-Dec-2002 23:50:00 GMT\n";
        Cookie[] cookies = cookieUtil.getCookies(new String[]{cookieString1,cookieString2});

        assertNotNull("cookie parser returned null cookie array reference", cookies);
        assertEquals("cookie parser returned a wrong number of cookies", 2, cookies.length);
        Cookie cookie = cookies[0];
        assertEquals("cookie parser returned wrong cookie name", "user", cookie.getName());
        assertEquals("cookie parser returned wrong cookie value", "54347326073a", cookie.getValue());
        cookie = cookies[1];
        assertEquals("cookie parser returned wrong cookie name", "name", cookie.getName());
        assertEquals("cookie parser returned wrong cookie value", "jspider", cookie.getValue());
    }

}
