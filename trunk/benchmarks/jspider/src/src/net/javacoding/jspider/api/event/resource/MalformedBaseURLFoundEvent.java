package net.javacoding.jspider.api.event.resource;

import net.javacoding.jspider.api.model.FetchedResource;
import net.javacoding.jspider.api.event.EventVisitor;

/**
 * $Id: MalformedBaseURLFoundEvent.java,v 1.2 2003/04/08 15:50:25 vanrogu Exp $
 */
public class MalformedBaseURLFoundEvent extends ResourceRelatedEvent {

    protected String malformedBaseURL;

    public MalformedBaseURLFoundEvent ( FetchedResource resource, String malformedBaseURL ) {
        super ( resource );
        this.malformedBaseURL = malformedBaseURL;
    }

    public FetchedResource getResource ( ) {
        return (FetchedResource) resource;
    }

    public String getComment() {
        return "malformed baseURL found in resource '" + resource.getURL() + "' : '" + malformedBaseURL + "'";
    }

    public String getMalformedBaseURL ( ) {
        return malformedBaseURL;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public String toString ( ) {
        return getComment();
    }
}
