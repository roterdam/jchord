package net.javacoding.jspider.mockobjects.plugin;

import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.spi.Plugin;

/**
 * $Id: UnitTestPlugin.java,v 1.5 2003/04/03 16:25:24 vanrogu Exp $
 */
public class UnitTestPlugin implements Plugin {

    protected JUnitEventSink eventSink;

    public UnitTestPlugin ( ) {
        eventSink = JUnitEventSink.getInstance();
    }

    public String getName() {
        return "JUnitTest Plugin";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "Enables JUnit tests with JSpider";
    }

    public String getVendor() {
        return "http://www.javacoding.net";
    }

    public void notify(JSpiderEvent event) {
      eventSink.notify(event);
    }

    public void initialize() {
    }

    public void shutdown() {
    }
}
