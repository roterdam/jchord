package net.javacoding.jspider.tool.plugin;

import net.javacoding.jspider.JSpiderTool;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.spi.Plugin;

/**
 * $Id: ToolPlugin.java,v 1.4 2003/04/03 16:25:22 vanrogu Exp $
 */
public class ToolPlugin implements Plugin {

    public String getName() {
        return "JSpider TOOL Plugin";
    }

    public String getVersion() {
        return "version 1.0";
    }

    public String getDescription() {
        return "Plugin for HTTP tools";
    }

    public String getVendor() {
        return "www.sourceforge.net/projects/j-spider";
    }

    public void initialize() {
        JSpiderTool.tool.initialize();
    }

    public void shutdown() {
        JSpiderTool.tool.shutdown();
    }

    public void notify(JSpiderEvent event) {
        JSpiderTool.tool.notify(event);
    }
}


