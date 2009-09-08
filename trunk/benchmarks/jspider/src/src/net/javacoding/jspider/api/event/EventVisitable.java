package net.javacoding.jspider.api.event;


/**
 * Visitable part of the Visitor pattern implementation that's used for the
 * handling of JSpider API events.  By implementing this interface, the Event
 * becomes visitable for an EventVisitor
 *
 * $Id: EventVisitable.java,v 1.1.1.1 2002/11/20 17:02:31 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public interface EventVisitable {


    /**
     * Notifies the visitable object that a visitor wants to visit it.
     * @param visitor the visitor object that wants to visit this visitable
     */
    public void accept(EventVisitor visitor);

}
