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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Mock object implementing the <code>java.sql.Driver</code> interface.
 * Returns <code>TestConnection</code>'s from getConnection methods.  
 * Valid username, password combinations are:
 *
 * <table>
 * <tr><th>user</th><th>password</th></tr>
 * <tr><td>foo</td><td>bar</td></tr>
 * <tr><td>u1</td><td>p1</td></tr>
 * <tr><td>u2</td><td>p2</td></tr>
 * <tr><td>username</td><td>password</td></tr>
 * </table>
 * 
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.7 $ $Date: 2004/02/28 11:47:52 $
 */
public class TesterDriver implements Driver {
    private static Properties validUserPasswords = new Properties();
    static {
        try {
            DriverManager.registerDriver(new TesterDriver());
        } catch(Exception e) {
        }
        validUserPasswords.put("foo", "bar");
        validUserPasswords.put("u1", "p1");
        validUserPasswords.put("u2", "p2");
        validUserPasswords.put("username", "password");
    }

    /** 
     * TesterDriver specific method to add users to the list of valid users 
     */
    public static void addUser(String username, String password) {
        validUserPasswords.put(username, password);
    }

    public boolean acceptsURL(String url) throws SQLException {
        return CONNECT_STRING.startsWith(url);
    }

    private void assertValidUserPassword(String user, String password) 
        throws SQLException {
        String realPassword = validUserPasswords.getProperty(user);
        if (realPassword == null) 
        {
            throw new SQLException(user + " is not a valid username.");
        }
        if (!realPassword.equals(password)) 
        {
            throw new SQLException(password + 
                                   " is not the correct password for " +
                                   user + ".  The correct password is " +
                                   realPassword);
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        //return (acceptsURL(url) ? new TesterConnection() : null);
        Connection conn = null;
        if (acceptsURL(url)) 
        {
            String username = "test";
            String password = "test";
            if (info != null) 
            {
                username = info.getProperty("user");
                password = info.getProperty("password");
                assertValidUserPassword(username, password);
            }
            conn = new TesterConnection(username, password);
        }
        
        return conn;
    }

    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public boolean jdbcCompliant() {
        return true;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    protected static String CONNECT_STRING = "jdbc:apache:commons:testdriver";

    // version numbers
    protected static int MAJOR_VERSION = 1;
    protected static int MINOR_VERSION = 0;

}
