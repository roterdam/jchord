package net.javacoding.jspider.api.model;


/**
 *
 * $Id: Folder.java,v 1.2 2003/04/09 17:08:03 vanrogu Exp $
 *
 * @author Günther Van Roey
 */
public interface Folder {

    public Site getSite();

    public String getName();

    public Folder getParent();

    public Resource[] getResources();

    public Folder[] getFolders ( );

    public Folder getFolder ( String name );

}
