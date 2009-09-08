package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.core.storage.*;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.model.SummaryInternal;
import net.javacoding.jspider.api.model.Summary;
import net.javacoding.jspider.api.model.Resource;

/**
 * $Id: StorageImpl.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
public class StorageImpl implements Storage {

    protected StorageSPI storage;
    protected SiteDAO siteDAO;
    protected ResourceDAO resourceDAO;
    protected DecisionDAO decisionDAO;
    protected CookieDAO cookieDAO;
    protected EMailAddressDAO emailAddressDAO;

    public StorageImpl ( StorageProvider provider, PropertySet props ) {
        this.storage = provider.createStorage(props);
        this.siteDAO = new SiteDAOImpl(LogFactory.getLog(SiteDAO.class),storage, storage.getSiteDAO());
        this.resourceDAO = new ResourceDAOImpl(LogFactory.getLog(ResourceDAO.class),storage, storage.getResourceDAO());
        this.decisionDAO = new DecisionDAOImpl(LogFactory.getLog(DecisionDAO.class),storage, storage.getDecisionDAO());
        this.cookieDAO = new CookieDAOImpl(LogFactory.getLog(CookieDAO.class),storage, storage.getCookieDAO());
        this.emailAddressDAO = new EMailAddressDAOImpl(LogFactory.getLog(EMailAddressDAO.class),storage, storage.getEMailAddressDAO());
    }

    public SiteDAO getSiteDAO() {
        return siteDAO;
    }

    public ResourceDAO getResourceDAO() {
        return resourceDAO;
    }

    public DecisionDAO getDecisionDAO() {
        return decisionDAO;
    }

    public CookieDAO getCookieDAO() {
        return cookieDAO;
    }

    public EMailAddressDAO getEMailAddressDAO() {
        return emailAddressDAO;
    }

    public Summary getSummary() {
        Resource[] resources = getResourceDAO().getAllResources();
        int knownURLs = resources.length;

        int parsedResources = 0;
        int ignoredForFetchingResources = 0;
        int ignoredForParsingResources = 0;
        int fetchErrorResources = 0;
        int parseErrorResources = 0;
        int forbiddenResources = 0;
        int unvisitedURLs = 0;
        for (int i = 0; i < resources.length; i++) {
            Resource resource = resources[i];
            switch ( resource.getState()) {
                case Resource.STATE_DISCOVERED:
                  unvisitedURLs++;
                  break;
                case Resource.STATE_FETCH_ERROR :
                    fetchErrorResources++;
                  break;
                case Resource.STATE_FETCH_IGNORED :
                    ignoredForFetchingResources++;
                  break;
                case Resource.STATE_FETCH_FORBIDDEN :
                   forbiddenResources++;
                  break;
                case Resource.STATE_FETCHED :
                  break;
                case Resource.STATE_PARSE_ERROR :
                  parseErrorResources++;
                  break;
                case Resource.STATE_PARSE_IGNORED :
                ignoredForParsingResources++;
                  break;
                case Resource.STATE_PARSED :
                  parsedResources++;
                  break;
            }
        }
        return new SummaryInternal(knownURLs, parsedResources,ignoredForFetchingResources, ignoredForParsingResources,fetchErrorResources,parseErrorResources, forbiddenResources, unvisitedURLs);
    }

}
