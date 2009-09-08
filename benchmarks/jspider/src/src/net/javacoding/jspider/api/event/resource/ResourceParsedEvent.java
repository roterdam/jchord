package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.ParsedResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceParsedEvent.java,v 1.2 2003/04/08 15:50:25 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceParsedEvent extends ResourceRelatedEvent {

    public ResourceParsedEvent(Resource resource) {
        super(resource);
    }

    public ParsedResource getResource() {
        return (ParsedResource) resource;
    }

    public String getComment() {
        return resource.getURL() + " parsed (handled)";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public String toString ( ) {
        return getComment();
    }

}
