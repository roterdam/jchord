package net.javacoding.jspider.core.throttle;

import net.javacoding.jspider.core.util.config.PropertySet;


/**
 * Interface that should be implemented by each Throttle provider class.
 * This allows for pluggable provider classes to be referenced from the
 * configuration files.
 *
 * $Id: ThrottleProvider.java,v 1.2 2002/12/23 17:13:36 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface ThrottleProvider {

    /**
     * Method that generated the Throttle implementation.
     * @return Throttle implementation
     */
    public Throttle createThrottle(PropertySet props);
}
