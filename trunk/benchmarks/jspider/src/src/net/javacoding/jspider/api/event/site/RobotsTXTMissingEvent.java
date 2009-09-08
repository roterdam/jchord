package net.javacoding.jspider.api.event.site;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Site;


/**
 *
 * $Id: RobotsTXTMissingEvent.java,v 1.3 2003/03/28 17:26:26 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RobotsTXTMissingEvent extends SiteRelatedEvent {

    public RobotsTXTMissingEvent(Site site) {
        super(site);
    }

    public String getComment() {
        return "robots.txt is missing for site " + site;
    }

    public Site getSite( ) {
        return site;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }

}
