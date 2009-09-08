package net.javacoding.jspider.api.model;

/**
 * $Id: ResourceReference.java,v 1.1 2003/04/08 15:50:26 vanrogu Exp $
 */
public interface ResourceReference {

    public FetchedResource getReferer ( );

    public Resource getReferee ( );

    public int getCount ( );

}
