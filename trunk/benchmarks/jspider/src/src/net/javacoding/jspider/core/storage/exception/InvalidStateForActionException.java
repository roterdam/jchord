package net.javacoding.jspider.core.storage.exception;


/**
 *
 * $Id: InvalidStateForActionException.java,v 1.1 2002/11/20 17:26:04 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class InvalidStateForActionException extends RuntimeException {

    public InvalidStateForActionException() {
        super();
    }

    public InvalidStateForActionException(String message) {
        super(message);
    }
}

