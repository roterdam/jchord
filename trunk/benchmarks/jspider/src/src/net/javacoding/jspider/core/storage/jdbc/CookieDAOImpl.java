package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.storage.CookieDAO;
import net.javacoding.jspider.core.storage.spi.CookieDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.logging.LogFactory;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.api.model.Cookie;

import java.util.*;
import java.sql.*;

/**
 * $Id: CookieDAOImpl.java,v 1.3 2003/04/11 16:37:05 vanrogu Exp $
 */
class CookieDAOImpl implements CookieDAOSPI {

    protected Log log;

    protected StorageSPI storage;
    protected DBUtil dbUtil;
    protected Map cookies;

    public CookieDAOImpl ( StorageSPI storage, DBUtil dbUtil ) {
        this.log = LogFactory.getLog(CookieDAO.class);
        this.storage = storage;
        this.dbUtil = dbUtil;
        this.cookies = new HashMap ( );
    }

    public Cookie[] find(int id) {
        ArrayList al = new ArrayList ( );
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_cookie where site='" + id + "'" );
            while ( rs.next() ) {
                al.add(createCookieFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (Cookie[]) al.toArray(new Cookie[al.size()]);
    }

    public void save(int id, Cookie[] cookies) {
        Connection connection = dbUtil.getConnection();

        Statement st = null;
        ResultSet rs = null;

        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            try {
                st = connection.createStatement();
                rs = st.executeQuery("select count(*) as count from jspider_cookie where id='" + id + "' and name='" + cookie.getName() + "'");
                rs.next();
                int count = rs.getInt("count");
                if ( count == 0 ) {
                    st = connection.createStatement();
                    st.executeUpdate("insert into jspider_cookie ( site, name, value, path, expires, domain ) values ( '" + id + "', '" + cookie.getName() + "', '" + cookie.getValue() + "', '"+  cookie.getPath() + "', '" + cookie.getExpires() + "', '" + cookie.getDomain() + "')");
                } else {
                    st = connection.createStatement();
                    st.executeUpdate("update jspider_cookie set value='" + cookie.getValue() + "', path='" + cookie.getPath() + "', domain='"+  cookie.getDomain() + "', expires='" + cookie.getExpires() + "' where site='" + id + "' and name='" + cookie.getName() + "'");
                }
            } catch (SQLException e) {
                log.error("SQLException", e);
            } finally {
                dbUtil.safeClose(rs, log);
                dbUtil.safeClose(st, log);
            }
        }
    }

    protected static Cookie createCookieFromRecord ( ResultSet rs ) throws SQLException {
        String name = rs.getString("name") ;
        String value = rs.getString("value") ;
        String path = rs.getString("path") ;
        String domain = rs.getString("domain") ;
        String expires = rs.getString("expires") ;
        return new Cookie(name, value, domain, path, expires);
    }

}
