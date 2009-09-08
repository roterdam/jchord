package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;

/**
 * Log implemenation that does no logging at all.  All incoming log
 * messages are simply discarded.
 *
 * $Id: DevNullLogImpl.java,v 1.2 2003/03/27 17:44:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class DevNullLogImpl implements Log {


    public boolean isDebugEnabled() {
        return false;
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public boolean isFatalEnabled() {
        return false;
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public boolean isWarnEnabled() {
        return false;
    }

    public void trace(Object o) {
    }

    public void trace(Object o, Throwable throwable) {
    }

    public void debug(Object o) {
    }

    public void debug(Object o, Throwable throwable) {
    }

    public void info(Object o) {
    }

    public void info(Object o, Throwable throwable) {
    }

    public void warn(Object o) {
    }

    public void warn(Object o, Throwable throwable) {
    }

    public void error(Object o) {
    }

    public void error(Object o, Throwable throwable) {
    }

    public void fatal(Object o) {
    }

    public void fatal(Object o, Throwable throwable) {
    }
}
