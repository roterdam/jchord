package net.javacoding.jspider.api.model;

/**
 * $Id: Cookie.java,v 1.1 2003/03/08 19:52:02 vanrogu Exp $
 * @todo improve Cookie support: use domain, path and expires
 */
public class Cookie {

    protected String name;
    protected String value;
    protected String domain;
    protected String path;
    protected String expires;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Cookie(String name, String value, String domain, String path, String expires) {
        this(name, value);
        this.domain = domain;
        this.path = path;
        this.expires = expires;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDomain ( ) {
        return domain;
    }

    public String getPath ( ) {
        return path;
    }

    public String getExpires ( ) {
        return expires;
    }

    public boolean equals(Object other) {
        if (other instanceof Cookie) {
            Cookie otherCookie = (Cookie) other;
            if (otherCookie.name.equals(this.name)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        return name.hashCode();
    }
}
