package net.javacoding.jspider.core.util.config.properties;

import net.javacoding.jspider.api.model.Site;
import net.javacoding.jspider.core.util.config.*;

import java.io.File;

/**
 * $Id: PropertiesConfiguration.java,v 1.14 2003/04/10 16:19:16 vanrogu Exp $
 */
public class PropertiesConfiguration implements JSpiderConfiguration {

    protected String configuration;
    protected String jspiderHome;
    protected PropertySet jspiderProperties;
    protected PropertySet pluginsProperties;
    protected PropertySet websitesConfig;
    protected File defaultOutputFolder;

    public PropertiesConfiguration ( ) {
        this ( ConfigurationFactory.CONFIG_DEFAULT );
    }

    public PropertiesConfiguration ( String configuration ) {

        jspiderHome = System.getProperty("jspider.home");
        if ( jspiderHome == null || "".equals(jspiderHome.trim()) || "null".equals(jspiderHome.trim())) {
          jspiderHome = ".";
        }
        defaultOutputFolder = new File ( jspiderHome + File.separator + "output" );
        System.err.println("[Engine] jspider.home=" + jspiderHome );
        System.err.println("[Engine] default output folder=" + defaultOutputFolder );
        System.err.println("[Engine] starting with configuration '" + configuration + "'");

        this.configuration = configuration;

        jspiderProperties = PropertiesFilePropertySet.getInstance ( jspiderHome, configuration, "jspider.properties" );
        pluginsProperties = PropertiesFilePropertySet.getInstance ( jspiderHome, configuration, "plugin.properties" );
        websitesConfig = PropertiesFilePropertySet.getInstance(jspiderHome, configuration, "sites.properties" );
    }

    public File getDefaultOutputFolder() {
        return defaultOutputFolder;
    }

    public PropertySet getJSpiderConfiguration() {
        return jspiderProperties;
    }

    public PropertySet getPluginsConfiguration() {
        return pluginsProperties;
    }

    public PropertySet getPluginConfiguration(String pluginName) {
        return PropertiesFilePropertySet.getInstance(jspiderHome, configuration, "plugins" + File.separator + pluginName + ".properties");
    }

    public File getPluginConfigurationFolder(String pluginName) {
        File jspiderHomeFile = new File (jspiderHome);
        File configFolder  = new File ( jspiderHomeFile,  "conf" );
        File config = new File ( configFolder, configuration );
        File plugins = new File ( config, "plugins" );
        return new File ( plugins, pluginName );
    }

    public PropertySet getSiteConfiguration(Site site) {
        if ( site.isBaseSite() ) {
          return ConfigurationFactory.getConfiguration().getBaseSiteConfiguration();
        } else {
          return getSiteConfiguration(site.getHost(), site.getPort());
        }
    }

    public PropertySet getSiteConfiguration(String host, int port) {
        String matchString = host + ":" + port;
        String configName = null;
        if ( port > 0 ) {
            configName = websitesConfig.getString(matchString, null);
        }
        if ( configName == null ) {
            matchString = host;
            configName = websitesConfig.getString(matchString, websitesConfig.getString(ConfigConstants.SITES_DEFAULT_SITE, "default") );
        }
        return PropertiesFilePropertySet.getInstance(jspiderHome, configuration, "sites" + File.separator + configName + ".properties" );
    }

    public PropertySet getDefaultSiteConfiguration() {
        return PropertiesFilePropertySet.getInstance(jspiderHome, configuration, "sites" + File.separator + websitesConfig.getString(ConfigConstants.SITES_DEFAULT_SITE, "default" ) + ".properties" );
    }

    public PropertySet getBaseSiteConfiguration() {
        return PropertiesFilePropertySet.getInstance(jspiderHome, configuration, "sites" + File.separator + websitesConfig.getString(ConfigConstants.SITES_BASE_SITE, "default" ) + ".properties" );
    }

}
