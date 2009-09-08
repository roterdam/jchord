package net.javacoding.jspider.core.threading;

import net.javacoding.jspider.api.event.monitor.MonitorEvent;
import net.javacoding.jspider.core.dispatch.EventDispatcher;

/**
 * $Id: MonitorThread.java,v 1.3 2003/02/27 16:47:48 vanrogu Exp $
 */
public abstract class MonitorThread extends Thread {

    protected EventDispatcher dispatcher;
    protected int interval;

    public MonitorThread ( EventDispatcher dispatcher, int interval, String subject ) {
        super ( subject + " monitor" );
        setDaemon(true);
        this.dispatcher = dispatcher;
        this.interval = interval;
    }

    public void run ( ) {
        MonitorEvent event = null;
        while ( true ) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            event = doMonitorTask ( );
            dispatcher.dispatch(event);
        }
    }

    public abstract MonitorEvent doMonitorTask ( );

}
