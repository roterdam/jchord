package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.core.event.impl.*;

import java.net.URL;

/**
 * $Id: ResourceDAOSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 */
public interface ResourceDAOSPI {

    public void create ( int id, ResourceInternal resource);

    public void registerURLReference (URL url, URL referer);

    public ResourceInternal[] findAllResources();

    public ResourceInternal[] getRefereringResources(ResourceInternal resource);

    public ResourceInternal[] getReferencedResources(ResourceInternal resource);

    public ResourceReferenceInternal[] getIncomingReferences(ResourceInternal resource);

    public ResourceReferenceInternal[] getOutgoingReferences(ResourceInternal resource);

    public ResourceInternal[] getRootResources(SiteInternal site);

    public ResourceInternal[] getBySite(SiteInternal site);

    public ResourceInternal[] findByFolder (FolderInternal folder);

    public ResourceInternal getResource(int id);

    public ResourceInternal getResource(URL url);

    public void setSpidered(URL url, URLSpideredOkEvent event);

    public void setIgnoredForParsing(URL url) throws InvalidStateTransitionException;

    public void setIgnoredForFetching(URL url, URLFoundEvent event) throws InvalidStateTransitionException;

    public void setForbidden(URL url, URLFoundEvent event) throws InvalidStateTransitionException;

    public void setError(URL url, ResourceParsedErrorEvent event) throws InvalidStateTransitionException;

    public void setParsed(URL url, ResourceParsedOkEvent event) throws InvalidStateTransitionException;

    public void setError(URL url, URLSpideredErrorEvent event) throws InvalidStateTransitionException;

}
