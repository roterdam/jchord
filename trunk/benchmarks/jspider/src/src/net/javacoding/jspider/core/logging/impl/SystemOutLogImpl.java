package net.javacoding.jspider.core.logging.impl;



/**
 * Logger implementation that redirects all logged output to the
 * console.
 *
 * $Id: SystemOutLogImpl.java,v 1.2 2003/03/27 17:44:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SystemOutLogImpl extends BaseLogImpl {


    /**
     * Worker method that logs the message to the Console.
     * @param message
     */
    public void doLog(String message) {
        System.out.println(message);
    }

    public void doLog(Object o) {
        doLog ("" + o);
    }

    public void doLog(Throwable t) {
        doLog ("" + t);
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public boolean isFatalEnabled() {
        return true;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public boolean isTraceEnabled() {
        return true;
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public void trace(Object o) {
        doLog(o);
    }

    public void trace(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

    public void debug(Object o) {
        doLog(o);
    }

    public void debug(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

    public void info(Object o) {
        doLog(o);
    }

    public void info(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

    public void warn(Object o) {
        doLog(o);
    }

    public void warn(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

    public void error(Object o) {
        doLog(o);
    }

    public void error(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

    public void fatal(Object o) {
        doLog(o);
    }

    public void fatal(Object o, Throwable throwable) {
        doLog(o);
        doLog(throwable);
    }

}
