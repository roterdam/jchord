package net.javacoding.jspider.core.event.impl;


import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.event.CoreEventVisitor;

import java.net.URL;


/**
 *
 * $Id: URLRelatedBaseEventImpl.java,v 1.2 2003/02/06 17:22:24 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class URLRelatedBaseEventImpl extends BaseCoreEventImpl {

    protected URL url;

    public URLRelatedBaseEventImpl(SpiderContext context, URL url) {
        super(context);
        this.url = url;
    }

    public URL getURL() {
        return url;
    }

    public void accept(URL url, CoreEventVisitor visitor) {
        visitor.visit(url, this);
    }

}
