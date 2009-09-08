package net.javacoding.jspider.core.util.html;

import java.net.URL;

/**
 * $Id: URLFinderCallback.java,v 1.3 2003/04/07 15:50:52 vanrogu Exp $
 */
public interface URLFinderCallback {

    public URL getContextURL ( );

    public void setContextURL ( URL url );

    public void malformedContextURLFound ( String malformedURL );

    public void urlFound ( URL foundURL );

    public void malformedUrlFound ( String malformedURL );

}
