package net.javacoding.jspider.core.storage.memory;

import net.javacoding.jspider.core.storage.spi.FolderDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.FolderInternal;
import net.javacoding.jspider.core.model.SiteInternal;

import java.util.*;

/**
 * $Id: FolderDAOImpl.java,v 1.2 2003/04/11 16:37:07 vanrogu Exp $
 */
class FolderDAOImpl implements FolderDAOSPI {

    protected StorageSPI storage;

    protected Map parents;
    protected Map children;
    protected Map byId;
    protected Map siteRoots;

    public FolderDAOImpl ( StorageSPI storage ) {
        this.storage = storage;
        this.parents = new HashMap();
        this.children = new HashMap();
        this.byId = new HashMap();
        this.siteRoots = new HashMap();
    }

    public FolderInternal[] findSubFolders(FolderInternal folder) {
        Set folders = (Set) children.get(folder);
        if ( folders == null ) {
            return new FolderInternal[0];
        } else {
          return (FolderInternal[]) folders.toArray(new FolderInternal[folders.size()]);
        }
    }

    public FolderInternal[] findSiteRootFolders(SiteInternal site) {
        Set rootFolders = (Set) siteRoots.get(new Integer(site.getId()));
        if ( rootFolders == null ) {
            return new FolderInternal[0];
        } else {
          return (FolderInternal[]) rootFolders.toArray(new FolderInternal[rootFolders.size()]);
        }
    }

    public FolderInternal createFolder(int id, FolderInternal parent, String name) {
        FolderInternal folder = new FolderInternal ( storage, id, parent.getId(), name, parent.getSiteId() );
        byId.put(new Integer(id), folder);
        parents.put(folder, parent);

        Set set = (Set) children.get(parent);
        if ( set == null ) {
            set = new HashSet();
            children.put(parent,set);
        }
        set.add(folder);

        return folder;
    }

    public FolderInternal createFolder(int id, SiteInternal site, String name) {
        FolderInternal folder = new FolderInternal ( storage, id, 0, name, site.getId() );
        Set roots = (Set) siteRoots.get(new Integer(site.getId()));
        if (roots == null){
            roots = new HashSet();
            siteRoots.put(new Integer(site.getId()), roots);
        }
        roots.add(folder);

        byId.put(new Integer(id), folder);
        return folder;
    }

    public FolderInternal findById(int id) {
        return (FolderInternal)byId.get(new Integer(id));
    }
}
