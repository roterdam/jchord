package net.javacoding.jspider.api.event.resource;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.event.JSpiderEvent;
import net.javacoding.jspider.api.model.Resource;

import java.net.URL;


/**
 *
 * $Id: ResourceRelatedEvent.java,v 1.3 2003/04/07 15:50:46 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public abstract class ResourceRelatedEvent extends JSpiderEvent {

    protected Resource resource;

    public ResourceRelatedEvent(Resource resource) {
        this.resource = resource;
    }

    public URL getURL() {
        return resource.getURL();
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public String toString() {
        return super.toString() + " " + getComment();
    }

}
