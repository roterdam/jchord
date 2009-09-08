package net.javacoding.jspider.mod.plugin;

import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.event.engine.*;
import net.javacoding.jspider.api.event.folder.FolderDiscoveredEvent;
import net.javacoding.jspider.api.event.folder.FolderRelatedEvent;
import net.javacoding.jspider.api.event.resource.*;
import net.javacoding.jspider.api.event.site.*;
import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.spi.Plugin;

/**
 * $Id: FlatOutputPlugin.java,v 1.12 2003/04/08 15:50:38 vanrogu Exp $
 */
public abstract class FlatOutputPlugin implements Plugin, EventVisitor {

    public void visit(JSpiderEvent event) {
        println (event);
    }

    public void visit(EngineRelatedEvent event) {
        println (event);
    }

    public void visit(SpideringStartedEvent event) {
        println("Module : " + getName ( ) );
        println("Version: " + getVersion( ) );
        println("Vendor : " + getVendor ( ) );
        println("Spidering Started, baseURL = " + event.getBaseURL());
    }

    public final void initialize() {
        setUp ( );
    }

    public void shutdown() {
        tearDown ( );
    }

    public void visit(SpideringStoppedEvent event) {
        println ("Spidering Stopped");
    }

    public void visit(FolderRelatedEvent event) {
        println (event);
    }

    public void visit(FolderDiscoveredEvent event) {
        println (event);
    }

    public void visit(ResourceRelatedEvent event) {
        println (event);
    }

    public void visit(EMailAddressDiscoveredEvent event) {
        println(event);
    }

    public void visit(EMailAddressReferenceDiscoveredEvent event) {
        println(event);
    }

    public void visit(MalformedURLFoundEvent event) {
        println(event);
    }

    public void visit(MalformedBaseURLFoundEvent event) {
        println(event);
    }

    public void visit(ResourceDiscoveredEvent event) {
       println (event.getComment());
    }

    public void visit(ResourceFetchedEvent event) {
        FetchedResource resource = event.getResource();
        println ( resource.getHttpStatus() + " - " + resource.getURL() + " - " + resource.getMime() + " " + resource.getSize() + " " + resource.getTimeMs() + " ms");
    }

    public void visit(ResourceFetchErrorEvent event) {
        FetchErrorResource resource = event.getResource();
        println ( resource.getHttpStatus() + " - ERROR !!!" + resource.getURL());
    }

    public void visit(ResourceForbiddenEvent event) {
        println (event);
    }

    public void visit(ResourceParsedEvent event) {
        println (event);
    }

    public void visit(ResourceIgnoredForFetchingEvent event) {
        FetchIgnoredResource resource = event.getResource();
        println ( resource.getURL() + " - Ignored for fetching");
    }

    public void visit(ResourceIgnoredForParsingEvent event) {
        ParseIgnoredResource resource = event.getResource();
        println ( resource.getURL() + " - Ignored for parsing");
    }

    public void visit(ResourceReferenceDiscoveredEvent event) {
        /*
        println ( "resource reference discovered :" );
        println ( "  from : " + event.getResource().getURL() );
        println ( "  to   : " + event.getReferencedResource().getURL() );
        */
    }

    public void visit(SiteRelatedEvent event) {
       println ( event);
    }

    public void visit(SiteDiscoveredEvent event) {
        println ( "site discovered : " + event.getSite().getURL() );
    }

    public void visit(RobotsTXTMissingEvent event) {
        println ( "robots.txt missing for site " + event.getSite());
    }

    public void visit(RobotsTXTFetchedEvent event) {
        println ( "robots.txt fetched from site " + event.getSite());
    }

    public void visit(UserAgentObeyedEvent event) {
        println ( event.getComment());
    }

    public void notify(JSpiderEvent event) {
        event.accept(this);
    }


    protected abstract void println ( Object object );

    protected void setUp ( ) {
    }

    protected void tearDown ( ) {
    }

}
