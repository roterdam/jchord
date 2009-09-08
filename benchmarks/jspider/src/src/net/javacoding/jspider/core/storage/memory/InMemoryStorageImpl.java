package net.javacoding.jspider.core.storage.memory;


import net.javacoding.jspider.core.storage.spi.*;


/**
 *
 * $Id: InMemoryStorageImpl.java,v 1.24 2003/04/11 16:37:07 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
class InMemoryStorageImpl implements StorageSPI {

    protected SiteDAOSPI siteDAO;
    protected ResourceDAOSPI resourceDAO;
    protected ContentDAOSPI contentDAO;
    protected DecisionDAOSPI decisionDAO;
    protected CookieDAOSPI cookieDAO;
    protected EMailAddressDAOSPI emailAddressDAO;
    protected FolderDAOSPI folderDAO;

    public InMemoryStorageImpl() {
        siteDAO = new SiteDAOImpl(this);
        resourceDAO = new ResourceDAOImpl(this);
        contentDAO = new ContentDAOImpl(this);
        decisionDAO = new DecisionDAOImpl(this);
        cookieDAO = new CookieDAOImpl(this);
        emailAddressDAO = new EMailAddressDAOImpl(this);
        folderDAO = new FolderDAOImpl(this);
    }

    public FolderDAOSPI getFolderDAO() {
        return folderDAO;
    }

    public SiteDAOSPI getSiteDAO() {
        return siteDAO;
    }

    public ResourceDAOSPI getResourceDAO() {
        return resourceDAO;
    }

    public ContentDAOSPI getContentDAO() {
        return contentDAO;
    }

    public DecisionDAOSPI getDecisionDAO() {
        return decisionDAO;
    }

    public CookieDAOSPI getCookieDAO() {
        return cookieDAO;
    }

    public EMailAddressDAOSPI getEMailAddressDAO() {
        return emailAddressDAO;
    }

}
