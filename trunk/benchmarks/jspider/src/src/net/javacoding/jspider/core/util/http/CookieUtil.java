package net.javacoding.jspider.core.util.http;


import net.javacoding.jspider.api.model.Cookie;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 *
 * $Id: CookieUtil.java,v 1.8 2003/04/08 18:44:15 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class CookieUtil {

    public static final String HEADER_SET_COOKIE = "Set-Cookie";

    public Cookie[] getCookies(URLConnection connection) {

        String[] cookieStrings = getCookieStrings(connection);
        return getCookies(cookieStrings);
    }

    protected String[] getCookieStrings(URLConnection connection) {
        ArrayList arrayList = new ArrayList();

        String headerKey = null;
        String headerValue = null;

        int i = 0;

        do {

            headerKey = connection.getHeaderFieldKey(i);
            // Must be read here because the HTTP header's key is null, so loop would end otherwise
            headerValue = connection.getHeaderField(i);

            if (HEADER_SET_COOKIE.equalsIgnoreCase(headerKey)) {
                if (headerValue != null) {
                    arrayList.add(headerValue.trim());
                } else {
                    arrayList.add(headerValue);
                }
            }
            i++;

        } while (headerKey != null || headerValue != null);

        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    protected Cookie[] getCookies(String[] cookieString) {
        Cookie[] cookies = new Cookie[cookieString.length];
        int used = 0;

        for (int i = 0; i < cookieString.length; i++) {
            String s = cookieString[i];
            Cookie cookie = getCookie(s);
            if (cookie != null) {
                cookies[used++] = cookie;
            }
        }
        Cookie[] retVal = new Cookie[used];
        System.arraycopy(cookies, 0, retVal, 0, used);
        return retVal;
    }

    public Cookie getCookie(String cookieString) {
        Cookie cookie = null;
        if (cookieString != null && !cookieString.trim().equals("")) {
            StringTokenizer st = new StringTokenizer(cookieString, ";");

            String name = "";
            String value = "";
            String domain = "";
            String path = "";
            String expires = "";

            // First part of a cookie string is name=value; ...
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(token, "=");
                if (st2.hasMoreTokens()) {
                    String propName = st2.nextToken().trim();
                    if (st2.hasMoreTokens()) {
                        String propValue = st2.nextToken().trim();
                        if ("value".equalsIgnoreCase(propName)) {
                            value = propValue;
                        } else if ("domain".equalsIgnoreCase(propName)) {
                            domain = propValue;
                        } else if ("path".equalsIgnoreCase(propName)) {
                            path = propValue;
                        } else if ("expires".equalsIgnoreCase(propName)) {
                            expires = propValue;
                        } else {
                            name = propName;
                            value = propValue;
                        }
                    }
                }
            }
            cookie = new Cookie(name, value, domain, path, expires);
        }
        return cookie;
    }

}
