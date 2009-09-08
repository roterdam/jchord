package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.storage.spi.StorageSPI;

import java.net.URL;

/**
 * $Id: ResourceReferenceInternal.java,v 1.2 2003/04/11 16:37:04 vanrogu Exp $
 */
public class ResourceReferenceInternal implements ResourceReference {

    protected StorageSPI storage;
    protected URL referer;
    protected URL referee;
    protected int count;

    public ResourceReferenceInternal ( StorageSPI storage, URL referer, URL referee, int count ) {
        this.storage = storage;
        this.referer = referer;
        this.referee = referee;
        this.count = count;
    }

    public FetchedResource getReferer() {
        return storage.getResourceDAO().getResource(referer);
    }

    public Resource getReferee() {
        return storage.getResourceDAO().getResource(referee);
    }

    public int getCount() {
        return count;
    }

    public void incrementCount ( ) {
        count++;
    }

}
