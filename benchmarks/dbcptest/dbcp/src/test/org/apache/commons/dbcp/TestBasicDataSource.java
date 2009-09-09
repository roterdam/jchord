/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource
 * 
 * @author Dirk Verbeeck
 * @version $Revision: 1.21 $ $Date: 2004/05/20 13:08:32 $
 */
public class TestBasicDataSource extends TestConnectionPool {
    public TestBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBasicDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected BasicDataSource ds = null;
    private static String CATALOG = "test catalog";

    public void setUp() throws Exception {
        super.setUp();
        ds = new BasicDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp.TesterDriver");
        ds.setUrl("jdbc:apache:commons:testdriver");
        ds.setMaxActive(getMaxActive());
        ds.setMaxWait(getMaxWait());
        ds.setDefaultAutoCommit(true);
        ds.setDefaultReadOnly(false);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("username");
        ds.setPassword("password");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        ds = null;
    }
    
    public void testTransactionIsolationBehavior() throws Exception {
        Connection conn = getConnection();
        assertTrue(conn != null);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        conn.close();
        
        Connection conn2 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn2.getTransactionIsolation());
        
        Connection conn3 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn3.getTransactionIsolation());

        conn2.close();
        
        conn3.close();
    }

    public void testPooling() throws Exception {
        // this also needs access to the undelying connection
        ds.setAccessToUnderlyingConnectionAllowed(true);
        super.testPooling();
    }    
    
    public void testNoAccessToUnderlyingConnectionAllowed() throws Exception {
        // default: false
        assertEquals(false, ds.isAccessToUnderlyingConnectionAllowed());
        
        Connection conn = getConnection();
        Connection dconn = ((DelegatingConnection) conn).getDelegate();
        assertNull(dconn);
        
        dconn = ((DelegatingConnection) conn).getInnermostDelegate();
        assertNull(dconn);
    }

    public void testAccessToUnderlyingConnectionAllowed() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        assertEquals(true, ds.isAccessToUnderlyingConnectionAllowed());
        
        Connection conn = getConnection();
        Connection dconn = ((DelegatingConnection) conn).getDelegate();
        assertNotNull(dconn);
        
        dconn = ((DelegatingConnection) conn).getInnermostDelegate();
        assertNotNull(dconn);
        
        assertTrue(dconn instanceof TesterConnection);
    }
    
    public void testEmptyValidationQuery() throws Exception {
        assertNotNull(ds.getValidationQuery());
        
        ds.setValidationQuery("");
        assertNull(ds.getValidationQuery());

        ds.setValidationQuery("   ");
        assertNull(ds.getValidationQuery());
    }

    public void testInvalidValidationQuery() {
        try {
            ds.setValidationQuery("invalid");
            ds.getConnection();
            fail("expected SQLException");
        }
        catch (SQLException e) {
            if (e.toString().indexOf("invalid") < 0) {
                fail("expected detailed error message");
            }
        }
    }

    public void testSetValidationTestProperties() {
        // defaults
        assertEquals(true, ds.getTestOnBorrow());
        assertEquals(false, ds.getTestOnReturn());
        assertEquals(false, ds.getTestWhileIdle());

        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setTestWhileIdle(true);
        assertEquals(true, ds.getTestOnBorrow());
        assertEquals(true, ds.getTestOnReturn());
        assertEquals(true, ds.getTestWhileIdle());

        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setTestWhileIdle(false);
        assertEquals(false, ds.getTestOnBorrow());
        assertEquals(false, ds.getTestOnReturn());
        assertEquals(false, ds.getTestWhileIdle());
    }

    public void testNoValidationQuery() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setTestWhileIdle(true);
        ds.setValidationQuery("");
        
        Connection conn = ds.getConnection();
        conn.close();

        assertEquals(false, ds.getTestOnBorrow());
        assertEquals(false, ds.getTestOnReturn());
        assertEquals(false, ds.getTestWhileIdle());
    }
    
    public void testDefaultCatalog() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertTrue(c[i] != null);
            assertEquals(CATALOG, c[i].getCatalog()); 
        }

        for (int i = 0; i < c.length; i++) {
            c[i].setCatalog("error");
            c[i].close();
        }
        
        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertTrue(c[i] != null);
            assertEquals(CATALOG, c[i].getCatalog()); 
        }        

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }
    
    public void testSetAutoCommitTrueOnClose() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ds.setDefaultAutoCommit(false);
        
        Connection conn = getConnection();
        assertNotNull(conn);
        assertEquals(false, conn.getAutoCommit());

        Connection dconn = ((DelegatingConnection) conn).getInnermostDelegate();
        assertNotNull(dconn);
        assertEquals(false, dconn.getAutoCommit());

        conn.close();

        assertEquals(true, dconn.getAutoCommit());
    }

    public void testInitialSize() throws Exception {
        ds.setMaxActive(20);
        ds.setMaxIdle(20);
        ds.setInitialSize(10);

        Connection conn = getConnection();
        assertNotNull(conn);
        conn.close();

        assertEquals(0, ds.getNumActive());
        assertEquals(10, ds.getNumIdle());
    }

    // Bugzilla Bug 28251:  Returning dead database connections to BasicDataSource
    // isClosed() failure blocks returning a connection to the pool 
    public void testIsClosedFailure() throws SQLException {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        assertEquals(1, ds.getNumActive());
        
        // set an IO failure causing the isClosed mathod to fail
        TesterConnection tconn = (TesterConnection) ((DelegatingConnection)conn).getInnermostDelegate();
        tconn.setFailure(new IOException("network error"));
        
        try {
            conn.close();
            fail("Expected SQLException");
        }
        catch(SQLException ex) { }
        
        assertEquals(0, ds.getNumActive());
    }
    
    /** 
     * Bugzilla Bug 29054: 
     * The BasicDataSource.setTestOnReturn(boolean) is not carried through to 
     * the GenericObjectPool variable _testOnReturn.
     */ 
    public void testPropertyTestOnReturn() throws Exception {
        ds.setValidationQuery("select 1 from dual");
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(false);
        ds.setTestOnReturn(true);
        
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        
        assertEquals(false, ds.connectionPool.getTestOnBorrow());
        assertEquals(false, ds.connectionPool.getTestWhileIdle());
        assertEquals(true, ds.connectionPool.getTestOnReturn());
    }
    
    /**
     * Bugzilla Bug 29055: AutoCommit and ReadOnly
     * The DaffodilDB driver throws an SQLException if 
     * trying to commit or rollback a readOnly connection. 
     */
    public void testRollbackReadOnly() throws Exception {
        ds.setDefaultReadOnly(true);
        ds.setDefaultAutoCommit(false);
        
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        conn.close();
    }
}
