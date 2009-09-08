package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;

import java.net.URL;


/**
 *
 * $Id: ResourceParsedErrorEvent.java,v 1.3 2003/02/28 17:39:04 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceParsedErrorEvent extends URLRelatedBaseEventImpl {

    protected Throwable error;

    public ResourceParsedErrorEvent(SpiderContext context, URL url, Throwable error) {
        super(context, url);
        this.error = error;
    }

    public String toString() {
        return "resource parse error : " + url;
    }


    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }
}
