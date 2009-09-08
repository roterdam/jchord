package net.javacoding.jspider.core.util.config;

import net.javacoding.jspider.api.model.Site;

import java.io.File;

/**
 * $Id: JSpiderConfiguration.java,v 1.6 2003/04/10 16:19:16 vanrogu Exp $
 */
public interface JSpiderConfiguration {

    public File getDefaultOutputFolder ( );

    public PropertySet getJSpiderConfiguration ( );

    public PropertySet getPluginsConfiguration ( );

    public PropertySet getPluginConfiguration ( String pluginName );

    public PropertySet getSiteConfiguration ( Site site );

    public PropertySet getSiteConfiguration ( String host, int port );

    public PropertySet getDefaultSiteConfiguration ( );

    public PropertySet getBaseSiteConfiguration();

    public File getPluginConfigurationFolder(String pluginName);

}
