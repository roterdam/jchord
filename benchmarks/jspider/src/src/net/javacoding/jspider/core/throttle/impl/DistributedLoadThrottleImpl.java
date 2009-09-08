package net.javacoding.jspider.core.throttle.impl;


import net.javacoding.jspider.core.throttle.Throttle;


/**
 * Throttle implementation that forces at least x milliseconds between two
 * subsequent requests on a certain host.
 *
 * $Id: DistributedLoadThrottleImpl.java,v 1.2 2003/02/27 16:47:50 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DistributedLoadThrottleImpl implements Throttle {

    /** min. milliseconds between two subsequent calls. */
    protected int milliseconds;

    /** last allowed time for a fetch. */
    protected long lastAllow;

    /**
     * Constructor taking the amount of milliseconds to wait between
     * two subsequent requests as a parameter.
     * @param milliseconds minimum nr of milliseconds
     */
    public DistributedLoadThrottleImpl(int milliseconds) {
        this.milliseconds = milliseconds;
        lastAllow = System.currentTimeMillis() - milliseconds;
    }

    /**
     * This method will block spider threads until they're allowed
     * to do a request.
     */
    public synchronized void throttle() {

        long thisTime = System.currentTimeMillis();
        long scheduledTime = lastAllow + milliseconds;

        if (scheduledTime > thisTime) {
            try {
                Thread.sleep(scheduledTime - thisTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastAllow = System.currentTimeMillis();
    }
}

