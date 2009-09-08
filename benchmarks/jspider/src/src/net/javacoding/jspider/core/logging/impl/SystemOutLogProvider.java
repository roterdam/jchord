package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogProvider;

/**
 * Log Provider implementation that generates Log instances that
 * redirect all output to the console.
 *
 * $Id: SystemOutLogProvider.java,v 1.3 2003/03/27 17:44:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SystemOutLogProvider implements LogProvider {

    /**
     * Creates a new Log instance.
     * @return Log instance
     */
    public Log createLog(String category) {
        return new SystemOutLogImpl();
    }

}
