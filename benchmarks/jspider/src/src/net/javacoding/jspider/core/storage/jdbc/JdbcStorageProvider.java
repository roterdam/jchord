package net.javacoding.jspider.core.storage.jdbc;


import net.javacoding.jspider.core.storage.StorageProvider;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.util.config.PropertySet;


/**
 *
 * $Id: JdbcStorageProvider.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public class JdbcStorageProvider implements StorageProvider {
    public StorageSPI createStorage(PropertySet props) {
        return new JdbcStorageImpl(props);
    }
}
