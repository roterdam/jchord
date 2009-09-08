package net.javacoding.jspider.api.model;


import java.io.InputStream;


/**
 *
 * $Id: FetchedResource.java,v 1.4 2003/04/01 19:44:36 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface FetchedResource extends FetchTriedResource {

    public int getTimeMs();

    public int getSize();

    public String getMime();

    public InputStream getInputStream();

}
