package net.javacoding.jspider.core.storage;


import net.javacoding.jspider.api.model.Summary;


/**
 *
 * $Id: Storage.java,v 1.17 2003/04/11 16:37:05 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Storage {

    public SiteDAO getSiteDAO ( );
    public ResourceDAO getResourceDAO ( );
    public DecisionDAO getDecisionDAO ( );
    public CookieDAO getCookieDAO ( );
    public EMailAddressDAO getEMailAddressDAO ( );
    public Summary getSummary ( );

}
