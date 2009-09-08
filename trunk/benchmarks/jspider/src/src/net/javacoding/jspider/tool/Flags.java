package net.javacoding.jspider.tool;

/**
 * $Id: Flags.java,v 1.1 2003/03/25 16:25:06 vanrogu Exp $
 */
public class Flags {

    protected boolean silent;

    public Flags ( boolean silent ) {
        this.silent = silent;
    }

    public boolean getSilent ( ) {
        return silent;
    }

}
