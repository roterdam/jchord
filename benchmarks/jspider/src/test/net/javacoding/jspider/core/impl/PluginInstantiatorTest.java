package net.javacoding.jspider.core.impl;

import junit.framework.TestCase;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.mockobjects.plugin.*;

/**
 * $Id: PluginInstantiatorTest.java,v 1.2 2003/04/03 15:57:23 vanrogu Exp $
 */
public class PluginInstantiatorTest extends TestCase {

    protected PluginInstantiator instantiator;
    protected PropertySet config;

    public PluginInstantiatorTest ( ) {
        super ( "PluginInstantiatorTest" );
    }

    protected void setUp() throws Exception {
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
        this.instantiator = new PluginInstantiator();
        this.config = null;
    }

    protected void tearDown() throws Exception {
        this.instantiator = null;
    }

    public void testDefaultConstructor ( ) {
        Class pluginClass = TestPluginDefaultCtr.class;
        BaseTestPluginImpl p = (BaseTestPluginImpl)
                instantiator.instantiate(pluginClass, "testPlugin", config);

        int expected = BaseTestPluginImpl.DEFAULT_CONSTRUCTOR;
        int actual = p.getUsedConstuctor();

        assertEquals("wrong constructor used for plugin instantiation", expected, actual );
    }

    public void testNameConstructor ( ) {
        Class pluginClass = TestPluginNameCtr.class;
        BaseTestPluginImpl p = (BaseTestPluginImpl)
                instantiator.instantiate(pluginClass, "testPlugin", config);

        int expected = BaseTestPluginImpl.NAME_CONSTRUCTOR;
        int actual = p.getUsedConstuctor();

        assertEquals("wrong constructor used for plugin instantiation", expected, actual );
    }

    public void testConfigConstructor ( ) {
        Class pluginClass = TestPluginConfigCtr.class;
        BaseTestPluginImpl p = (BaseTestPluginImpl)
                instantiator.instantiate(pluginClass, "testPlugin", config);

        int expected = BaseTestPluginImpl.CONFIG_CONSTRUCTOR;
        int actual = p.getUsedConstuctor();

        assertEquals("wrong constructor used for plugin instantiation", expected, actual );
    }

    public void testNameConfigConstructor ( ) {
        Class pluginClass = TestPluginNameConfigCtr.class;
        BaseTestPluginImpl p = (BaseTestPluginImpl)
                instantiator.instantiate(pluginClass, "testPlugin", config);

        int expected = BaseTestPluginImpl.NAMECONFIG_CONSTRUCTOR;
        int actual = p.getUsedConstuctor();

        assertEquals("wrong constructor used for plugin instantiation", expected, actual );
    }

}
