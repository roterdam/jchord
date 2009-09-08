package net.javacoding.jspider.mockobjects.plugin;

import net.javacoding.jspider.core.util.config.PropertySet;

/**
 * $Id: TestPluginNameConfigCtr.java,v 1.1 2003/04/02 20:55:45 vanrogu Exp $
 */
public class TestPluginNameConfigCtr extends BaseTestPluginImpl {

    public TestPluginNameConfigCtr ( String name, PropertySet props ) {
        super ( BaseTestPluginImpl.NAMECONFIG_CONSTRUCTOR );
    }

    public TestPluginNameConfigCtr ( String name ) {
        super ( BaseTestPluginImpl.NAME_CONSTRUCTOR );
    }

    public TestPluginNameConfigCtr ( PropertySet props ) {
        super ( BaseTestPluginImpl.CONFIG_CONSTRUCTOR );
    }

    public TestPluginNameConfigCtr (  ) {
        super ( BaseTestPluginImpl.DEFAULT_CONSTRUCTOR );
    }
}
