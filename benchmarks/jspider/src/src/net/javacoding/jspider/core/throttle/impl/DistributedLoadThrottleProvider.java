package net.javacoding.jspider.core.throttle.impl;


import net.javacoding.jspider.core.throttle.Throttle;
import net.javacoding.jspider.core.throttle.ThrottleProvider;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;


/**
 * Throttle provider class for the Distributed Load Throttle implementation.
 * This class will generate throttle implementations when refernced from the
 * configuration file (default).
 *
 * $Id: DistributedLoadThrottleProvider.java,v 1.7 2003/04/03 15:57:20 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DistributedLoadThrottleProvider implements ThrottleProvider {

    public static final String INTERVAL = "interval";
    public static final int INTERVAL_DEFAULT = 1000;
    public static final int INTERVAL_MIN = 250;

    /**
     * Method that instantiates the Throttle implementation.
     * @return Throttle instance
     */
    public Throttle createThrottle(PropertySet props) {

        /* get the interval from the configuration. */
        int interval = props.getInteger(INTERVAL, INTERVAL_DEFAULT);

        Log log = LogFactory.getLog(DistributedLoadThrottleProvider.class);

        if (interval < INTERVAL_MIN) {
            log.warn("Throttle interval < " + INTERVAL_MIN + " ms is dangereous - set to minimum allowed of " + INTERVAL_MIN + " ms");
            interval = INTERVAL_MIN;
        }

        log.debug("throttle interval set to " + interval + " ms.");

        return new DistributedLoadThrottleImpl(interval);
    }
}
