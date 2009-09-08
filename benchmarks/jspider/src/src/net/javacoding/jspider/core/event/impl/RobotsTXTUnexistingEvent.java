package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;
import net.javacoding.jspider.api.model.HTTPHeader;

import java.net.URL;
import java.net.URLConnection;


/**
 *
 * $Id: RobotsTXTUnexistingEvent.java,v 1.3 2003/04/01 19:44:37 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RobotsTXTUnexistingEvent extends URLSpideredErrorEvent {

    protected URL robotsTXTURL;

    public RobotsTXTUnexistingEvent(URL robotsTXTURL, SpiderContext context, URL url, int httpStatus, URLConnection urlConnection, HTTPHeader[] headers, Exception error) {
        super(context, url,  httpStatus, urlConnection, headers, error );
        this.robotsTXTURL = robotsTXTURL;
    }

    public String toString() {
        return "robots.txt spidered : " + url + " ERROR : " + error.getClass() + ((httpStatus != 0) ? " (HTTP Status: " + httpStatus + ")" : "");
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

    public URL getRobotsTXTURL(){
        return robotsTXTURL;
    }

}
