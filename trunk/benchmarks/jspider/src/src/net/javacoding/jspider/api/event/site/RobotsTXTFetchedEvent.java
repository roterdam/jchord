package net.javacoding.jspider.api.event.site;


import net.javacoding.jspider.api.event.EventVisitor;
import net.javacoding.jspider.api.model.Site;


/**
 *
 * $Id: RobotsTXTFetchedEvent.java,v 1.4 2003/03/28 17:26:26 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RobotsTXTFetchedEvent extends SiteRelatedEvent {

    protected String robotsTXT;

    public RobotsTXTFetchedEvent(Site site, String robotsTXT) {
        super(site);
        this.robotsTXT = robotsTXT;
    }

    public String getComment() {
        return "robots.txt fetched from site " + site;
    }

    public String getRobotsTXT ( ) {
        return robotsTXT;
    }

    public Site getSite( ) {
        return site;
    }

    public void accept(EventVisitor visitor) {
        visitor.visit(this);
    }
}
