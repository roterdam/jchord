package net.javacoding.jspider.core.impl;

import net.javacoding.jspider.core.dispatch.impl.PluginSocket;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.util.config.*;
import net.javacoding.jspider.spi.Plugin;

import java.util.ArrayList;

/**
 *
 * $Id: PluginFactory.java,v 1.9 2003/04/22 16:43:34 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class PluginFactory {

    PluginInstantiator pluginInstantiator;


    public PluginFactory() {
        pluginInstantiator = new PluginInstantiator();
    }

    public Plugin[] createPlugins() {

        Log log = LogFactory.getLog(PluginFactory.class);
        ArrayList loadedPlugins = new ArrayList();

        PropertySet props = ConfigurationFactory.getConfiguration().getPluginsConfiguration();
        PropertySet pluginsProps = new MappedPropertySet(ConfigConstants.PLUGINS,props);
        int pluginCount = pluginsProps.getInteger(ConfigConstants.PLUGINS_COUNT, 0);
        log.info("Loading " + pluginCount + " plugins.");

        for (int i = 0; i < pluginCount; i++) {
            String pluginInstance = pluginsProps.getString( "" + (i+1) + "." + ConfigConstants.PLUGINS_CONFIG, null);
            if (pluginInstance != null) {
                log.info("Loading plugin configuration '" + pluginInstance + "'...");
                PropertySet config = ConfigurationFactory.getConfiguration().getPluginConfiguration(pluginInstance);
                PropertySet pluginConfig = new MappedPropertySet(ConfigConstants.PLUGIN, config);
                Class pluginClass = pluginConfig.getClass(ConfigConstants.PLUGIN_CLASS, null);
                if (pluginClass == null) {
                    log.info("Plugin class '" + pluginConfig.getString(ConfigConstants.PLUGIN_CLASS, "") + "' not found");
                } else {
                    PropertySet pluginParams = new MappedPropertySet(ConfigConstants.PLUGIN_CONFIG, pluginConfig);
                    Plugin plugin = pluginInstantiator.instantiate(pluginClass, pluginInstance, pluginParams);

                    PropertySet filterConfig = new MappedPropertySet(ConfigConstants.PLUGIN_FILTER, pluginConfig);
                    if (filterConfig.getBoolean(ConfigConstants.PLUGIN_FILTER_ENABLED, false)) {

                        log.info("Plugin uses local event filtering");
                        loadedPlugins.add(new PluginSocket(plugin, filterConfig));
                    } else {
                        log.info("Plugin not configured for local event filtering");
                        loadedPlugins.add(plugin);
                    }

                    log.info("Plugin Name    : " + plugin.getName());
                    log.info("Plugin Version : " + plugin.getVersion());
                    log.info("Plugin Vendor  : " + plugin.getVendor());
                }
            } else {
                log.info("Plugin configuration '" + pluginInstance + "' couldn't be loaded");
            }
        }

        log.info("Loaded " + loadedPlugins.size() + " plugins.");
        return (Plugin[]) loadedPlugins.toArray(new Plugin[loadedPlugins.size()]);
    }
}
