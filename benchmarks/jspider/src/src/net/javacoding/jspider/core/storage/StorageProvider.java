package net.javacoding.jspider.core.storage;

import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.storage.spi.StorageSPI;


/**
 *
 * $Id: StorageProvider.java,v 1.3 2003/04/11 16:37:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface StorageProvider {

    public StorageSPI createStorage(PropertySet props);

}
