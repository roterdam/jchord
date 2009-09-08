package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;
import net.javacoding.jspider.api.model.Folder;

import java.net.URL;


/**
 *
 * $Id: URLFoundEvent.java,v 1.5 2003/04/09 17:08:04 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class URLFoundEvent extends BaseCoreEventImpl {

    protected URL url;
    protected URL foundURL;

    public URLFoundEvent(SpiderContext context, URL url, URL foundURL) {
        super(context);
        this.url = url;
        this.foundURL = foundURL;
    }

    public URL getURL() {
        return url;
    }

    public URL getFoundURL() {
        return foundURL;
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

}
