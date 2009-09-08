package net.javacoding.jspider.tool.util;

import net.javacoding.jspider.tool.Parameters;

import java.util.ArrayList;

/**
 * $Id: ParametersFactory.java,v 1.2 2003/04/01 19:44:42 vanrogu Exp $
 */
public class ParametersFactory {

    public static Parameters createParameters(String[] args) {
        int skip = 2; // skip tool & url params
        ArrayList params = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                if (skip == 0) {
                    params.add(arg);
                } else {
                    skip--;
                }
            }
        }
        String[] paramArray = (String[]) params.toArray(new String[params.size()]);
        return new Parameters(paramArray);
    }

}
