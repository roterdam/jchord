package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.model.SiteInternal;
import net.javacoding.jspider.core.storage.spi.SiteDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;

/**
 * $Id: SiteDAOImpl.java,v 1.8 2003/04/11 16:37:06 vanrogu Exp $
 */
class SiteDAOImpl implements SiteDAOSPI {

    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_HOST = "host";
    public static final String ATTRIBUTE_PORT = "port";
    public static final String ATTRIBUTE_STATE = "state";
    public static final String ATTRIBUTE_OBEYROBOTSTXT = "obeyrobotstxt";
    public static final String ATTRIBUTE_USEPROXY = "useproxy";
    public static final String ATTRIBUTE_USECOOKIES = "usecookies";
    public static final String ATTRIBUTE_HASROBOTSTXT = "hasrobotstxt";
    public static final String ATTRIBUTE_USERAGENT = "useragent";
    public static final String ATTRIBUTE_BASESITE = "basesite";
    public static final String ATTRIBUTE_HANDLE = "handle";

    protected DBUtil dbUtil;
    protected StorageSPI storage;
    protected Log log;

    public SiteDAOImpl(StorageSPI storage, DBUtil dbUtil) {
        this.storage = storage;
        this.dbUtil = dbUtil;
        this.log = LogFactory.getLog(SiteDAOImpl.class);
    }

    public SiteInternal find(URL siteURL) {
        SiteInternal site = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection();
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_site where host = '" + siteURL.getHost() + "' and port = " + siteURL.getPort());
            if (rs.next()) {
                site = createSiteFromRecord(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return site;
    }

    public SiteInternal find(int id) {
        SiteInternal site = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection();
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_site id=" + id);
            if (rs.next()) {
                site = createSiteFromRecord(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return site;
    }

    public void create(int id, SiteInternal site) {
        Connection connection = dbUtil.getConnection();
        Statement st = null;
        StringBuffer sb = new StringBuffer();
        sb.append("insert into jspider_site (");
        sb.append("id,");
        sb.append("host,");
        sb.append("port,");
        sb.append("handle,");
        sb.append("robotstxthandled,");
        sb.append("usecookies,");
        sb.append("useproxy,");
        sb.append("state,");
        sb.append("obeyrobotstxt,");
        sb.append("basesite,");
        sb.append("useragent");
        sb.append(") values (");
        sb.append(DBUtil.format(id));
        sb.append(",");
        sb.append(DBUtil.format(site.getHost()));
        sb.append(",");
        sb.append(DBUtil.format(site.getPort()));
        sb.append(",");
        sb.append(DBUtil.format(site.mustHandle()));
        sb.append(",");
        sb.append(DBUtil.format(site.isRobotsTXTHandled()));
        sb.append(",");
        sb.append(DBUtil.format(site.getUseCookies()));
        sb.append(",");
        sb.append(DBUtil.format(site.getUseProxy()));
        sb.append(",");
        sb.append(DBUtil.format(site.getState()));
        sb.append(",");
        sb.append(DBUtil.format(site.getObeyRobotsTXT()));
        sb.append(",");
        sb.append(DBUtil.format(site.isBaseSite()));
        sb.append(",");
        sb.append(DBUtil.format(site.getUserAgent()));
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

    public void save(int id, SiteInternal site) {
        StringBuffer sb = new StringBuffer();
        Connection connection = dbUtil.getConnection();
        Statement st = null;
        sb.append("update jspider_site set ");
        sb.append("handle=");
        sb.append(DBUtil.format(site.mustHandle()));
        sb.append(",robotstxthandled=");
        sb.append(DBUtil.format(site.isRobotsTXTHandled()));
        sb.append(",usecookies=");
        sb.append(DBUtil.format(site.getUseCookies()));
        sb.append(",useproxy=");
        sb.append(DBUtil.format(site.getUseProxy()));
        sb.append(",state=");
        sb.append(DBUtil.format(site.getState()));
        sb.append(",obeyrobotstxt=");
        sb.append(DBUtil.format(site.getObeyRobotsTXT()));
        sb.append(",basesite=");
        sb.append(DBUtil.format(site.isBaseSite()));
        sb.append(",useragent=");
        sb.append(DBUtil.format(site.getUserAgent()));
        sb.append(" where id = ");
        sb.append(DBUtil.format(id));
        try {
            st = connection.createStatement();
            st.executeUpdate(sb.toString());
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(st, log);
        }
    }

    public SiteInternal[] findAll() {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection c = dbUtil.getConnection();
            st = c.createStatement();
            rs = st.executeQuery("select * from jspider_site");
            while (rs.next()) {
                al.add(createSiteFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (SiteInternal[]) al.toArray(new SiteInternal[al.size()]);
    }


    protected SiteInternal createSiteFromRecord(ResultSet rs) throws SQLException {
        String host = rs.getString(ATTRIBUTE_HOST);
        int id = rs.getInt(ATTRIBUTE_ID);
        int port = rs.getInt(ATTRIBUTE_PORT);
        int state = rs.getInt(ATTRIBUTE_STATE);
        boolean obeyRobotsTXT = rs.getInt(ATTRIBUTE_OBEYROBOTSTXT) != 0;
        boolean useProxy = rs.getInt(ATTRIBUTE_USEPROXY) != 0;
        boolean useCookies = rs.getInt(ATTRIBUTE_USECOOKIES) != 0;
        String userAgent = rs.getString(ATTRIBUTE_USERAGENT);
        boolean baseSite = rs.getInt(ATTRIBUTE_BASESITE) != 0;
        boolean handle = rs.getInt(ATTRIBUTE_HANDLE) != 0;

        URL url = null;
        try {
            url = new URL("http", host, port, "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new SiteInternal(storage, id, handle, url, state, obeyRobotsTXT, useProxy, useCookies, userAgent, baseSite);
    }

}
