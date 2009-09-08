package net.javacoding.jspider.core.util.config;

import net.javacoding.jspider.core.util.config.properties.PropertiesConfiguration;

/**
 * $Id: ConfigurationFactory.java,v 1.3 2003/04/03 15:57:22 vanrogu Exp $
 */
public class ConfigurationFactory {

    public static final String CONFIG_DEFAULT = "default";
    public static final String CONFIG_UNITTEST = "unittest";
    public static final String CONFIG_TOOL = "tool";

    protected static JSpiderConfiguration instance;

    public static synchronized JSpiderConfiguration getConfiguration ( ) {
        if ( instance == null ) {
            instance = new PropertiesConfiguration ( );
        }
        return instance;
    }

    public static synchronized JSpiderConfiguration getConfiguration ( String configuration ) {

        if ( instance == null ) {
          instance = new PropertiesConfiguration ( configuration );
        }
        return instance;
    }

    public static synchronized JSpiderConfiguration setConfiguration ( JSpiderConfiguration configuration ) {
        instance = configuration;
        return instance;
    }

    public static synchronized void cleanConfiguration ( ) {
        instance = null;
    }

}
