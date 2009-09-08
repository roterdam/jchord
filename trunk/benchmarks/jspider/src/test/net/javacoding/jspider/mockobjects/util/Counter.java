package net.javacoding.jspider.mockobjects.util;

/**
 * $Id: Counter.java,v 1.1 2002/12/06 19:13:32 vanrogu Exp $
 */
public class Counter {

    protected int count;

    public Counter ( ) {
        count = 0;
    }

    public synchronized void increment ( ) {
        count = count + 1;
    }

    public synchronized int getValue ( )  {
        return count;
    }

}
