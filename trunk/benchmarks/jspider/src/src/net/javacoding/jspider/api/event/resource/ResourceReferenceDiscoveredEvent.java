package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.FetchedResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceReferenceDiscoveredEvent.java,v 1.1.1.1 2002/11/20 17:02:32 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceReferenceDiscoveredEvent extends ResourceRelatedEvent {

    protected Resource targetResource;

    public ResourceReferenceDiscoveredEvent(Resource resource, Resource targetResource) {
        super(resource);
        this.targetResource = targetResource;
    }

    public FetchedResource getResource() {
        return (FetchedResource) resource;
    }

    public Resource getReferencedResource() {
        return targetResource;
    }

    public String getComment() {
        return "resource " + resource + " references " + targetResource;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
