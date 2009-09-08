package net.javacoding.jspider.core.storage.jdbc;


import net.javacoding.jspider.core.storage.spi.*;
import net.javacoding.jspider.core.util.config.PropertySet;
import net.javacoding.jspider.core.logging.LogFactory;

import java.sql.*;


/**
 * $Id: JdbcStorageImpl.java,v 1.8 2003/04/11 16:37:06 vanrogu Exp $
 *
 * @author Günther Van Roey
 *
 * @todo DAO's need caching - definitely !
 * @todo find a good way of cleaning the store at spidering start or layering it per spider session
 */
class JdbcStorageImpl implements StorageSPI {

    protected SiteDAOSPI siteDAO;
    protected ResourceDAOSPI resourceDAO;
    protected ContentDAOSPI contentDAO;
    protected DecisionDAOSPI decisionDAO;
    protected CookieDAOSPI cookieDAO;
    protected EMailAddressDAOSPI emailAddressDAO;
    protected FolderDAOSPI folderDAO;

    public JdbcStorageImpl(PropertySet props) {
        DBUtil dbUtil = new DBUtil(props);
        siteDAO = new SiteDAOImpl(this,dbUtil);
        resourceDAO = new ResourceDAOImpl(this,dbUtil);
        contentDAO = new ContentDAOImpl(this,dbUtil);
        decisionDAO = new DecisionDAOImpl(this,dbUtil);
        cookieDAO = new CookieDAOImpl(this,dbUtil);
        emailAddressDAO = new EMailAddressDAOImpl(this,dbUtil);
        folderDAO = new FolderDAOImpl(this, dbUtil);
        clearDatabase ( dbUtil );
    }

    protected void clearDatabase ( DBUtil dbUtil ) {
        Connection connection = dbUtil.getConnection();
        try {
            Statement st = connection.createStatement();
            st.executeUpdate("delete from jspider_site");
            st.executeUpdate("delete from jspider_resource");
            st.executeUpdate("delete from jspider_content");
            st.executeUpdate("delete from jspider_cookie");
            st.executeUpdate("delete from jspider_resource_reference");
            st.executeUpdate("delete from jspider_decision");
            st.executeUpdate("delete from jspider_decision_step");
            st.executeUpdate("delete from jspider_email_address");
            st.executeUpdate("delete from jspider_email_address_reference");
            st.executeUpdate("delete from jspider_folder");
        } catch (SQLException e) {
            LogFactory.getLog(JdbcStorageImpl.class).error("SQLException during emtpy of database", e);
        }

    }

    public FolderDAOSPI getFolderDAO() {
        return folderDAO;
    }

    public SiteDAOSPI getSiteDAO() {
        return siteDAO;
    }

    public ResourceDAOSPI getResourceDAO() {
        return resourceDAO;
    }

    public ContentDAOSPI getContentDAO() {
        return contentDAO;
    }

    public DecisionDAOSPI getDecisionDAO() {
        return decisionDAO;
    }

    public CookieDAOSPI getCookieDAO() {
        return cookieDAO;
    }

    public EMailAddressDAOSPI getEMailAddressDAO() {
        return emailAddressDAO;
    }

}
