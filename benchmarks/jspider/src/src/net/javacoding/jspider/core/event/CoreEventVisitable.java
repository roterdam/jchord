package net.javacoding.jspider.core.event;


import java.net.URL;


/**
 *
 * $Id: CoreEventVisitable.java,v 1.3 2003/02/28 17:39:03 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface CoreEventVisitable {

    public void accept(URL url, CoreEventVisitor visitor);

}
