package net.javacoding.jspider.core.throttle.impl;

import net.javacoding.jspider.core.throttle.Throttle;

import java.util.HashMap;
import java.util.Map;


/**
 * Throttle implementation that displays behaviour like multiple simultaneous
 * users would do.  Each worker thread (which represents a user) is throttled
 * separately, and the latency between two requests is a random value between
 * a configurable minimum and maximum value.
 * This way, a normal web user's 'thinking time' can be faked.  More inequal
 * partitioning of requests on the webserver will be the result, as with
 * human users accessing a system.
 *
 * $Id: SimultaneousUsersThrottleImpl.java,v 1.3 2003/02/27 16:47:51 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public class SimultaneousUsersThrottleImpl implements Throttle {

    /** Map which contains the last fetch times per worker thread (user). */
    protected Map lastAllows;

    /** configured minimum value of thinking time. */
    protected int min;

    /** configured maximum value of thinking time. */
    protected int max;


    /**
     * Public constructor taking the thinking time min and max values as params.
     * @param minThinkTime the minimum time between two requests from the same user
     * @param maxThinkTime the maximum time between two requests from the same user
     */
    public SimultaneousUsersThrottleImpl ( int minThinkTime, int maxThinkTime ) {
        this.min = minThinkTime;
        this.max = maxThinkTime;
        this.lastAllows = new HashMap();
    }

    /**
     * Implemented throttle method of the Throttle interface, which will keep
     * the worker threads under control.
     */
    public void throttle() {
        Thread thread = Thread.currentThread();

        Long lastAllowObject = (Long)lastAllows.get(thread);

        if ( lastAllowObject == null ) {
            lastAllowObject = new Long ( System.currentTimeMillis() );
        }

        long lastAllow = lastAllowObject.longValue();
        long thisTime = System.currentTimeMillis();
        long milliseconds = min + (int)(Math.random() * ( max-min ) );

        long scheduledTime = lastAllow + milliseconds;

        if (scheduledTime > thisTime) {
            try {
                Thread.sleep(scheduledTime - thisTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastAllow = System.currentTimeMillis();
        lastAllows.put(thread,new Long(lastAllow));
    }

}
