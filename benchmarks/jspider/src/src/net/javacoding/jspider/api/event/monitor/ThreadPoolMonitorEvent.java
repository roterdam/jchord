package net.javacoding.jspider.api.event.monitor;

/**
 * $Id: ThreadPoolMonitorEvent.java,v 1.3 2003/03/28 17:26:26 vanrogu Exp $
 */
public class ThreadPoolMonitorEvent extends MonitorEvent {

    protected String name;
    protected int occupationPct;
    protected int idlePct;
    protected int blockedPct;
    protected int busyPct;
    protected int size;

    public ThreadPoolMonitorEvent ( String name, int occupationPct, int idlePct, int blockedPct, int busyPct, int size ) {
        this.name = name;
        this.occupationPct = occupationPct;
        this.idlePct = idlePct;
        this.blockedPct = blockedPct;
        this.busyPct = busyPct;
        this.size = size;
    }

    public String toString() {
        return "ThreadPool " + getName() + " occupation:" + (getOccupationPct()) + "% [idle: " + getIdlePct() + "%, blocked: " + getBlockedPct() + "%, busy: " + getBusyPct() + "%], size: " + getSize();
    }

    public String getComment() {
        return toString();
    }

    public String getName() {
        return name;
    }

    public int getOccupationPct() {
        return occupationPct;
    }

    public int getIdlePct() {
        return idlePct;
    }

    public int getBlockedPct() {
        return blockedPct;
    }

    public int getBusyPct() {
        return busyPct;
    }

    public int getSize() {
        return size;
    }

}
