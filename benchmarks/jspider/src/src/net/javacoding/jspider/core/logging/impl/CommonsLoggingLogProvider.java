package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogProvider;
import org.apache.commons.logging.LogFactory;

/**
 * $Id: CommonsLoggingLogProvider.java,v 1.1 2003/03/27 17:44:05 vanrogu Exp $
 */
public class CommonsLoggingLogProvider implements LogProvider {

    public Log createLog(String category) {
        return new CommonsLoggingLogImpl(LogFactory.getLog(category));
    }
}
