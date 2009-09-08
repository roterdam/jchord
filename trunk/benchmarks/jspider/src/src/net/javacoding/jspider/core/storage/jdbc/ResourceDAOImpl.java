package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.event.impl.*;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.core.storage.spi.ResourceDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.storage.exception.InvalidStateTransitionException;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;

/**
 * $Id: ResourceDAOImpl.java,v 1.11 2003/04/19 19:00:46 vanrogu Exp $
 */
class ResourceDAOImpl implements ResourceDAOSPI {

    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_SITE = "site";
    public static final String ATTRIBUTE_URL = "url";
    public static final String ATTRIBUTE_STATE = "state";
    public static final String ATTRIBUTE_MIME = "mimetype";
    public static final String ATTRIBUTE_TIME = "timems";
    public static final String ATTRIBUTE_SIZE = "size";
    public static final String ATTRIBUTE_FOLDER = "folder";
    public static final String ATTRIBUTE_HTTP_STATUS = "httpstatus";

    protected DBUtil dbUtil;
    protected StorageSPI storage;
    protected Log log;

    public ResourceDAOImpl(StorageSPI storage, DBUtil dbUtil) {
        this.storage = storage;
        this.dbUtil = dbUtil;
        this.log = LogFactory.getLog(ResourceDAOImpl.class);
    }

    public void registerURLReference(URL url, URL refererURL) {
        ResourceInternal resource = getResource(url);
        Statement st = null;
        ResultSet rs = null;
        if (refererURL != null) {
            ResourceInternal referer = getResource(refererURL);
            try {
                int from = referer.getId();
                int to = resource.getId();
                Connection connection = dbUtil.getConnection();

                st = connection.createStatement();
                rs = st.executeQuery("select count(*) from jspider_resource_reference where referer = " + from + " and referee = " + to);
                rs.next();
                Statement st2 = connection.createStatement();
                if (rs.getInt(1) == 0) {
                    st2.executeUpdate("insert into jspider_resource_reference ( referer, referee, count ) values (" + from + "," + to + ", 1)");
                } else {
                    st2.executeUpdate("update jspider_resource_reference set count = count + 1 where referer = " + from + " and referee = " + to);
                }
            } catch (SQLException e) {
                log.error("SQLException", e);
            } finally {
                dbUtil.safeClose(rs, log);
                dbUtil.safeClose(st, log);
            }
        }
    }

