package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogProvider;

/**
 * Log Provider that creates Log instances that simply discard all
 * incoming log messages.
 *
 * $Id: DevNullLogProvider.java,v 1.3 2003/03/27 17:44:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DevNullLogProvider implements LogProvider {

    /**
     * Creates a Log implementation.
     * @return Log object
     */
    public Log createLog(String category) {
        return new DevNullLogImpl();
    }

}
