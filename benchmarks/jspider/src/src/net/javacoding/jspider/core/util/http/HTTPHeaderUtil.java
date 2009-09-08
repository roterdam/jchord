package net.javacoding.jspider.core.util.http;

import net.javacoding.jspider.api.model.HTTPHeader;

import java.net.URLConnection;
import java.util.ArrayList;

/**
 * $Id: HTTPHeaderUtil.java,v 1.2 2003/03/08 19:52:02 vanrogu Exp $
 */
public class HTTPHeaderUtil {

    public static HTTPHeader[] getHeaders ( URLConnection connection ) {
        ArrayList arrayList = new ArrayList( );

        String headerKey = null;
        String headerValue = null;

        int i = 0;

        headerKey = connection.getHeaderFieldKey(i);
        headerValue = connection.getHeaderField(i);
        while ( headerKey != null || headerValue != null  ) {
            HTTPHeader header= new HTTPHeader(headerKey,  headerValue );
            arrayList.add(header);
            i++;
            headerKey = connection.getHeaderFieldKey(i);
            headerValue = connection.getHeaderField(i);
        }

        return (HTTPHeader[]) arrayList.toArray(new HTTPHeader[arrayList.size()]);
    }

}
