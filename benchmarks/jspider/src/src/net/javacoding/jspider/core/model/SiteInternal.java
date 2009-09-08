package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.Constants;
import net.javacoding.jspider.core.storage.spi.StorageSPI;

import java.net.URL;

/**
 * $Id: SiteInternal.java,v 1.13 2003/04/29 17:53:48 vanrogu Exp $
 */
public class SiteInternal implements Site {

    protected StorageSPI storage;

    protected int id;
    protected boolean handle;
    protected URL url;
    protected String host;
    protected int port;
    protected int hashCode;
    protected boolean isBaseSite;
    protected boolean hasRobotsTXT;
    protected boolean useCookies;
    protected boolean useProxy;
    protected int state;
    protected boolean obeyRobotsTXT;
    protected boolean fetchRobotsTXT;
    protected String userAgent;

    public SiteInternal ( StorageSPI storage, int id, boolean handle, URL url, boolean isBaseSite ) {
        this(storage, id, handle, url, Site.STATE_DISCOVERED,  true, true, true, Constants.USERAGENT, isBaseSite );
    }

    public SiteInternal ( int id, StorageSPI storage, URL url ) {
        this ( storage, id, true, url, false );
    }

    public SiteInternal ( StorageSPI storage, int id, boolean handle, URL url, int state, boolean obeyRobotsTXT, boolean useProxy, boolean useCookies,String userAgent, boolean isBaseSite ) {
        this.storage = storage;
        this.handle = handle;
        this.id = id;
        this.url = url;
        this.host = url.getHost();
        this.port = url.getPort();
        this.state = state;
        this.obeyRobotsTXT = obeyRobotsTXT;
        this.useProxy = useProxy;
        this.useCookies = useCookies;
        this.userAgent = userAgent;
        this.isBaseSite = isBaseSite;
        hashCode = (host + port).hashCode();
    }

    public int getId ( ) {
        return id;
    }

    public void setId ( int id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isRobotsTXTHandled() {
        return (state == Site.STATE_ROBOTSTXT_HANDLED) || (state == Site.STATE_ROBOTSTXT_UNEXISTING) || (state == Site.STATE_ROBOTSTXT_ERROR ) || (state == Site.STATE_ROBOTSTXT_SKIPPED);
    }

    public URL getURL() {
        return url;
    }

    public Folder[] getRootFolders() {
        return storage.getFolderDAO().findSiteRootFolders(this);
    }

    public Folder getRootFolder(String name) {
        Folder[] folders = getRootFolders();
        for (int i = 0; i < folders.length; i++) {
            Folder folder = folders[i];
            if ( folder.getName().equals(name)) {
                return folder;
            }
        }
        return null;
    }

    public Resource[] getRootResources() {
        return storage.getResourceDAO().getRootResources(this);
    }

    public Resource[] getAllResources() {
        return storage.getResourceDAO().getBySite(this);
    }

    public Cookie[] getCookies() {
        return storage.getCookieDAO().find(id);
    }

    public String getCookieString() {
        Cookie[] c = getCookies();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < c.length; i++) {
            Cookie cookie = c[i];
            sb.append(cookie.getName());
            sb.append("=");
            sb.append(cookie.getValue());
            sb.append("; ");
        }
        return sb.toString();
    }

    public boolean getUseCookies() {
        return useCookies;
    }

    public boolean equals(Object object) {
        if (object instanceof Site) {
            Site other = (Site) object;
            return (other.getHost().equalsIgnoreCase(host) && other.getPort() == port);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean getUseProxy() {
        return useProxy;
    }

    public void registerNoRobotsTXTFound ( ) {
        state = Site.STATE_ROBOTSTXT_UNEXISTING;
    }

    public void registerRobotsTXTError() {
        state = Site.STATE_ROBOTSTXT_ERROR;
    }

    public void registerRobotsTXT() {
        this.state = Site.STATE_ROBOTSTXT_HANDLED;
    }

    public void registerRobotsTXTSkipped() {
        this.state = Site.STATE_ROBOTSTXT_SKIPPED;
    }

    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    public void setObeyRobotsTXT(boolean obey) {
        this.obeyRobotsTXT = obey;
    }

    public boolean getObeyRobotsTXT() {
        return this.obeyRobotsTXT;
    }

    public boolean getFetchRobotsTXT ( ) {
        return fetchRobotsTXT;
    }

    public void setFetchRobotsTXT ( boolean fetchRobotsTXT ) {
        this.fetchRobotsTXT = fetchRobotsTXT;
    }

    public String translateState ( ) {
        switch ( state ) {
            case Site.STATE_DISCOVERED:
                return "DISCOVERED";
            case Site.STATE_ROBOTSTXT_ERROR:
                return "ROBOTSTXT_ERROR";
            case Site.STATE_ROBOTSTXT_UNEXISTING:
                return "ROBOTSTXT_UNEXISTING";
            case Site.STATE_ROBOTSTXT_HANDLED:
                return "ROBOTSTXT_HANDLED";
            case Site.STATE_ROBOTSTXT_SKIPPED:
                return "ROBOTSTXT_SKIPPED";
        }
        return "<ERROR_UNKNOWN_STATE>";
    }

    public String toString ( ) {
        return "[Site: " + url + " - " + translateState() + ((isBaseSite)?" *":"") + "]";
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent ( String userAgent ) {
        this.userAgent = userAgent;
    }

    public void setBaseSite ( boolean isBaseSite ) {
        this.isBaseSite = isBaseSite;
    }

    public boolean isBaseSite ( ) {
        return isBaseSite;
    }

    public boolean mustHandle ( ) {
        return handle;
    }

    /**
     * needed for easy templating
     */
    public boolean getMustHandle ( ) {
        return handle;
    }

    public void setHandle ( boolean mustHandle ) {
        this.handle = mustHandle;
    }

}
