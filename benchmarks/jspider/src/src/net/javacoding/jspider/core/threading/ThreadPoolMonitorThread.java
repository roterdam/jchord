package net.javacoding.jspider.core.threading;

import net.javacoding.jspider.api.event.monitor.MonitorEvent;
import net.javacoding.jspider.api.event.monitor.ThreadPoolMonitorEvent;
import net.javacoding.jspider.core.dispatch.EventDispatcher;

/**
 * $Id: ThreadPoolMonitorThread.java,v 1.3 2003/02/22 07:55:58 vanrogu Exp $
 */
public class ThreadPoolMonitorThread extends MonitorThread {

    protected WorkerThreadPool pool;

    public ThreadPoolMonitorThread ( EventDispatcher dispatcher, int interval, WorkerThreadPool pool ) {
        super ( dispatcher, interval, pool.getName() );
        this.pool = pool;
        start();
    }

    public MonitorEvent doMonitorTask() {
        return new ThreadPoolMonitorEvent ( pool.getName(), pool.getOccupation(), pool.getIdlePercentage(), pool.getBlockedPercentage(),  pool.getBusyPercentage(), pool.getSize() );
    }
}
