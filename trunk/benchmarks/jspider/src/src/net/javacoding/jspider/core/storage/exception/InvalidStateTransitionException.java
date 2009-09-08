package net.javacoding.jspider.core.storage.exception;


/**
 *
 * $Id: InvalidStateTransitionException.java,v 1.1 2002/11/20 17:26:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException() {
        super();
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
