package net.javacoding.jspider.tool.util;

import net.javacoding.jspider.tool.Flags;

/**
 * $Id: FlagsFactory.java,v 1.1 2003/03/25 16:25:07 vanrogu Exp $
 */
public class FlagsFactory {

    public static Flags createFlags ( String[] args ) {

        boolean silent = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ( "-silent".equals(arg) ) {
                silent = true;
            }
        }
        return new Flags ( silent );
    }

}
