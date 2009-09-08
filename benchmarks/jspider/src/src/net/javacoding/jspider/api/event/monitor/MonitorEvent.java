package net.javacoding.jspider.api.event.monitor;

import net.javacoding.jspider.api.event.JSpiderEvent;

/**
 * $Id: MonitorEvent.java,v 1.2 2002/12/14 15:36:43 vanrogu Exp $
 */
public abstract class MonitorEvent extends JSpiderEvent {

    public int getType() {
        return JSpiderEvent.EVENT_TYPE_MONITORING;
    }

    public MonitorEvent ( ) {

    }

}
