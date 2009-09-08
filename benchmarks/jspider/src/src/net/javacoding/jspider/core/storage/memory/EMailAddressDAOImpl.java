package net.javacoding.jspider.core.storage.memory;

import net.javacoding.jspider.core.storage.spi.EMailAddressDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.*;

import java.util.*;

/**
 * $Id: EMailAddressDAOImpl.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 */
class EMailAddressDAOImpl implements EMailAddressDAOSPI {

    protected StorageSPI storage;

    protected Map byResource;
    protected Map addresses;

    public EMailAddressDAOImpl ( StorageSPI storage ) {
        this.storage = storage;
        this.byResource= new HashMap();
        this.addresses = new HashMap ();
    }

    public void register(ResourceInternal resource, EMailAddressInternal address) {
        Map map = (Map)byResource.get(new Integer(resource.getId()));
        if (map == null) {
            map = new HashMap();
            byResource.put(new Integer(resource.getId()), map);
        }

        EMailAddressReferenceInternal reference =(EMailAddressReferenceInternal) map.get(address.getAddress());
        if ( reference == null ) {
            reference = new EMailAddressReferenceInternal(storage, resource.getId(), address.getAddress(), 0);
            map.put(address.getAddress(), reference);
        }
        reference.incrementCount();
        addresses.put(address.getAddress(), address);
    }

    public EMailAddressInternal[] findByResource(ResourceInternal resource) {
        EMailAddressReferenceInternal[] refs = findReferencesByResource(resource);
        ArrayList al = new ArrayList();
        for (int i = 0; i < refs.length; i++) {
            EMailAddressReferenceInternal ref = refs[i];
            al.add(ref.getEMailAddress());
        }
        return (EMailAddressInternal[])al.toArray(new EMailAddressInternal[al.size()]);
    }

    public EMailAddressReferenceInternal[] findReferencesByResource(ResourceInternal resource) {
        Map map = (Map)byResource.get(new Integer(resource.getId()));
        if (map == null) {
            return new EMailAddressReferenceInternal[0];
        } else {
            return (EMailAddressReferenceInternal[]) map.values().toArray(new EMailAddressReferenceInternal[map.size()]);
        }
    }

    public EMailAddressInternal find(String address) {
        return (EMailAddressInternal) addresses.get(address);
    }
}
