package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.FetchedResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceFetchedEvent.java,v 1.2 2002/12/23 17:13:35 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceFetchedEvent extends ResourceRelatedEvent {

    public ResourceFetchedEvent(Resource resource) {
        super(resource);
    }

    public FetchedResource getResource() {
        return (FetchedResource) resource;
    }

    public String getComment() {
        FetchedResource fetchedResource = (FetchedResource) resource;
        return "resource " + resource.getURL() + " fetched [" + fetchedResource.getMime() + "]";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
