package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.EMailAddress;

/**
 * $Id: EMailAddressInternal.java,v 1.1 2003/04/08 15:50:26 vanrogu Exp $
 */
public class EMailAddressInternal implements EMailAddress {

    protected int id;
    protected String address;

    public EMailAddressInternal ( int id, String address ) {
        this.id = id;
        this.address = address;
    }

    public EMailAddressInternal ( String address ) {
        this ( 0, address );
    }

    public String getAddress() {
        return address;
    }

    public int getId ( ) {
        return id;
    }

    public void setId ( int id ) {
        this.id = id;
    }

    public String toString ( ) {
        return address;
    }

}
