package net.javacoding.jspider.core.dispatch;


import net.javacoding.jspider.api.event.JSpiderEvent;


/**
 *
 * $Id: EventDispatcher.java,v 1.3 2003/02/27 16:47:31 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface EventDispatcher {

    public void initialize ( );

    public void shutdown ( );

    public void dispatch(JSpiderEvent event);


}
