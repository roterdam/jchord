package net.javacoding.jspider.core.throttle.impl;

import net.javacoding.jspider.core.throttle.Throttle;
import net.javacoding.jspider.core.throttle.ThrottleProvider;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;



/**
 * Throttle Provider implementation that will create throttle objects that
 * simulate multiple simultaneous users making requests on the webserver.
 *
 * $Id: SimultaneousUsersThrottleProvider.java,v 1.6 2003/04/03 15:57:21 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SimultaneousUsersThrottleProvider implements ThrottleProvider {

    public static final String THINKTIME_MIN = "thinktime.min";
    public static final String THINKTIME_MAX = "thinktime.max";

    public static final int THINKTIME_MIN_DEFAULT = 1000;
    public static final int THINKTIME_MAX_DEFAULT = 1000;


    /**
     * Factory method that will create the Throttle implementation for
     * simultaneous user simulation, with the config parameters defined
     * in the configuration file.
     * @return Throttle implementation to be used
     */
    public Throttle createThrottle(PropertySet props) {

        Log log = LogFactory.getLog(SimultaneousUsersThrottleProvider.class);

        int min = props.getInteger(THINKTIME_MIN,THINKTIME_MIN_DEFAULT);
        int max = props.getInteger(THINKTIME_MAX,THINKTIME_MAX_DEFAULT);

        log.debug("min thinktime set to " + min);
        log.debug("max thinktime set to " + max);

        return new SimultaneousUsersThrottleImpl ( min, max );
    }

}
