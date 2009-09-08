package net.javacoding.jspider.core.event;


import net.javacoding.jspider.core.SpiderContext;



/**
 *
 * $Id: CoreEvent.java,v 1.3 2003/02/20 19:49:56 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface CoreEvent extends CoreEventVisitable {

    public String getName();

    public SpiderContext getContext();

}