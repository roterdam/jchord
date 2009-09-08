package net.javacoding.jspider.core.util.config;

/**
 * $Id: PropertySet.java,v 1.5 2003/04/03 15:57:23 vanrogu Exp $
 */
public interface PropertySet {

    public String getString(String name, String defaultValue);

    public Class getClass(String name, Class defaultValue);

    public int getInteger(String name, int defaultValue);

    public boolean getBoolean(String name, boolean defaultValue);

}
