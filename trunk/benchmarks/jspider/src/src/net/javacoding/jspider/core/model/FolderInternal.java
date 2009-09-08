package net.javacoding.jspider.core.model;

import net.javacoding.jspider.api.model.*;
import net.javacoding.jspider.core.storage.spi.StorageSPI;

/**
 * $Id: FolderInternal.java,v 1.2 2003/04/11 16:37:04 vanrogu Exp $
 */
public class FolderInternal implements Folder {

    protected StorageSPI storage;
    protected int id;
    protected int parent;
    protected String name;
    protected int site;

    public FolderInternal ( StorageSPI storage, int id, int parent, String name, int site ) {
        this.storage = storage;
        this.id = id;
        this.parent= parent;
        this.name = name;
        this.site = site;
    }

    public int getId() {
        return id;
    }

    public int getSiteId ( ) {
        return site;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Site getSite() {
        return storage.getSiteDAO().find(site);
    }

    public String getName() {
        return name;
    }

    public Folder getParent() {
        if ( parent == 0 ) {
            return null;
        } else {
          return storage.getFolderDAO().findById(parent);
        }
    }

    public Resource[] getResources() {
        return storage.getResourceDAO().findByFolder(this);
    }

    public Folder[] getFolders() {
        return storage.getFolderDAO().findSubFolders(this);
    }

    public Folder getFolder(String name) {
        Folder[] folders = getFolders();
        for (int i = 0; i < folders.length; i++) {
            Folder folder = folders[i];
            if ( folder.getName().equals( name ) ){
                return folder;
            }
        }
        return null;
    }

    public String toString ( ) {
        return "[FOLDER id: " + id + " name:" + name + "]";
    }

    public int hashCode () {
        return id;
    }

    public boolean equals ( Object other ) {
        if ( other instanceof FolderInternal ) {
          FolderInternal otherFolder = (FolderInternal)other;
            if (otherFolder.id == id ) {
                return true;
            }
        }
        return false;
    }

}
