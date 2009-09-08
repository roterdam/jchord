package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.core.storage.EMailAddressDAO;
import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.storage.spi.EMailAddressDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.model.ResourceInternal;
import net.javacoding.jspider.core.model.EMailAddressInternal;
import net.javacoding.jspider.api.model.*;

/**
 * $Id: EMailAddressDAOImpl.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
class EMailAddressDAOImpl implements EMailAddressDAO {

    protected Log log;
    protected StorageSPI storage;
    protected EMailAddressDAOSPI spi;

    public EMailAddressDAOImpl ( Log log, StorageSPI storage, EMailAddressDAOSPI spi ) {
        this.log = log;
        this.storage = storage;
        this.spi = spi;
    }

    public EMailAddress find(String address) {
        return spi.find(address);
    }

    public void register(Resource resource, EMailAddress address) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        EMailAddressInternal ai = TypeTranslator.translate(address);
        spi.register(ri, ai);
    }

    public EMailAddress[] findByResource(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.findByResource(ri);
    }

    public EMailAddressReference[] findReferencesByResource(Resource resource) {
        ResourceInternal ri = TypeTranslator.translate(resource);
        return spi.findReferencesByResource(ri);
    }

}
