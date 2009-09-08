package net.javacoding.jspider.tool.util;

/**
 * $Id: URLFactory.java,v 1.1 2003/03/25 16:25:09 vanrogu Exp $
 */
public class URLFactory {

    public static String createURL ( String[] args ) {
        int counter = 0;
        String url = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ( ! arg.startsWith("-") ) {
                if ( ++counter == 2 ) {
                    url = arg;
                    break;
                }
            }
        }
        return url;
    }

}
