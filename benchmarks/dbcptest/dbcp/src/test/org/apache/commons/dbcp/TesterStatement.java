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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * A dummy {@link Statement}, for testing purposes.
 * 
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.15 $ $Date: 2004/03/07 10:54:55 $
 */
public class TesterStatement implements Statement {
    public TesterStatement(Connection conn) {
        _connection = conn;
    }

    public TesterStatement(Connection conn, int resultSetType, int resultSetConcurrency) {
        _connection = conn;
        _resultSetType = resultSetType;
        _resultSetConcurrency = resultSetConcurrency;
    }
    
    protected Connection _connection = null;
    protected boolean _open = true;
    protected int _rowsUpdated = 1;
    protected boolean _executeResponse = true;
    protected int _maxFieldSize = 1024;
    protected int _maxRows = 1024;
    protected boolean _escapeProcessing = false;
    protected int _queryTimeout = 1000;
    protected String _cursorName = null;
    protected int _fetchDirection = 1;
    protected int _fetchSize = 1;
    protected int _resultSetConcurrency = 1;
    protected int _resultSetType = 1;
    protected ResultSet _resultSet = null;

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        if("null".equals(sql)) {
            return null;
        } if("invalid".equals(sql)) {
            throw new SQLException("invalid query");
        } if ("broken".equals(sql)) {
            throw new SQLException("broken connection");
        } if("select username".equals(sql)) {
            String username = ((TesterConnection) _connection).getUsername();
            Object[][] data = {{username}};
            return new TesterResultSet(this, data);
        } else {
            return new TesterResultSet(this);
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen();
        return _rowsUpdated;
    }

    public void close() throws SQLException {
        checkOpen();
        _open = false;
        if (_resultSet != null) {
            _resultSet.close();
            _resultSet = null;
        }
    }

    public int getMaxFieldSize() throws SQLException {
        checkOpen();
        return _maxFieldSize;
    }

    public void setMaxFieldSize(int max) throws SQLException {
        checkOpen();
        _maxFieldSize = max;
    }

    public int getMaxRows() throws SQLException {
        checkOpen();
        return _maxRows;
    }

    public void setMaxRows(int max) throws SQLException {
        checkOpen();
        _maxRows = max;
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        checkOpen();
        _escapeProcessing = enable;
    }

    public int getQueryTimeout() throws SQLException {
        checkOpen();
        return _queryTimeout;
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        checkOpen();
        _queryTimeout = seconds;
    }

    public void cancel() throws SQLException {
        checkOpen();
    }

    public SQLWarning getWarnings() throws SQLException {
        checkOpen();
        return null;
    }

    public void clearWarnings() throws SQLException {
        checkOpen();
    }

    public void setCursorName(String name) throws SQLException {
        checkOpen();
        _cursorName = name;
    }

    public boolean execute(String sql) throws SQLException {
        checkOpen();
        return _executeResponse;
    }

    public ResultSet getResultSet() throws SQLException {
        checkOpen();
        if (_resultSet == null) {
            _resultSet = new TesterResultSet(this); 
        }
        return _resultSet;
    }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        return _rowsUpdated;
    }

    public boolean getMoreResults() throws SQLException {
        checkOpen();
        return false;
    }

    public void setFetchDirection(int direction) throws SQLException {
        checkOpen();
        _fetchDirection = direction;
    }

    public int getFetchDirection() throws SQLException {
        checkOpen();
        return _fetchDirection;
    }

    public void setFetchSize(int rows) throws SQLException {
        checkOpen();
        _fetchSize = rows;
    }

    public int getFetchSize() throws SQLException {
        checkOpen();
        return _fetchSize;
    }

    public int getResultSetConcurrency() throws SQLException {
        checkOpen();
        return _resultSetConcurrency;
    }

    public int getResultSetType() throws SQLException {
        checkOpen();
        return _resultSetType;
    }

    public void addBatch(String sql) throws SQLException {
        checkOpen();
    }

    public void clearBatch() throws SQLException {
        checkOpen();
    }

    public int[] executeBatch() throws SQLException {
        checkOpen();
        return new int[0];
    }

    public Connection getConnection() throws SQLException {
        checkOpen();
        return _connection;
    }

    protected void checkOpen() throws SQLException {
        if(!_open) {
            throw new SQLException("Connection is closed.");
        }
    }

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */
    public boolean getMoreResults(int current) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, int columnIndexes[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, String columnNames[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sql, int columnIndexes[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sql, String columnNames[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int getResultSetHoldability() throws SQLException {
        checkOpen();
        throw new SQLException("Not implemented.");
    }
/* JDBC_3_ANT_KEY_END */

}
