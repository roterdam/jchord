package net.javacoding.jspider.api.event.site;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Site;


/**
 *
 * $Id: SiteDiscoveredEvent.java,v 1.3 2003/03/23 15:44:48 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class SiteDiscoveredEvent extends SiteRelatedEvent {

    public SiteDiscoveredEvent(Site site) {
        super(site);
    }

    public String getComment() {
        return "site " + site + " discovered";
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

    public Site getSite ( ) {
        return site;
    }

}