    public ResourceInternal[] findAllResources() {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource");
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceInternal[] getRefereringResources(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource, jspider_resource_reference where jspider_resource.id = jspider_resource_reference.referer and jspider_resource_reference.referee = " + resource.getId());
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceReferenceInternal[] getOutgoingReferences(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select referer.url as referer, referee.url as referee, count from jspider_resource referer, jspider_resource referee, jspider_resource_reference where jspider_resource_reference.referer = " + resource.getId() + " and jspider_resource_reference.referee = referee.id and jspider_resource_reference.referer = referer.id");
            while (rs.next()) {
                al.add(createResourceReferenceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceReferenceInternal[]) al.toArray(new ResourceReferenceInternal[al.size()]);
    }

    public ResourceReferenceInternal[] getIncomingReferences(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select referer.url as referer, referee.url as referee, count from jspider_resource referer, jspider_resource referee, jspider_resource_reference where jspider_resource_reference.referee = " + resource.getId() + " and jspider_resource_reference.referee = referee.id and jspider_resource_reference.referer = referer.id");
            while (rs.next()) {
                al.add(createResourceReferenceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceReferenceInternal[]) al.toArray(new ResourceReferenceInternal[al.size()]);
    }

    public ResourceInternal[] getReferencedResources(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource, jspider_resource_reference where jspider_resource.id = jspider_resource_reference.referee and jspider_resource_reference.referer = " + resource.getId());
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }


    public ResourceInternal[] findByFolder(FolderInternal folder) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource where folder=" + folder.getId());
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceInternal[] getBySite(SiteInternal site) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource where site=" + site.getId());
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public ResourceInternal[] getRootResources(SiteInternal site) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_resource where site=" + site.getId() + " and folder=0");
            while (rs.next()) {
                al.add(createResourceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (ResourceInternal[]) al.toArray(new ResourceInternal[al.size()]);
    }

    public synchronized void setSpidered(URL url, URLSpideredOkEvent event) {
        ResourceInternal resource = getResource(url);
        resource.setFetched(event.getHttpStatus(), event.getSize(), event.getTimeMs(), event.getMimeType(), null, event.getHeaders());
        save(resource);
        resource.setBytes(event.getBytes());
    }

    public synchronized void setIgnoredForParsing(URL url) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParseIgnored();
        save(resource);
    }

    public synchronized void setIgnoredForFetching(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setFetchIgnored();
        save(resource);
    }

    public synchronized void setForbidden(URL url, URLFoundEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setForbidden();
        save(resource);
    }

    public synchronized void setError(URL url, ResourceParsedErrorEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParseError();
        save(resource);
    }

    public synchronized void setParsed(URL url, ResourceParsedOkEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setParsed();
        save(resource);
    }

    public synchronized void setError(URL url, URLSpideredErrorEvent event) throws InvalidStateTransitionException {
        ResourceInternal resource = getResource(url);
        resource.setFetchError(event.getHttpStatus(), event.getHeaders());
        save(resource);
    }

    public ResourceInternal getResource(int id) {
        ResourceInternal resource = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            st = dbUtil.getConnection().createStatement();
            rs = st.executeQuery("select * from jspider_resource where id='" + id + "'");
            if (rs.next()) {
                resource = createResourceFromRecord(rs);
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return resource;
    }

    public ResourceInternal getResource(URL url) {
        ResourceInternal resource = null;
        Statement st = null;
        ResultSet rs = null;
        if (url != null) {
            try {
                st = dbUtil.getConnection().createStatement();
                rs = st.executeQuery("select * from jspider_resource where url='" + url + "'");
                if (rs.next()) {
                    resource = createResourceFromRecord(rs);
                }
            } catch (SQLException e) {
                log.error("SQLException", e);
            } finally {
                dbUtil.safeClose(rs, log);
                dbUtil.safeClose(st, log);
            }
        }
        return resource;
    }

    public void create(int id, ResourceInternal resource) {
        Connection connection = dbUtil.getConnection();
        StringBuffer sb = new StringBuffer();
        Statement st = null;

        sb.append("insert into jspider_resource (");
        sb.append("id,");
        sb.append("url,");
        sb.append("site,");
        sb.append("state,");
        sb.append("httpstatus,");
        sb.append("timems,");
        sb.append("folder");
        sb.append(") values (");
        sb.append(DBUtil.format(id));
        sb.append(",");
        sb.append(DBUtil.format(resource.getURL()));
        sb.append(",");
        sb.append(DBUtil.format(resource.getSiteId()));
        sb.append(",");
        sb.append(DBUtil.format(resource.getState()));
        sb.append(",");
        sb.append(DBUtil.format(resource.getHttpStatusInternal()));
        sb.append(",");
        sb.append(DBUtil.format(resource.getTimeMsInternal()));
        sb.append(",");
        FolderInternal folder = (FolderInternal) resource.getFolder();
        int folderId = (folder == null) ? 0 : folder.getId();
        sb.append(DBUtil.format(folderId));
        sb.append(")");
        try {
            st = connection.createStatement();
            st.executeUpdate(sb.toString());
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(st, log);
        }
    }

    public void save(ResourceInternal resource) {
        Connection connection = dbUtil.getConnection();
        StringBuffer sb = new StringBuffer();
        Statement st = null;
        sb.append("update jspider_resource set ");
        sb.append("state=");
        sb.append(DBUtil.format(resource.getState()));
        sb.append(",mimetype=");
        sb.append(DBUtil.format(resource.getMimeInternal()));
        sb.append(",httpstatus=");
        sb.append(DBUtil.format(resource.getHttpStatusInternal()));
        sb.append(",size=");
        sb.append(DBUtil.format(resource.getSizeInternal()));
        sb.append(",timems=");
        sb.append(DBUtil.format(resource.getTimeMsInternal()));
        sb.append(" where id=");
        sb.append(DBUtil.format(resource.getId()));
        try {
            st = connection.createStatement();
            st.executeUpdate(sb.toString());
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(st, log);
        }
    }

    protected ResourceInternal createResourceFromRecord(ResultSet rs) throws SQLException {
        int id = rs.getInt(ATTRIBUTE_ID);
        int folderId = rs.getInt(ATTRIBUTE_FOLDER);
        int siteId = rs.getInt(ATTRIBUTE_SITE);
        String urlString = rs.getString(ATTRIBUTE_URL);
        int state = rs.getInt(ATTRIBUTE_STATE);
        String mime = rs.getString(ATTRIBUTE_MIME);
        int time = rs.getInt(ATTRIBUTE_TIME);
        int size = rs.getInt(ATTRIBUTE_SIZE);
        int httpStatus = rs.getInt(ATTRIBUTE_HTTP_STATUS);

        FolderInternal folder = storage.getFolderDAO().findById(folderId);

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.error("MalformedURLException", e);
        }
        ResourceInternal ri = new ResourceInternal(storage, id, siteId, url, null, folder);
        ri.setSize(size);
        ri.setTime(time);
        ri.setState(state);
        ri.setMime(mime);
        ri.setHttpStatus(httpStatus);
        return ri;
    }

    protected ResourceReferenceInternal createResourceReferenceFromRecord(ResultSet rs) throws SQLException {
        ResourceReferenceInternal rr = null;
        try {
            String refererURL = rs.getString("referer");
            String refereeURL = rs.getString("referee");
            URL referer = new URL(refererURL);
            URL referee = new URL(refereeURL);
            int count = rs.getInt("count");
            rr = new ResourceReferenceInternal(storage, referer, referee, count);
        } catch (MalformedURLException e) {
            log.error("MalformedURLException", e);
        }
        return rr;
    }

}
