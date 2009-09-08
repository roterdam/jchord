package net.javacoding.jspider.mod.eventfilter;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.spi.EventFilter;

/**
 * $Id: AllowNoneEventFilter.java,v 1.3 2003/04/03 16:25:12 vanrogu Exp $
 */
public class AllowNoneEventFilter implements EventFilter {

    public boolean filterEvent(JSpiderEvent event) {
        return false;
    }

}
