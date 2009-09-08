package net.javacoding.jspider.mod.eventfilter;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.spi.EventFilter;

/**
 * Event filter implementation that filters out all events that are error
 * notifications.
 *
 * $Id: NonErrorsOnlyEventFilter.java,v 1.1 2003/04/07 15:50:53 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class NonErrorsOnlyEventFilter implements EventFilter {

    /**
     * Method that filters the events.  Returns true for error events, false for
     * other events.
     * @param event the event to be evaluated.
     * @return true if the event is to be let through the filter
     */
    public boolean filterEvent(JSpiderEvent event) {
        return ! event.isError ( );
    }

}
