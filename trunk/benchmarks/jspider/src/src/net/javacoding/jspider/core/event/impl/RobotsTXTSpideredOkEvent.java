package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.api.model.HTTPHeader;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;

import java.net.URL;
import java.net.URLConnection;


/**
 *
 * $$
 *
 * @author Günther Van Roey
 */
public class RobotsTXTSpideredOkEvent extends URLSpideredOkEvent {

    protected URL robotsTXTURL;

    public RobotsTXTSpideredOkEvent(URL robotsTXTURL,SpiderContext context, URL url, int httpStatus, URLConnection urlConnection, String mimeType, int timeMs, int size, byte[] bytes, HTTPHeader[] headers) {
        super(context,url, httpStatus, urlConnection, mimeType, timeMs, size, bytes, headers);
        this.robotsTXTURL = robotsTXTURL;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String toString() {
        return "robots.txt spidered : " + url + " (" + httpStatus + "," + mimeType + "," + timeMs + " ms," + size + " bytes )";
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

    public URL getRobotsTXTURL(){
        return robotsTXTURL;
    }

}
