package net.javacoding.jspider.api.event.site;

import net.javacoding.jspider.api.model.Site;

/**
 * $Id: RobotsTXTSkippedEvent.java,v 1.2 2003/03/28 17:26:27 vanrogu Exp $
 */
public class RobotsTXTSkippedEvent extends SiteRelatedEvent {

    public RobotsTXTSkippedEvent ( Site site ) {
        super (site);
    }

    public Site getSite( ) {
        return site;
    }

    public String getComment() {
        return "RobotsTXTSkippedEvent for site " + site;
    }
}
