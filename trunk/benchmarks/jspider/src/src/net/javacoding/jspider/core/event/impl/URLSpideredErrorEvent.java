package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;
import net.javacoding.jspider.api.model.HTTPHeader;

import java.net.URL;
import java.net.URLConnection;


/**
 *
 * $Id: URLSpideredErrorEvent.java,v 1.4 2003/04/01 19:44:37 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class URLSpideredErrorEvent extends URLRelatedBaseEventImpl {

    protected Exception error;
    protected int httpStatus;
    protected HTTPHeader[] headers;

    public URLSpideredErrorEvent(SpiderContext context, URL url, int httpStatus, URLConnection urlConnection, HTTPHeader[] headers, Exception error) {
        super(context, url);
        this.error = error;
        this.httpStatus = httpStatus;
        this.headers = headers;
    }

    public String toString() {
        return "url spidered : " + url + " ERROR : " + error.getClass() + ((httpStatus != 0) ? " (HTTP Status: " + httpStatus + ")" : "");
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

    public HTTPHeader[] getHeaders ( ) {
        return headers;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public Exception getException() {
        return error;
    }

}
