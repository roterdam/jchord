package net.javacoding.jspider.mod.plugin.filewriter;

import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.mod.plugin.FlatOutputPlugin;

import java.io.*;
import java.util.Date;


/**
 *
 * $Id: FileWriterPlugin.java,v 1.7 2003/04/02 20:55:37 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class FileWriterPlugin extends FlatOutputPlugin {

    public static final String MODULE_NAME = "File writer JSpider plugin";
    public static final String MODULE_VERSION = "v1.0";
    public static final String MODULE_DESCRIPTION = "A simple JSpider module that writes down all jobs carried out by the JSpider in a file";
    public static final String MODULE_VENDOR = "http://www.javacoding.net";

    public static final String FILENAME = "filename";
    public static final String DEFAULT_FILENAME = "filewriter.out";

    protected Log log;

    protected String fileName;

    protected PrintWriter pw;

    public FileWriterPlugin ( PropertySet config ) {
        log = LogFactory.getLog ( FileWriterPlugin.class );
        fileName = config.getString(FILENAME, DEFAULT_FILENAME );
        log.info("Writing to file: " + fileName );
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

    protected void setUp() {
        try {
          pw = new PrintWriter ( new OutputStreamWriter ( new FileOutputStream(new File(ConfigurationFactory.getConfiguration().getDefaultOutputFolder(),fileName) ) ) );
          log.debug("Opened file : " + fileName );
        } catch (FileNotFoundException e) {
          log.error("Error opening file " + fileName, e );
        }
    }

    protected void println ( Object object ) {
        pw.println ( "[" + new Date ( ) + "] " + object );
    }

    protected void tearDown() {
        log.debug("Closing file...");
        pw.close ( );
        log.debug("Shutdown.");
    }
}
