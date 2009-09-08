package net.javacoding.jspider.core.storage.impl;

import net.javacoding.jspider.core.storage.CookieDAO;
import net.javacoding.jspider.core.storage.spi.CookieDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.api.model.Cookie;
import net.javacoding.jspider.api.model.Site;

/**
 * $Id: CookieDAOImpl.java,v 1.1 2003/04/11 16:37:05 vanrogu Exp $
 */
class CookieDAOImpl implements CookieDAO {

    protected Log log;
    protected StorageSPI storage;
    protected CookieDAOSPI spi;

    public CookieDAOImpl ( Log log, StorageSPI storage, CookieDAOSPI spi ) {
        this.log = log;
        this.storage = storage;
        this.spi = spi;
    }

    public Cookie[] find(Site site) {
        SiteInternal si = TypeTranslator.translate(site);
        return spi.find(si.getId());
    }

    public void save(Site site, Cookie[] cookies) {
        SiteInternal si = TypeTranslator.translate(site);
        spi.save(si.getId(),cookies);
    }

}
