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
import java.sql.Statement;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.6 $ $Date: 2004/02/28 11:47:51 $
 */
public class TestDelegatingStatement extends TestCase {
    public TestDelegatingStatement(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingStatement.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private DelegatingStatement stmt = null;
    private Statement delegateStmt = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateStmt = new TesterStatement(delegateConn);
        conn = new DelegatingConnection(delegateConn);
        stmt = new DelegatingStatement(conn,delegateStmt);
    }

    public void testExecuteQueryReturnsNull() throws Exception {
        assertNull(stmt.executeQuery("null"));
    }

    public void testGetDelegate() throws Exception {
        assertEquals(delegateStmt,stmt.getDelegate());
    }

    public void testHashCode() {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        DelegatingStatement stmt = new DelegatingStatement(conn,delegateStmt);
        DelegatingStatement stmt2 = new DelegatingStatement(conn,delegateStmt);
        assertEquals(stmt.hashCode(), stmt2.hashCode());
    }
    
    public void testEquals() {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        DelegatingStatement stmt = new DelegatingStatement(conn, delegateStmt);
        DelegatingStatement stmt2 = new DelegatingStatement(conn, delegateStmt);
        DelegatingStatement stmt3 = new DelegatingStatement(conn, null);
        
        assertTrue(!stmt.equals(null));
        assertTrue(stmt.equals(stmt2));
        assertTrue(!stmt.equals(stmt3));
    }
}
