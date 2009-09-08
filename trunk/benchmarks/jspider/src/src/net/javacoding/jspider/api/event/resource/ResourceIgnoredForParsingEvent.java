package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.ParseIgnoredResource;
import net.javacoding.jspider.api.model.Resource;


/**
 *
 * $Id: ResourceIgnoredForParsingEvent.java,v 1.1.1.1 2002/11/20 17:02:32 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class ResourceIgnoredForParsingEvent extends ResourceRelatedEvent {

    public ResourceIgnoredForParsingEvent(Resource resource) {
        super(resource);
    }

    public ParseIgnoredResource getResource() {
        return (ParseIgnoredResource) resource;
    }

    public String getComment() {
        return resource.getURL() + " ignored for parsing";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

}
