package net.javacoding.jspider.core.storage;

import net.javacoding.jspider.api.model.Site;

import java.net.URL;

/**
 * $Id: SiteDAO.java,v 1.2 2003/04/11 16:37:05 vanrogu Exp $
 */
public interface SiteDAO {

    public Site createSite ( URL siteURL );

    public Site find ( URL siteURL );

    public void save ( Site site );

    public Site[] findAll( );

}
