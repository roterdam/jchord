package net.javacoding.jspider.api.model;


import java.util.Date;


/**
 *
 * $Id: FetchTriedResource.java,v 1.2 2003/04/01 19:44:35 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface FetchTriedResource extends Resource {

    public int getHttpStatus();

    public HTTPHeader[] getHeaders ( );

    public Date getFetchTime();
}
