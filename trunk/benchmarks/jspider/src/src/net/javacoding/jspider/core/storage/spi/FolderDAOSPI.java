package net.javacoding.jspider.core.storage.spi;

import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.model.FolderInternal;

/**
 * $Id: FolderDAOSPI.java,v 1.1 2003/04/11 16:37:08 vanrogu Exp $
 */
public interface FolderDAOSPI {


    public FolderInternal findById ( int id );

    public FolderInternal[] findSubFolders ( FolderInternal folder );

    public FolderInternal[] findSiteRootFolders ( SiteInternal site );

    public FolderInternal createFolder ( int id, FolderInternal parent, String name );

    public FolderInternal createFolder ( int id, SiteInternal site, String name );

}
