package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;

import java.net.URL;


/**
 *
 * $Id: ResourceParsedOkEvent.java,v 1.3 2003/02/28 17:39:04 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceParsedOkEvent extends URLRelatedBaseEventImpl {


    public ResourceParsedOkEvent(SpiderContext context, URL url) {
        super(context, url);
    }


    public String toString() {
        return "resource parsed : " + url;
    }


    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

}
