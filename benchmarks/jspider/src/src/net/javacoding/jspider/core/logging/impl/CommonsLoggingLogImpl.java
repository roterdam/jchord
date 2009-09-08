package net.javacoding.jspider.core.logging.impl;

import net.javacoding.jspider.core.logging.Log;

/**
 * $Id: CommonsLoggingLogImpl.java,v 1.1 2003/03/27 17:44:04 vanrogu Exp $
 */
public class CommonsLoggingLogImpl implements Log {

    org.apache.commons.logging.Log logger;

    public CommonsLoggingLogImpl ( org.apache.commons.logging.Log logger ) {
        this.logger = logger;
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return logger.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void trace(Object o) {
        logger.trace(o);
    }

    public void trace(Object o, Throwable throwable) {
        logger.trace(o, throwable);
    }

    public void debug(Object o) {
        logger.debug(o);
    }

    public void debug(Object o, Throwable throwable) {
        logger.debug(o, throwable);
    }

    public void info(Object o) {
        logger.info(o);
    }

    public void info(Object o, Throwable throwable) {
        logger.info(o, throwable);
    }

    public void warn(Object o) {
        logger.warn(o);
    }

    public void warn(Object o, Throwable throwable) {
        logger.warn(o, throwable);
    }

    public void error(Object o) {
        logger.error(o);
    }

    public void error(Object o, Throwable throwable) {
        logger.error(o, throwable);
    }

    public void fatal(Object o) {
        logger.fatal(o);
    }

    public void fatal(Object o, Throwable throwable) {
        logger.fatal(o, throwable);
    }

}
