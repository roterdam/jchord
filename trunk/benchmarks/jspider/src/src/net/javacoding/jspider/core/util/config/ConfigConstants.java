package net.javacoding.jspider.core.util.config;

/**
 * $Id: ConfigConstants.java,v 1.4 2003/04/22 16:43:34 vanrogu Exp $
 */
public interface ConfigConstants {

    public static final String JSPIDER = "jspider";

    public static final String CONFIG_PROXY = "jspider.proxy";

    public static final String CONFIG_THREADING = "jspider.threads";
    public static final String CONFIG_THREADING_THINKERS = "thinkers";
    public static final String CONFIG_THREADING_SPIDERS = "spiders";
    public static final String CONFIG_THREADING_MONITORING = "monitoring";
    public static final String CONFIG_THREADING_MONITORING_ENABLED = "enabled";
    public static final String CONFIG_THREADING_MONITORING_INTERVAL = "interval";
    public static final String CONFIG_THREADING_COUNT = "count";

    public static final String CONFIG_PROXY_USE = "use";
    public static final String CONFIG_PROXY_HOST = "host";
    public static final String CONFIG_PROXY_PORT = "port";
    public static final String CONFIG_PROXY_AUTHENTICATE = "authenticate";
    public static final String CONFIG_PROXY_USERNAME = "user";
    public static final String CONFIG_PROXY_PASSWORD = "password";

    public static final String CONFIG_STORAGE = "jspider.storage";
    public static final String CONFIG_STORAGE_PROVIDER = "provider";
    public static final String CONFIG_STORAGE_CONFIG = "config";

    public static final String CONFIG_LOG = "jspider.log";
    public static final String CONFIG_LOG_PROVIDER = "provider";

    public static final String CONFIG_SCHEDULER = "jspider.taskscheduler";
    public static final String CONFIG_SCHEDULER_PROVIDER = "provider";
    public static final String CONFIG_SCHEDULER_MONITORING = "monitoring";
    public static final String CONFIG_SCHEDULER_MONITORING_ENABLED = "enabled";
    public static final String CONFIG_SCHEDULER_MONITORING_INTERVAL = "interval";

    public static final String CONFIG_FILTER = "jspider.filter";

    public static final String CONFIG_USERAGENT = "jspider.userAgent";

    public static final String SITES_BASE_SITE = "jspider.site.config.base";
    public static final String SITES_DEFAULT_SITE = "jspider.site.config.default";

    public static final String SITE = "site";

    public static final String SITE_USERAGENT = "site.userAgent";
    public static final String SITE_THROTTLE = "site.throttle";
    public static final String SITE_THROTTLE_CONFIG = "config";
    public static final String SITE_THROTTLE_PROVIDER = "provider";
    public static final String SITE_HANDLE = "site.handle";
    public static final String SITE_COOKIES_USE = "site.cookies.use";
    public static final String SITE_PROXY_USE = "site.proxy.use";
    public static final String SITE_ROBOTSTXT_FETCH = "site.robotstxt.fetch";
    public static final String SITE_ROBOTSTXT_OBEY = "site.robotstxt.obey";

    public static final String PLUGINS = "jspider.plugin";
    public static final String PLUGINS_COUNT = "count";
    public static final String PLUGINS_CONFIG = "config";

    public static final String PLUGIN = "plugin";
    public static final String PLUGIN_CLASS = "class";
    public static final String PLUGIN_CONFIG = "config";
    public static final String PLUGIN_FILTER = "filter";
    public static final String PLUGIN_FILTER_ENABLED = "enabled";

    public static final String FILTER_ENABLED = "enabled";
    public static final String FILTER_ENGINE = "engine";
    public static final String FILTER_MONITORING= "monitoring";
    public static final String FILTER_SPIDER = "spider";

    public static final String RULES = "rules";
    public static final String RULES_COUNT = "count";

    public static final String RULE_CLASS = "class";
    public static final String RULE_CONFIG = "config";
}
