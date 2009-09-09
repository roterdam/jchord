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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Dirk Verbeeck
 * @version $Revision: 1.6 $ $Date: 2004/02/28 11:47:51 $
 */
public class TestDelegatingConnection extends TestCase {
    public TestDelegatingConnection(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingConnection.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private Connection delegateConn2 = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateConn2 = new TesterConnection("test", "test");
        conn = new DelegatingConnection(delegateConn);
    }


    public void testGetDelegate() throws Exception {
        assertEquals(delegateConn,conn.getDelegate());
    }

    public void testHashCodeEqual() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        assertEquals(conn.hashCode(), conn2.hashCode());
    }

    public void testHashCodeNotEqual() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn2);
        assertTrue(conn.hashCode() != conn2.hashCode());
    }
    
    public void testEquals() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        DelegatingConnection conn3 = new DelegatingConnection(null);
        
        assertTrue(!conn.equals(null));
        assertTrue(conn.equals(conn2));
        assertTrue(!conn.equals(conn3));
    }
}
