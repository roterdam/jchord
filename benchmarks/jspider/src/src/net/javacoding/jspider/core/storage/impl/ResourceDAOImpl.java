package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.core.storage.ResourceDAO;
import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;
import net.javacoding.jspider.core.storage.spi.ResourceDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.core.util.URLUtil;
import net.javacoding.jspider.api.model.*;

import java.net.URL;
import java.util.Date;

/**
 * $Id: ResourceDAOImpl.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
class ResourceDAOImpl implements ResourceDAO {

    protected int counter;
    protected Log log;
    protected StorageSPI storage;
    protected ResourceDAOSPI spi;

    protected int folderCounter;

    public ResourceDAOImpl  ( Log log, StorageSPI storage, ResourceDAOSPI spi ) {
        this.log = log;
        this.storage = storage;
        this.spi = spi;
        this.counter = 0;
        this.folderCounter = 0;
    }

    public Resource registerURL(URL url) {
        FolderInternal folder = ensureFolders(url);
        int id = ++counter;
        ResourceInternal resource = spi.getResource(url);
        if (resource == null) {
            SiteInternal site = storage.getSiteDAO().find(URLUtil.getSiteURL(url));
            if (site == null) {
                log.warn("unable to register resource " + url + " because Site object wasn't found");
            } else {
                resource = new ResourceInternal(storage, id, site, url, new Date(), folder);
                spi.create(id, resource);
            }
        }
        return resource;
    }

    public void registerURLReference(URL url, URL referer) {
        spi.registerURLReference(url, referer);
    }

    public Resource[] getAllResources() {
        return spi.findAllResources();
    }

    public Resource[] getRefereringResources(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.getRefereringResources(ri);
    }

    public Resource[] getReferencedResources(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.getReferencedResources(ri);
    }

    public ResourceReference[] getIncomingReferences(Resource resource) {
        return new ResourceReference[0];
    }

    public ResourceReference[] getOutgoingReferences(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.getOutgoingReferences(ri);
    }

    public Resource[] getRootResources(Site site) {
        SiteInternal si = (SiteInternal)site;
        return spi.getRootResources(si);
    }

    public Resource[] getBySite(Site site) {
        SiteInternal si = TypeTranslator.translate(site);
        return spi.getBySite(si);
    }

    public Resource[] findByFolder(Folder folder) {
        FolderInternal fi = TypeTranslator.translate(folder);
        return spi.findByFolder(fi);
    }

    public Resource getResource(URL url) {
        return spi.getResource(url);
    }

    public void setSpidered(URL url, URLSpideredOkEvent event) {
        spi.setSpidered(url, event);
    }

    public void setIgnoredForParsing(URL url) throws InvalidStateTransitionException {
        spi.setIgnoredForParsing(url);
    }

    public void setIgnoredForFetching(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        spi.setIgnoredForFetching(url, event);
    }

    public void setForbidden(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        spi.setForbidden(url, event);
    }

    public void setError(URL url, ResourceParsedErrorEvent event) throws InvalidStateTransitionException {
        spi.setError(url, event);
    }

    public void setParsed(URL url, ResourceParsedOkEvent event) throws InvalidStateTransitionException {
        spi.setParsed(url, event);
    }

    public void setError(URL url, URLSpideredErrorEvent event) throws InvalidStateTransitionException {
        spi.setError(url, event);
    }


    protected FolderInternal ensureFolders(URL url) {
        URL siteURL = URLUtil.getSiteURL(url);
        SiteInternal site = storage.getSiteDAO().find(siteURL);

        String[] folderNames = URLUtil.getFolderNames(url);

        FolderInternal folder = null;
        if (folderNames.length > 0) {
            folder = (FolderInternal)site.getRootFolder(folderNames[0]);
            if (folder == null) {
                int id = ++folderCounter;
                folder = storage.getFolderDAO().createFolder(id, site, folderNames[0]);
            }
            if ( folderNames.length > 1 ) {
              folder = ensureRecursively(folder, folderNames, 1);
            }
        }
        return folder;
    }

    protected FolderInternal ensureRecursively(FolderInternal folder, String[] folderNames, int depth) {
        FolderInternal subFolder = (FolderInternal)folder.getFolder(folderNames[depth]);
        if (subFolder == null) {
            int id = ++folderCounter;
            subFolder = storage.getFolderDAO().createFolder(id, folder, folderNames[depth]);
        }
        if ((depth + 1) < folderNames.length) {
            subFolder = ensureRecursively(subFolder, folderNames, depth + 1);
        }
        return subFolder;
    }
}
