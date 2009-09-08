package net.javacoding.jspider.api.model;

/**
 * $Id: EMailAddressReference.java,v 1.1 2003/04/08 15:50:26 vanrogu Exp $
 */
public interface EMailAddressReference {

    public FetchedResource getResource ( );

    public EMailAddress getEMailAddress ( );

    public int getCount ( );

}
