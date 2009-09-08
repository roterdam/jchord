package net.javacoding.jspider.api.event.resource;

import net.javacoding.jspider.api.model.Resource;

/**
 * $Id: MalformedURLFoundEvent.java,v 1.2 2003/04/08 15:50:25 vanrogu Exp $
 */
public class MalformedURLFoundEvent extends ResourceRelatedEvent {

    protected String malformedURL;

    public MalformedURLFoundEvent ( Resource resource, String malformedURL ) {
        super ( resource );
        this.malformedURL = malformedURL;
    }

    public String getComment() {
        return "malformed url found in " + resource.getURL() + ": " + malformedURL;
    }

    public String getMalformedURL ( ) {
        return malformedURL;
    }

    public Resource getResource ( ) {
        return resource;
    }

    public String toString ( ) {
        return getComment();
    }

}
