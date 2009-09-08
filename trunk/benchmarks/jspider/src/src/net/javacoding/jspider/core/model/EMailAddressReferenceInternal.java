package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.storage.spi.StorageSPI;


/**
 * $Id: EMailAddressReferenceInternal.java,v 1.2 2003/04/11 16:37:03 vanrogu Exp $
 */
public class EMailAddressReferenceInternal implements EMailAddressReference {

    protected StorageSPI storage;
    protected int resource;
    protected String emailAddress;
    protected int count;

    public EMailAddressReferenceInternal ( StorageSPI storage, int resource, String emailAddress, int count ) {
        this.storage = storage;
        this.resource = resource;
        this.emailAddress = emailAddress;
        this.count = count;
    }

    public FetchedResource getResource() {
        return storage.getResourceDAO().getResource(resource) ;
    }

    public EMailAddress getEMailAddress() {
        return storage.getEMailAddressDAO().find(emailAddress);
    }

    public int getCount() {
        return count;
    }

    public void incrementCount ( ) {
        count = count + 1;
    }

}
