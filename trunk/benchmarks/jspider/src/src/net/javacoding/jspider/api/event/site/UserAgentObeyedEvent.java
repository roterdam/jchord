package net.javacoding.jspider.api.event.site;


import net.javacoding.jspider.api.model.Site;

/**
 * $Id: UserAgentObeyedEvent.java,v 1.4 2003/03/24 16:58:35 vanrogu Exp $
 */
public class UserAgentObeyedEvent extends SiteRelatedEvent {

    protected String userAgent;

    public UserAgentObeyedEvent ( Site site, String userAgent ) {
        super(site);
        this.userAgent = userAgent;
    }

    public String getComment() {
        return "obeyed rules for useragent '" + userAgent + "' as found in robots.txt on site " + site.getURL();
    }

    public Site getSite ( ) {
        return site;
    }

    public String getUserAgent ( ) {
        return userAgent;
    }
}
