package net.javacoding.jspider.core.storage;

import net.javacoding.jspider.api.model.*;

/**
 * $Id: EMailAddressDAO.java,v 1.1 2003/04/08 15:50:27 vanrogu Exp $
 */
public interface EMailAddressDAO {

    public EMailAddress find ( String address );

    public void register ( Resource resource, EMailAddress address );

    public EMailAddress[] findByResource ( Resource resource );

    public EMailAddressReference[] findReferencesByResource ( Resource resource );

}
