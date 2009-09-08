package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.storage.spi.FolderDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.model.FolderInternal;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;

import java.sql.*;
import java.util.ArrayList;

/**
 * $Id: FolderDAOImpl.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 */
class FolderDAOImpl implements FolderDAOSPI {

    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_SITE = "site";
    public static final String ATTRIBUTE_PARENT = "parent";
    public static final String ATTRIBUTE_NAME = "name";

    protected Log log;

    protected StorageSPI storage;
    protected DBUtil dbUtil;

    public FolderDAOImpl ( StorageSPI storage, DBUtil dbUtil ) {
        this.log = LogFactory.getLog(FolderDAOSPI.class);
        this.storage = storage;
        this.dbUtil = dbUtil;
    }

    public FolderInternal[] findSiteRootFolders(SiteInternal site) {
        ArrayList al = new ArrayList ();

        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection ( );
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_folder where parent=0 and site=" + site.getId());
            while ( rs.next() ) {
                al.add(createFolderFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (FolderInternal[])al.toArray(new FolderInternal[al.size()]);
    }

    public FolderInternal[] findSubFolders(FolderInternal folder) {
        ArrayList al = new ArrayList ( );

        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection ( );
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_folder where parent= " + folder.getId());
            while ( rs.next() ) {
                al.add(createFolderFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (FolderInternal[])al.toArray(new FolderInternal[al.size()]);
    }

    public FolderInternal findById(int folderId) {
        FolderInternal folder = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection ( );
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_folder where id=" + folderId);
            if ( rs.next() ) {
                folder = createFolderFromRecord(rs);
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return folder;
    }

    public FolderInternal createFolder(int id, FolderInternal parent, String name) {
        return createFolder ( id, parent.getSiteId(), parent.getId(), name );
    }

    public FolderInternal createFolder(int id, SiteInternal site, String name) {
        return createFolder ( id, site.getId(), 0, name );
    }

    public FolderInternal createFolder ( int id, int siteId, int parentId, String name ) {
        FolderInternal folder = null;

        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection ( );
            st = c.createStatement();
            st.executeUpdate("insert into jspider_folder ( id, parent, site, name ) values (" + id + "," + parentId + "," + siteId + ",'" + name + "')");
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        folder = findById(id);
        return folder;
    }


    protected FolderInternal createFolderFromRecord ( ResultSet rs ) throws SQLException {
        int id = rs.getInt(ATTRIBUTE_ID);
        int parent = rs.getInt(ATTRIBUTE_PARENT);
        int site = rs.getInt(ATTRIBUTE_SITE);
        String name = rs.getString(ATTRIBUTE_NAME);

        return new FolderInternal ( storage, id, parent,  name, site );
    }

}
