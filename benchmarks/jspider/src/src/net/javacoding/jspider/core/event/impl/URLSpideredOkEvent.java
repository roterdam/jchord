package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.api.model.HTTPHeader;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;

import java.net.URL;
import java.net.URLConnection;


/**
 *
 * $Id: URLSpideredOkEvent.java,v 1.6 2003/03/27 17:44:02 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class URLSpideredOkEvent extends URLRelatedBaseEventImpl {

    protected String mimeType;
    protected int timeMs;
    protected int size;
    protected byte[] bytes;
    protected int httpStatus;
    protected URLConnection urlConnection;
    protected HTTPHeader[] headers;

    public URLSpideredOkEvent(SpiderContext context, URL url, int httpStatus, URLConnection urlConnection, String mimeType, int timeMs, int size, byte[] bytes, HTTPHeader[] headers) {
        super(context, url);
        this.mimeType = mimeType;
        this.timeMs = timeMs;
        this.size = size;
        this.bytes = bytes;
        this.httpStatus = httpStatus;
        this.urlConnection = urlConnection;
        this.headers = headers;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String toString() {
        return "url spidered : " + url + " (" + httpStatus + "," + mimeType + "," + timeMs + " ms," + size + " bytes )";
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

    public URLConnection getURLConnection() {
        return urlConnection;
    }

    public int getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getTimeMs() {
        return timeMs;
    }

    public HTTPHeader[] getHeaders ( ) {
        return headers;
    }
}
