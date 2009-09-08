package net.javacoding.jspider.api.model;


/**
 *
 * $Id: ParsedResource.java,v 1.2 2003/04/08 15:50:26 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface ParsedResource extends FetchedResource {

    public Resource[] getReferencedResources();

    public ResourceReference[] getIncomingReferences();

    public ResourceReference[] getOutgoingReferences();

    public EMailAddress[] getEmailAddresses ( );

    public EMailAddressReference[] getEmailAddressReferences ( );

}
