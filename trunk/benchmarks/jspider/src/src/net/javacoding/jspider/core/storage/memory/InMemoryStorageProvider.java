package net.javacoding.jspider.core.storage.memory;


import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.storage.StorageProvider;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.util.config.PropertySet;


/**
 *
 * $Id: InMemoryStorageProvider.java,v 1.3 2003/04/11 16:37:07 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class InMemoryStorageProvider implements StorageProvider {
    public StorageSPI createStorage(PropertySet props) {
        return new InMemoryStorageImpl();
    }
}
