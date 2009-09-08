package net.javacoding.jspider.mod.plugin.console;


import net.javacoding.jspider.mod.plugin.FlatOutputPlugin;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;


/**
 *
 * $Id: ConsolePlugin.java,v 1.5 2003/04/02 20:55:35 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ConsolePlugin extends FlatOutputPlugin {

    public static final String MODULE_NAME = "Console writer JSpider module";
    public static final String MODULE_VERSION = "v1.0";
    public static final String MODULE_DESCRIPTION = "A simple JSpider module that writes down all jobs carried out by the JSpider on your console";
    public static final String MODULE_VENDOR = "http://www.javacoding.net";

    public static final String PREFIX = "prefix";
    public static final String ADDSPACE = "addspace";

    public static final String PREFIX_DEFAULT = "[Plugin] ";

    protected String name;
    protected String prefix;
    protected Log log;

    public ConsolePlugin ( String name, PropertySet config ) {
        log = LogFactory.getLog(ConsolePlugin.class);
        this.name = name;
        prefix = config.getString(PREFIX, PREFIX_DEFAULT);
        log.debug("plugin '" + name + "' prefix is '" + prefix + "'" );
        if ( config.getBoolean(ADDSPACE, false)) {
            prefix = prefix + " ";
            log.debug("adding space after prefix");
        }
        log.info("Prefix set to '" + prefix + "'");
    }

    public ConsolePlugin ( ) {
        log = LogFactory.getLog(ConsolePlugin.class);
        prefix = PREFIX_DEFAULT;
        log.info("Prefix set to default: '" + prefix + "'");
    }


    public String getName() {
        return MODULE_NAME;
    }

    public String getVersion() {
        return MODULE_VERSION;
    }

    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    public String getVendor() {
        return MODULE_VENDOR;
    }

    protected void println(Object object) {
        System.out.println(prefix + object);
    }
}
