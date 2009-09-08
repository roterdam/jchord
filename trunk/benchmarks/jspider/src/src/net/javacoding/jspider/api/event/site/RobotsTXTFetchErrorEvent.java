/**
 * This source is subject to the LGPL Open Source license.
 */
package net.javacoding.jspider.api.event.site;

import net.javacoding.jspider.api.model.Site;


/**
 *
 * $Id: RobotsTXTFetchErrorEvent.java,v 1.5 2003/03/28 17:26:26 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class RobotsTXTFetchErrorEvent extends SiteRelatedEvent {

    protected Exception exception;

    public RobotsTXTFetchErrorEvent(Site site, Exception exception) {
        super(site);
        this.exception = exception;
    }

    public String getComment() {
        return "robots.txt was unreachable on site '" + site + "'";
    }

    public Site getSite( ) {
        return site;
    }

    public boolean isError() {
        return true;
    }
}
