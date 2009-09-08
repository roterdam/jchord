package net.javacoding.jspider.api.model;


import java.net.URL;


/**
 *
 * $Id: Site.java,v 1.23 2003/04/10 16:19:03 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Site {

    /**
     * if the site is newly discovered, we're not going to fetch resources
     * until we have interpreted the site's robots.txt rules.
     */
    public static final int STATE_DISCOVERED = 0;

    /**
     * if the robots.txt was handled (interpreted or missing), we can
     * fetch resources from it.
     */
    public static final int STATE_ROBOTSTXT_HANDLED = 1;

    /**
     * if robots.txt couldn't be fetched (but seems to be there), no resources
     * will be fetched from the site!
     */
    public static final int STATE_ROBOTSTXT_ERROR = 2;

    /**
     * if robots.txt was not found, all resources can be fetched !
     */
    public static final int STATE_ROBOTSTXT_UNEXISTING = 3;

    /**
     * if robots.txt was skipped, all resources can be fetched !
     */
    public static final int STATE_ROBOTSTXT_SKIPPED = 4;


    public int getState();

    public String getHost();

    public int getPort();

    public boolean isRobotsTXTHandled();

    public boolean getObeyRobotsTXT();

    public boolean getFetchRobotsTXT();

    public URL getURL();

    public Folder[] getRootFolders();

    public Folder getRootFolder(String name);

    public Resource[] getRootResources();

    public Resource[] getAllResources();

    public Cookie[] getCookies();

    public String getCookieString();

    public boolean getUseCookies();

    public boolean getUseProxy();

    public String getUserAgent ( );

    public boolean isBaseSite ( );

    public boolean mustHandle ( );

 }
