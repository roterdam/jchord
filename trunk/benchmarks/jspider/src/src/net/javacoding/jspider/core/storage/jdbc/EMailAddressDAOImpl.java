package net.javacoding.jspider.core.storage.jdbc;

import net.javacoding.jspider.core.storage.EMailAddressDAO;
import net.javacoding.jspider.core.storage.spi.EMailAddressDAOSPI;
import net.javacoding.jspider.core.storage.spi.StorageSPI;
import net.javacoding.jspider.core.model.*;
import net.javacoding.jspider.core.logging.Log;
import net.javacoding.jspider.core.logging.LogFactory;

import java.sql.*;
import java.util.ArrayList;

/**
 * $Id: EMailAddressDAOImpl.java,v 1.2 2003/04/11 16:37:06 vanrogu Exp $
 */
class EMailAddressDAOImpl implements EMailAddressDAOSPI {

    protected Log log;
    protected StorageSPI storage;
    protected DBUtil dbUtil;

    public EMailAddressDAOImpl(StorageSPI storage, DBUtil dbUtil) {
        this.log = LogFactory.getLog(EMailAddressDAO.class);
        this.storage = storage;
        this.dbUtil = dbUtil;
    }

    public void register(ResourceInternal resource, EMailAddressInternal address) {
        Connection connection = dbUtil.getConnection();

        Statement st = null;
        ResultSet rs = null;

        try {
            st = connection.createStatement();
            rs = st.executeQuery("select count(*) as count from jspider_email_address where address='" + address.getAddress() + "'");
            rs.next();
            int count = rs.getInt("count");
            if (count == 0) {
                st = connection.createStatement();
                st.executeUpdate("insert into jspider_email_address ( address ) values ( '" + address.getAddress() + "' )");
                st = connection.createStatement();
                ResultSet rs2 = st.executeQuery("select id from jspider_email_address where address = '" + address.getAddress() + "'");
                rs2.next();
                address.setId(rs2.getInt("id"));
            }

            rs = st.executeQuery("select count(*) as count from jspider_email_address, jspider_email_address_reference where jspider_email_address_reference.resource=" + resource.getId() + " and jspider_email_address_reference.address = jspider_email_address.id and jspider_email_address.id = " + address.getId());
            rs.next();
            st = connection.createStatement();
            if ( rs.getInt("count") == 0 ) {
                st.executeUpdate("insert into jspider_email_address_reference ( resource, address, count ) values ( " + resource.getId() + "," + address.getId() + ", 1 )");
            } else {
                st.executeUpdate("update jspider_email_address_reference set count = count + 1 where address = " + address.getId() + " and resource = " + resource.getId() );
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
    }

    public EMailAddressInternal find(String address) {
        Statement st = null;
        ResultSet rs = null;
        EMailAddressInternal emailAddress = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select * from jspider_email_address where address = '" + address + "'");
            while (rs.next()) {
                emailAddress = createEMailAddressFromRecord(rs);
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return emailAddress;
    }

    public EMailAddressInternal[] findByResource(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select address.id, address.address from jspider_email_address address, jspider_email_address_reference ref where ref.address=address.id and ref.resource=" + resource.getId());
            while (rs.next()) {
                al.add(createEMailAddressFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (EMailAddressInternal[]) al.toArray(new EMailAddressInternal[al.size()]);
    }

    public EMailAddressReferenceInternal[] findReferencesByResource(ResourceInternal resource) {
        ArrayList al = new ArrayList();
        Statement st = null;
        ResultSet rs = null;
        try {
            Connection connection = dbUtil.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery("select jspider_email_address.address, jspider_email_address_reference.resource, jspider_email_address_reference.count from jspider_email_address, jspider_email_address_reference where jspider_email_address_reference.resource=" + resource.getId() + " and jspider_email_address_reference.address = jspider_email_address.id");
            while (rs.next()) {
                al.add(createEMailAddressReferenceFromRecord(rs));
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        } finally {
            dbUtil.safeClose(rs, log);
            dbUtil.safeClose(st, log);
        }
        return (EMailAddressReferenceInternal[]) al.toArray(new EMailAddressReferenceInternal[al.size()]);
    }

    protected EMailAddressInternal createEMailAddressFromRecord(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String address = rs.getString("address");
        return new EMailAddressInternal(id, address);
    }

    protected EMailAddressReferenceInternal createEMailAddressReferenceFromRecord(ResultSet rs) throws SQLException {
        int resource = rs.getInt("resource");
        String address = rs.getString("address");
        int count = rs.getInt("count");
        return new EMailAddressReferenceInternal(storage, resource, address, count);
    }

}
