package net.javacoding.jspider.core.storage.memory;

import net.javacoding.jspider.core.storage.Storage;
import net.javacoding.jspider.core.storage.spi.CookieDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.api.model.Cookie;
import net.javacoding.jspider.api.model.Site;

import java.util.*;
import java.sql.*;

/**
 * $Id: CookieDAOImpl.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 */
class CookieDAOImpl implements CookieDAOSPI {

    protected StorageSPI storage;
    protected Map cookies;

    public CookieDAOImpl ( StorageSPI storage ) {
        this.storage = storage;
        this.cookies = new HashMap ( );
    }

    public Cookie[] find(int id) {
        Cookie[] cookies = (Cookie[]) this.cookies.get(new Integer(id));
        if ( cookies == null ) {
            cookies = new Cookie[0];
        }
        return cookies;
    }

    public void save(int id, Cookie[] cookies) {
        this.cookies.put(new Integer(id), cookies);
    }

}
