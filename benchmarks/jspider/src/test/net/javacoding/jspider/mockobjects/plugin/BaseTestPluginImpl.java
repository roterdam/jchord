package net.javacoding.jspider.mockobjects.plugin;

import net.javacoding.jspider.spi.Plugin;
import net.javacoding.jspider.api.event.JSpiderEvent;

/**
 * $Id: BaseTestPluginImpl.java,v 1.2 2003/04/03 16:25:23 vanrogu Exp $
 */
public class BaseTestPluginImpl implements Plugin {

    public static final int DEFAULT_CONSTRUCTOR = 1;
    public static final int NAME_CONSTRUCTOR = 2;
    public static final int CONFIG_CONSTRUCTOR = 3;
    public static final int NAMECONFIG_CONSTRUCTOR = 3;

    protected int usedConstructor;

    public BaseTestPluginImpl ( int usedConstructor ) {
      this.usedConstructor = usedConstructor;
    }

    public String getName() {
        return null;
    }

    public String getVersion() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public String getVendor() {
        return null;
    }

    public void initialize() {
    }

    public void shutdown() {
    }

    public void notify(JSpiderEvent event) {
    }

    public int getUsedConstuctor ( ) {
        return usedConstructor;
    }
}
