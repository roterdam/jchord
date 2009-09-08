package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.FetchIgnoredResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceIgnoredForFetchingEvent.java,v 1.1.1.1 2002/11/20 17:02:31 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceIgnoredForFetchingEvent extends ResourceRelatedEvent {

    public ResourceIgnoredForFetchingEvent(Resource resource) {
        super(resource);
    }

    public FetchIgnoredResource getResource() {
        return (FetchIgnoredResource) resource;
    }

    public String getComment() {
        return resource.getURL() + " ignored for fetching";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

}
