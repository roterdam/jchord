package net.javacoding.jspider.api.model;


import java.net.URL;
import java.util.Date;


/**
 *
 * $Id: Resource.java,v 1.8 2003/04/09 17:08:03 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Resource {

    public static final int STATE_DISCOVERED = 1;
    public static final int STATE_FETCH_ERROR = 2;
    public static final int STATE_FETCH_IGNORED = 3;
    public static final int STATE_FETCH_FORBIDDEN = 4;
    public static final int STATE_FETCHED = 5;
    public static final int STATE_PARSE_ERROR = 6;
    public static final int STATE_PARSE_IGNORED = 7;
    public static final int STATE_PARSED = 8;

    public int getState();

    public String getStateName ();

    public URL getURL();

    public String getFileName();

    public Site getSite();

    public Folder getFolder();

    public String getName();

    public Date getDiscoveryTime();

    public Resource[] getReferers();

    public Decision getSpiderDecision ( );

    public Decision getParseDecision ( );

}
