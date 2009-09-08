package net.javacoding.jspider.mod.plugin.statusbasedfilewriter;

import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.engine.*;
import net.javacoding.jspider.api.event.folder.FolderDiscoveredEvent;
import net.javacoding.jspider.api.event.folder.FolderRelatedEvent;
import net.javacoding.jspider.api.event.resource.*;
import net.javacoding.jspider.api.event.site.*;
import net.javacoding.jspider.api.model.FetchTriedResource;
import net.javacoding.jspider.api.model.Resource;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.spi.Plugin;

import java.io.*;
import java.util.*;

/**
 * $Id: StatusBasedFileWriterPlugin.java,v 1.10 2003/04/08 15:50:38 vanrogu Exp $
 */
public class StatusBasedFileWriterPlugin implements Plugin, EventVisitor {

    public static final String MODULE_NAME = "Status based  Filewriter JSpider plugin";
    public static final String MODULE_VERSION = "v1.0";
    public static final String MODULE_DESCRIPTION = "A JSpider plugin that writes a report file per HTTP status";
    public static final String MODULE_VENDOR = "http://www.javacoding.net";

    protected Log log;

    protected HashMap fileWriters;

    public StatusBasedFileWriterPlugin ( ) {
        log = LogFactory.getLog ( StatusBasedFileWriterPlugin.class );
        fileWriters = new HashMap ( );
        log.info("initialized." );
    }

    public void initialize() {
    }

    public void shutdown() {
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

    public void notify(JSpiderEvent event) {
        event.accept(this);
    }

    public void visit(JSpiderEvent event) {
    }

    public void visit(EngineRelatedEvent event) {
    }

    public void visit(SpideringStartedEvent event) {
    }

    public void visit(SpideringStoppedEvent event) {
        Collection printWriters = fileWriters.values();
        Iterator it = printWriters.iterator();
        while ( it.hasNext() ) {
            PrintWriter pw = (PrintWriter)it.next();
            pw.close();
        }
    }

    public void visit(FolderRelatedEvent event) {
    }

    public void visit(FolderDiscoveredEvent event) {
    }

    public void visit(ResourceRelatedEvent event) {
    }

    public void visit(EMailAddressDiscoveredEvent event) {
    }

    public void visit(EMailAddressReferenceDiscoveredEvent event) {
    }

    public void visit(MalformedURLFoundEvent event) {
    }

    public void visit(MalformedBaseURLFoundEvent event) {
    }

    public void visit(ResourceDiscoveredEvent event) {
    }

    public void visit(ResourceFetchedEvent event) {
        FetchTriedResource resource = event.getResource();
        int state = resource.getHttpStatus();
        writeInFile ( state, resource );
    }

    public void visit(ResourceFetchErrorEvent event) {
        FetchTriedResource resource = event.getResource();
        int state = resource.getHttpStatus();
        writeInFileWithReferer ( state, resource );
    }

    public void visit(ResourceForbiddenEvent event) {
    }

    public void visit(ResourceParsedEvent event) {
    }

    public void visit(ResourceIgnoredForFetchingEvent event) {
    }

    public void visit(ResourceIgnoredForParsingEvent event) {
    }

    public void visit(ResourceReferenceDiscoveredEvent event) {
    }

    public void visit(SiteRelatedEvent event) {
    }

    public void visit(SiteDiscoveredEvent event) {
    }

    public void visit(RobotsTXTMissingEvent event) {
    }

    public void visit(RobotsTXTFetchedEvent event) {
    }

    public void visit(UserAgentObeyedEvent event) {
    }

    protected void writeInFile ( int state, Resource resource ) {
        PrintWriter pw = getFileWriter(state);
        pw.println(resource.getURL());
    }

    protected void writeInFileWithReferer ( int state, Resource resource ) {
        PrintWriter pw = getFileWriter(state);
        pw.println(resource.getURL());
        pw.println("  REFERED BY:");
        Resource[] referers = resource.getReferers();
        for (int i = 0; i < referers.length; i++) {
            Resource referer = referers[i];
            pw.println("  " + referer.getURL() );
        }
    }

    protected PrintWriter getFileWriter ( int state ) {
        try {
            Integer idObject = new Integer ( state );
            PrintWriter retVal = (PrintWriter) fileWriters.get( ( idObject ));
            if ( retVal == null ) {
                log.info("creating file for status '" + state + "'" );
                retVal = new PrintWriter ( new FileOutputStream (new File(ConfigurationFactory.getConfiguration().getDefaultOutputFolder(), state + ".out")));
                log.debug("opened file for status '" + state + "'" );
                fileWriters.put(idObject, retVal);
            }
            return retVal;
        } catch (IOException e) {
            log.error("i/o exception writing file for state " + state, e);
        }
        return null;
    }

}
