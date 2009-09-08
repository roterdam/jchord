package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceDiscoveredEvent.java,v 1.2 2002/12/24 18:11:38 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceDiscoveredEvent extends ResourceRelatedEvent {


    public ResourceDiscoveredEvent(Resource resource) {
        super(resource);
    }

    public Resource getResource() {
        return resource;
    }

    public String getComment() {
        return "resource discovered: " + resource.getURL();
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
