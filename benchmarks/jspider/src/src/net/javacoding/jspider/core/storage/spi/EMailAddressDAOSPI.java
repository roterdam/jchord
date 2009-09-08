package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.core.storage.EMailAddressDAO;
import net.javacoding.jspider.core.model.*;

/**
 * $Id: EMailAddressDAOSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 */
public interface EMailAddressDAOSPI  {

    public EMailAddressInternal find ( String address );

    public void register ( ResourceInternal resource, EMailAddressInternal address );

    public EMailAddressInternal[] findByResource ( ResourceInternal resource );

    public EMailAddressReferenceInternal[] findReferencesByResource ( ResourceInternal resource );

}
