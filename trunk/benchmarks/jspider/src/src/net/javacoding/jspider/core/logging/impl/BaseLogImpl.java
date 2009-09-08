package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;

/**
 * Base implementation of a logger.  It contains a template method
 * that allows concrete subclasses to do the real logging work.
 *
 * $Id: BaseLogImpl.java,v 1.2 2003/03/27 17:44:04 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public abstract class BaseLogImpl implements Log {

    /**
     * Log method.
     * @param type the type of logging
     * @param message the message to be logged
     */
    public void log(int type, String message) {
        doLog ( "[LOG] " + message );
    }

    /**
     * Logs a message.
     * @param type type of the log
     * @param object object of which the toString ( ) will be logged
     */
    public void log(int type, Object object) {
        log ( type, "" + object );
    }

    /**
     * Worker method that will do the real logging.
     * @param message formatted message to be logged
     */
    public abstract void doLog ( String message );

}
