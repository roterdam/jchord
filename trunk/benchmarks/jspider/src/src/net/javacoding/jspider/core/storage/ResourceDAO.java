package net.javacoding.jspider.core.storage;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;

import java.net.URL;

/**
 * $Id: ResourceDAO.java,v 1.10 2003/04/11 16:37:05 vanrogu Exp $
 */
public interface ResourceDAO {

    public Resource registerURL(URL url);

    public void registerURLReference (URL url, URL referer);

    public Resource[] getAllResources();

    public Resource[] getRefereringResources(Resource resource);

    public Resource[] getReferencedResources(Resource resource);

    public ResourceReference[] getIncomingReferences(Resource resource);

    public ResourceReference[] getOutgoingReferences(Resource resource);

    public Resource[] getRootResources(Site site);

    public Resource[] getBySite(Site site);

    public Resource[] findByFolder (Folder folder);

    public Resource getResource(URL url);

    public void setSpidered(URL url, URLSpideredOkEvent event);

    public void setIgnoredForParsing(URL url) throws InvalidStateTransitionException;

    public void setIgnoredForFetching(URL url, URLFoundEvent event) throws InvalidStateTransitionException;

    public void setForbidden(URL url, URLFoundEvent event) throws InvalidStateTransitionException;

    public void setError(URL url, ResourceParsedErrorEvent event) throws InvalidStateTransitionException;

    public void setParsed(URL url, ResourceParsedOkEvent event) throws InvalidStateTransitionException;

    public void setError(URL url, URLSpideredErrorEvent event) throws InvalidStateTransitionException;

}
