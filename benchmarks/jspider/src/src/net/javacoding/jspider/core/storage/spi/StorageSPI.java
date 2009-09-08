package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.api.model.Summary;

/**
 * $Id: StorageSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 * @todo add id-references as much as possible
 */
public interface StorageSPI {

    public SiteDAOSPI getSiteDAO ( );
    public ResourceDAOSPI getResourceDAO ( );
    public ContentDAOSPI getContentDAO ( );
    public DecisionDAOSPI getDecisionDAO ( );
    public CookieDAOSPI getCookieDAO ( );
    public EMailAddressDAOSPI getEMailAddressDAO ( );
    public FolderDAOSPI getFolderDAO ( );

}
