package net.javacoding.jspider.core.util.config;

/**
 * $Id: MappedPropertySet.java,v 1.1 2003/04/01 19:44:41 vanrogu Exp $
 */
public class MappedPropertySet implements PropertySet {

    protected String prefix;
    protected PropertySet delegate;

    public MappedPropertySet ( String prefix, PropertySet delegate ) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    public String getString(String name, String defaultValue) {
        return delegate.getString(prefix + "." + name, defaultValue);
    }

    public Class getClass(String name, Class defaultValue) {
        return delegate.getClass(prefix + "." + name, defaultValue);
    }

    public int getInteger(String name, int defaultValue) {
        return delegate.getInteger(prefix + "." + name, defaultValue);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return delegate.getBoolean(prefix + "." + name, defaultValue);
    }

}
