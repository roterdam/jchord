package net.javacoding.jspider.core.util;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * $Id: EMailAddressUtil.java,v 1.1 2003/04/08 15:50:37 vanrogu Exp $
 */
public class EMailAddressUtil {

    public static final String PROTOCOL_PREFIX = "mailto:";

    public static boolean canBeEMailAddress(String urlString) {
        boolean result = false;
        URL url = fixEMailAddress(urlString);

        if ( url != null ) {
            result = isEMailAddress(fixEMailAddress(urlString));
        }

        return result;
    }

    public static URL fixEMailAddress(String urlString) {
        URL result = null;
        try {
            result = new URL(urlString);
        } catch (MalformedURLException e) {
            try {
                result = new URL(PROTOCOL_PREFIX + urlString);
            } catch (MalformedURLException ex) { }
        }

        if (result == null || !isEMailAddress(result)) {
            result = null;
        }

        return result;
    }

    public static boolean isEMailAddress(URL url) {
        boolean result = true;

        String urlString = url.toString();

        if (url.getHost() != null && !url.getHost().trim().equals("")) {
            result = false;
        }
        if (url.getPort() != -1) {
            result = false;
        }

        int pos = urlString.indexOf('@');
        if (pos == -1) {
            result = false;
        } else {
            // check for a second '@' sign
            if (urlString.indexOf('@', pos + 1) != -1) {
                return false;
            }
        }

        // check if the host is possible
        if ( pos > (urlString.length() - 6) ) {
            result = false;
        }

        return result;

    }

    public static String getEMailAddress(URL url) {
        String urlString = url.toString();
        if (urlString.toLowerCase().startsWith(PROTOCOL_PREFIX)) {
            urlString = urlString.substring(PROTOCOL_PREFIX.length());
        } else {
            urlString = null;
        }
        return urlString;
    }

}
