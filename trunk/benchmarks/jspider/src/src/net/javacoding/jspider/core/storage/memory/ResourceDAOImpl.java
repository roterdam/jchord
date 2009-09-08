package net.javacoding.jspider.core.storage.memory;

import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.core.storage.spi.ResourceDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;
import net.javacoding.jspider.core.util.URLUtil;

import java.net.URL;
import java.util.*;

/**
 * $Id: ResourceDAOImpl.java,v 1.12 2003/04/11 16:37:07 vanrogu Exp $
 */
class ResourceDAOImpl implements ResourceDAOSPI {

    protected StorageSPI storage;

    protected Map knownURLs;
    protected Map byId;

    protected Set spideredResources; /* urls visited by a spider, but not yet parsed */

    protected Set ignoredForFetchingResources; /* urls ignored because of rule decisions */
    protected Set ignoredForParsingResources; /* urls ignored because non-HTML */
    protected Set forbiddenResources; /* forbidden urls */
    protected Set fetchErrorResources; /* urls that could not be visited by the spider */
    protected Set parseErrorResources; /* resources that could not be parsed correctly */
    protected Set parsedResources; /* urls that were spidered AND interpreted */

    protected Map referers;
    protected Map referees;

    protected Map byFolder;
    protected Map rootResources;

    public ResourceDAOImpl(StorageSPI storage) {
        this.storage = storage;
        spideredResources = new HashSet();
        ignoredForFetchingResources = new HashSet();
        ignoredForParsingResources = new HashSet();
        forbiddenResources = new HashSet();
        fetchErrorResources = new HashSet();
        parseErrorResources = new HashSet();
        parsedResources = new HashSet();
        knownURLs = new HashMap();
        this.byId = new HashMap();
        this.referees = new HashMap();
        this.referers = new HashMap();
        this.byFolder = new HashMap();
        this.rootResources = new HashMap();
    }

    public void create(int id, ResourceInternal resource) {
        URL url = resource.getURL();
            knownURLs.put(url, resource);
            byId.put(new Integer(id), resource);

            if (resource.getFolder() == null) {
                Set set = (Set) rootResources.get(URLUtil.getSiteURL(url));
                if (set == null) {
                    set = new HashSet();
                    rootResources.put(URLUtil.getSiteURL(url), set);
                }
                set.add(resource);
            } else {
                Set set = (Set) byFolder.get(resource.getFolder());
                if (set == null) {
                    set = new HashSet();
                    byFolder.put(resource.getFolder(), set);
                }
                set.add(resource);
            }
    }

    public void registerURLReference(URL url, URL refererURL) {
        ResourceInternal resource = (ResourceInternal) knownURLs.get(url);
        if (refererURL != null) {
            ResourceInternal referer = (ResourceInternal) knownURLs.get(refererURL);
            storeRef(referers, resource, referer, refererURL, url);
            storeRef(referees, referer, resource, refererURL, url);
        }
    }

    public ResourceInternal[] findByFolder(FolderInternal folder) {
        Set set = (Set) byFolder.get(folder);
        if (set == null) {
            return new ResourceInternal[0];
        }
        return (ResourceInternal[]) set.toArray(new ResourceInternal[set.size()]);
    }

    protected void storeRef(Map map, ResourceInternal key, ResourceInternal data, URL referer, URL referee) {
        Map refmap = (Map) map.get(key.getURL());
        if (refmap == null) {
            refmap = new HashMap();
            map.put(key.getURL(), refmap);
        }
        ResourceReferenceInternal rri = (ResourceReferenceInternal) refmap.get(data.getURL());
        if (rri == null) {
            rri = new ResourceReferenceInternal(storage, referer, referee, 0);
            refmap.put(data.getURL(), rri);
        }
        rri.incrementCount();
    }

    public ResourceInternal[] findAllResources() {
        return (ResourceInternal[]) knownURLs.values().toArray(new ResourceInternal[knownURLs.size()]);
    }

    public ResourceInternal[] getRefereringResources(ResourceInternal resource) {
        ResourceReferenceInternal[] refs = getIncomingReferences(resource);
        ArrayList al = new ArrayList();
        for (int i = 0; i < refs.length; i++) {
            ResourceReferenceInternal ref = refs[i];
            al.add(ref.getReferer());
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceReferenceInternal[] getOutgoingReferences(ResourceInternal resource) {
        Map map = (Map) referees.get(resource.getURL());
        if (map == null) {
            return new ResourceReferenceInternal[0];
        } else {
            return (ResourceReferenceInternal[]) map.values().toArray(new ResourceReferenceInternal[map.size()]);
        }
    }

    public ResourceReferenceInternal[] getIncomingReferences(ResourceInternal resource) {
        Map map = (Map) referers.get(resource.getURL());
        if (map == null) {
            return new ResourceReferenceInternal[0];
        } else {
            return (ResourceReferenceInternal[]) map.values().toArray(new ResourceReferenceInternal[map.size()]);
        }
    }

    public ResourceInternal[] getReferencedResources(ResourceInternal resource) {
        ResourceReferenceInternal[] refs = getOutgoingReferences(resource);
        ArrayList al = new ArrayList();
        for (int i = 0; i < refs.length; i++) {
            ResourceReferenceInternal ref = refs[i];
            al.add(ref.getReferee());
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceInternal[] getBySite(SiteInternal site) {
        ArrayList al = new ArrayList();
        Iterator it = knownURLs.keySet().iterator();
        while (it.hasNext()) {
            URL url = (URL) it.next();
            URL siteURL = URLUtil.getSiteURL(url);
            if (site.getURL().equals(siteURL)) {
                al.add(getResource(url));
            }
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceInternal[] getRootResources(SiteInternal site) {
        Set set = (Set) rootResources.get(site.getURL());
        if ( set == null ) {
            return new ResourceInternal[0];
        } else {
            return (ResourceInternal[]) set.toArray(new ResourceInternal[set.size()]);
        }
    }

    public ResourceInternal getResource(int id) {
        return (ResourceInternal)byId.get(new Integer(id));
    }

    public ResourceInternal getResource(URL url) {
        return (ResourceInternal) knownURLs.get(url);
    }

    public synchronized void setSpidered(URL url, URLSpideredOkEvent event) {
        ResourceInternal resource = getResource(url);
        resource.setFetched(event.getHttpStatus(), event.getSize(), event.getTimeMs(), event.getMimeType(), null, event.getHeaders());
        resource.setBytes(event.getBytes());
    }

    public synchronized void setIgnoredForParsing(URL url) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParseIgnored();
        ignoredForParsingResources.add(url);
    }

    public synchronized void setIgnoredForFetching(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setFetchIgnored();
        ignoredForFetchingResources.add(event.getFoundURL());
    }

    public synchronized void setForbidden(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setForbidden();
        forbiddenResources.add(event.getFoundURL());
    }

    public synchronized void setError(URL url, ResourceParsedErrorEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParseError();
        parseErrorResources.add(url);
    }

    public synchronized void setParsed(URL url, ResourceParsedOkEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParsed();
        parsedResources.add(resource);
    }

    public synchronized void setError(URL url, URLSpideredErrorEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setFetchError(event.getHttpStatus(), event.getHeaders());
        fetchErrorResources.add(url);
    }


}
