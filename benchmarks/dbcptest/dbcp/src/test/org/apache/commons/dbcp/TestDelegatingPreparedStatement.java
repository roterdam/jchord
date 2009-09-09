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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.6 $ $Date: 2004/02/28 11:47:51 $
 */
public class TestDelegatingPreparedStatement extends TestCase {
    public TestDelegatingPreparedStatement(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingPreparedStatement.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private DelegatingPreparedStatement stmt = null;
    private PreparedStatement delegateStmt = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection(delegateConn);
    }

    public void testExecuteQueryReturnsNull() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"null");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertNull(stmt.executeQuery());
    }

    public void testExecuteQueryReturnsNotNull() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertTrue(null != stmt.executeQuery());
    }

    public void testGetDelegate() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertEquals(delegateStmt,stmt.getDelegate());
    }

    public void testHashCodeNull() {
        stmt = new DelegatingPreparedStatement(conn, null);
        assertEquals(0, stmt.hashCode());
    }
    
    public void testHashCode() {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        DelegatingPreparedStatement stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        DelegatingPreparedStatement stmt2 = new DelegatingPreparedStatement(conn,delegateStmt);
        assertEquals(stmt.hashCode(), stmt2.hashCode());
    }
    
    public void testEquals() {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        DelegatingPreparedStatement stmt = new DelegatingPreparedStatement(conn, delegateStmt);
        DelegatingPreparedStatement stmt2 = new DelegatingPreparedStatement(conn, delegateStmt);
        DelegatingPreparedStatement stmt3 = new DelegatingPreparedStatement(conn, null);
        
        assertTrue(!stmt.equals(null));
        assertTrue(stmt.equals(stmt2));
        assertTrue(!stmt.equals(stmt3));
    }

}
