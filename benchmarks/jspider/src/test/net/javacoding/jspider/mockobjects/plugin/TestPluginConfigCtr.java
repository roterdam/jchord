package net.javacoding.jspider.mockobjects.plugin;

import net.javacoding.jspider.core.util.config.PropertySet;

/**
 * $Id: TestPluginConfigCtr.java,v 1.1 2003/04/02 20:55:44 vanrogu Exp $
 */
public class TestPluginConfigCtr extends BaseTestPluginImpl {

    public TestPluginConfigCtr ( PropertySet props ) {
        super ( BaseTestPluginImpl.CONFIG_CONSTRUCTOR );
    }

    public TestPluginConfigCtr ( String name ) {
        super ( BaseTestPluginImpl.NAME_CONSTRUCTOR);
    }

    public TestPluginConfigCtr ( ) {
        super ( BaseTestPluginImpl.DEFAULT_CONSTRUCTOR);
    }

}
