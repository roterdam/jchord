package net.javacoding.jspider.api.model;

/**
 * $Id: HTTPHeader.java,v 1.1 2003/03/08 19:52:02 vanrogu Exp $
 */
public class HTTPHeader {

    protected String name;
    protected String value;

    public HTTPHeader ( String name, String value ) {
        this.name = name;
        this.value = value;
    }

    public String getName ( ) {
        return name;
    }

    public String getValue ( ) {
        return value;
    }

}
