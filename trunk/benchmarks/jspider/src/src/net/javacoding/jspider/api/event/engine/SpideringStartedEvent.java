package net.javacoding.jspider.api.event.engine;


import net.javacoding.jspider.api.event.EventVisitor;

import java.net.URL;


/**
 *
 * $Id: SpideringStartedEvent.java,v 1.2 2002/12/20 22:22:31 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SpideringStartedEvent extends EngineRelatedEvent {

    protected URL baseURL;

    public SpideringStartedEvent(URL baseURL) {
        this.baseURL = baseURL;
    }

    public boolean isFilterable() {
        return false;
    }

    public String getComment() {
        return "Spidering started at '" + baseURL + "'";
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

}
