package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.core.model.SiteInternal;

import java.net.URL;

/**
 * $Id: SiteDAOSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 */
public interface SiteDAOSPI {

    public SiteInternal find ( URL siteURL );

    public SiteInternal find ( int id );

    public void create ( int id, SiteInternal site );

    public void save ( int is, SiteInternal site );

    public SiteInternal[] findAll( );

}
