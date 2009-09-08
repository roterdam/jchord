package net.javacoding.jspider.api.event;


/**
 * Interface that will be implemented upon each class that will be used as an
 * event sink for JSpider API events.
 *
 * $Id: EventSink.java,v 1.3 2003/02/27 16:47:31 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public interface EventSink {

    public void initialize ( );

    public void shutdown ( );

    /**
     * Notifies the event sink of a new event.
     * @param event the event that occurred
     */
    public void notify(JSpiderEvent event);
}
