package net.javacoding.jspider.mod.eventfilter;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.spi.EventFilter;

/**
 * Event filter implementation that filters out all events that are not error
 * notifications.  This way it is possible to get an overview of all errors that
 * are encountered during the spidering process.
 *
 * $Id: ErrorsOnlyEventFilter.java,v 1.3 2003/04/03 16:25:12 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ErrorsOnlyEventFilter implements EventFilter {

    /**
     * Method that filters the events.  Returns true for error events, false for
     * other events.
     * @param event the event to be evaluated.
     * @return true if the event is to be let through the filter
     */
    public boolean filterEvent(JSpiderEvent event) {
        return event.isError ( );
    }

}
