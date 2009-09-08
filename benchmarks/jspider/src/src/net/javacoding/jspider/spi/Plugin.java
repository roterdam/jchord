package net.javacoding.jspider.spi;

import net.javacoding.jspider.api.event.EventSink;


/**
 * Interface that must be implemented by a JSpider module.  This interface
 * allows interaction between the JSpider core and the module.
 *
 * $Id: Plugin.java,v 1.1 2003/04/03 16:25:22 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public interface Plugin extends EventSink {

    /**
     * Returns the name of the module.
     * @return name of the module
     */
    public String getName();


    /**
     * Returns the version string of the module.
     * @return version string of the module
     */
    public String getVersion();


    /**
     * Returns a description of the module.
     * @return description of the module
     */
    public String getDescription();


    /**
     * Returns the vednor of the module.
     * @return the vendor of the module
     */
    public String getVendor();

}
