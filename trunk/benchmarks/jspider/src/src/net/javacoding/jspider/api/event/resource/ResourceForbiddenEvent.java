package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.ForbiddenResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceForbiddenEvent.java,v 1.1.1.1 2002/11/20 17:02:31 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceForbiddenEvent extends ResourceRelatedEvent {

    public ResourceForbiddenEvent(Resource resource) {
        super(resource);
    }

    public ForbiddenResource getResource() {
        return (ForbiddenResource) resource;
    }

    public String getComment() {
        return resource.getURL() + " is forbidden";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
