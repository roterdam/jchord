package net.javacoding.jspider.mockobjects;

import net.javacoding.jspider.core.util.config.PropertySet;

import java.util.HashMap;

/**
 * $Id: OverridingPropertySet.java,v 1.1 2002/12/29 15:30:00 vanrogu Exp $
 */
public class OverridingPropertySet implements PropertySet {

    protected PropertySet props;
    protected HashMap overridden;

    public OverridingPropertySet ( PropertySet props ) {
        this.props = props;
        this.overridden = new HashMap ( );
    }

    public void setValue ( String name, Object value ) {
        overridden.put(name, value);
    }

    public String getString(String name, String defaultValue) {
        if ( overridden.containsKey(name) ) {
            return (String)overridden.get(name);
        } else {
            return props.getString(name, defaultValue);
        }
    }

    public Class getClass(String name, Class defaultValue) {
        if ( overridden.containsKey(name) ) {
            return (Class)overridden.get(name);
        } else {
            return props.getClass(name, defaultValue);
        }
    }

    public int getInteger(String name, int defaultValue) {
        if ( overridden.containsKey(name) ) {
            return ((Integer)overridden.get(name)).intValue();
        } else {
            return props.getInteger(name, defaultValue);
        }
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        if ( overridden.containsKey(name) ) {
            return ((Boolean)overridden.get(name)).booleanValue();
        } else {
            return props.getBoolean(name, defaultValue);
        }
    }
}
