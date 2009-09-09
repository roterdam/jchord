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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource
 * 
 * @author Dirk Verbeeck
 * @version $Revision: 1.21 $ $Date: 2004/05/20 13:08:32 $
 */
public class TestOracleBasicDataSource extends TestCase {
    public TestOracleBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestOracleBasicDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected BasicDataSource ds = null;
    private static String CATALOG = "test catalog";

    public void setUp() throws Exception {
        super.setUp();
        ds = new BasicDataSource();
        ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        ds.setUrl("jdbc:oracle:thin:@localhost:1521:DEMO");
        ds.setMaxActive(10);
        ds.setMaxWait(100);
        ds.setDefaultAutoCommit(true);
        ds.setDefaultReadOnly(true);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        // ds.setDefaultCatalog(CATALOG);
        ds.setUsername("SCOTT");
        ds.setPassword("TIGER");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        ds = null;
    }
 
    public void testSimple() throws Exception {
         Connection conn = getConnection();
         assertTrue(null != conn);
         PreparedStatement stmt = conn.prepareStatement("select * from dual");
         assertTrue(null != stmt);
         ResultSet rset = stmt.executeQuery();
         assertTrue(null != rset);
         assertTrue(rset.next());
         rset.close();
         stmt.close();
         conn.close();
     }
   

}
