package net.javacoding.jspider.mockobjects.plugin;

/**
 * $Id: TestPluginNameCtr.java,v 1.1 2003/04/02 20:55:45 vanrogu Exp $
 */
public class TestPluginNameCtr extends BaseTestPluginImpl {

    public TestPluginNameCtr ( String name ) {
        super ( BaseTestPluginImpl.NAME_CONSTRUCTOR);
    }

    public TestPluginNameCtr ( ) {
        super ( BaseTestPluginImpl.DEFAULT_CONSTRUCTOR );
    }
}
