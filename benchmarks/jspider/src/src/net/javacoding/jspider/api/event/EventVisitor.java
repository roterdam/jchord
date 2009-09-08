package net.javacoding.jspider.api.event;


import net.javacoding.jspider.api.event.engine.*;
import net.javacoding.jspider.api.event.folder.FolderDiscoveredEvent;
import net.javacoding.jspider.api.event.folder.FolderRelatedEvent;
import net.javacoding.jspider.api.event.resource.*;
import net.javacoding.jspider.api.event.site.*;


/**
 * Visitor interface that must be implemented upon each class that will act as
 * a visitor in the applied visitor pattern for event handling.
 * This interface contains a visit method for each JSpider API event type.
 *
 * $Id: EventVisitor.java,v 1.6 2003/04/08 15:50:24 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public interface EventVisitor {


    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(JSpiderEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(EngineRelatedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(SpideringStartedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(SpideringStoppedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(FolderRelatedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(FolderDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceRelatedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(EMailAddressDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(EMailAddressReferenceDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(MalformedURLFoundEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(MalformedBaseURLFoundEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceFetchedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceFetchErrorEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceForbiddenEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceParsedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceIgnoredForFetchingEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceIgnoredForParsingEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(ResourceReferenceDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(SiteRelatedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(SiteDiscoveredEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(RobotsTXTMissingEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(RobotsTXTFetchedEvent event);

    /**
     * Visit method.
     * @param event that occured
     */
    public void visit(UserAgentObeyedEvent event);

}
