/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.resource.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class JdbcUncachedPreparedStatementHandle implements PreparedStatement {

    // The 'parent' connection. Used to remove this statement delegate
    // from the un-closed statements list when close() is called.
    private final JdbcPooledConnection parentConnection;

    private final PreparedStatement delegate;

    public JdbcUncachedPreparedStatementHandle(PreparedStatement delegate, JdbcPooledConnection pooledConnection) {
        this.delegate = delegate;
        this.parentConnection = pooledConnection;
    }

    protected PreparedStatement getDelegate() {
        return delegate;
    }

    /* Overridden methods of java.sql.PreparedStatement */

    public void close() throws SQLException {
        parentConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }

    /* Delegated methods */

	public ResultSet executeQuery(String sql) throws SQLException {
		return delegate.executeQuery(sql);
	}

	public ResultSet executeQuery() throws SQLException {
		return delegate.executeQuery();
	}

	public int executeUpdate(String sql) throws SQLException {
		return delegate.executeUpdate(sql);
	}

	public int executeUpdate() throws SQLException {
		return delegate.executeUpdate();
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		delegate.setNull(parameterIndex, sqlType);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		delegate.setBoolean(parameterIndex, x);
	}

	public int getMaxFieldSize() throws SQLException {
		return delegate.getMaxFieldSize();
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		delegate.setByte(parameterIndex, x);
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		delegate.setShort(parameterIndex, x);
	}

	public void setMaxFieldSize(int max) throws SQLException {
		delegate.setMaxFieldSize(max);
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		delegate.setInt(parameterIndex, x);
	}

	public int getMaxRows() throws SQLException {
		return delegate.getMaxRows();
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		delegate.setLong(parameterIndex, x);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		delegate.setFloat(parameterIndex, x);
	}

	public void setMaxRows(int max) throws SQLException {
		delegate.setMaxRows(max);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		delegate.setDouble(parameterIndex, x);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		delegate.setEscapeProcessing(enable);
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		delegate.setBigDecimal(parameterIndex, x);
	}

	public int getQueryTimeout() throws SQLException {
		return delegate.getQueryTimeout();
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		delegate.setString(parameterIndex, x);
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		delegate.setQueryTimeout(seconds);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		delegate.setBytes(parameterIndex, x);
	}

	public void cancel() throws SQLException {
		delegate.cancel();
	}

	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		delegate.setDate(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		delegate.setTime(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		delegate.setTimestamp(parameterIndex, x);
	}

	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setAsciiStream(parameterIndex, x, length);
	}

	public void setCursorName(String name) throws SQLException {
		delegate.setCursorName(name);
	}

	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setUnicodeStream(parameterIndex, x, length);
	}

	public boolean execute(String sql) throws SQLException {
		return delegate.execute(sql);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setBinaryStream(parameterIndex, x, length);
	}

	public ResultSet getResultSet() throws SQLException {
		return delegate.getResultSet();
	}

	public int getUpdateCount() throws SQLException {
		return delegate.getUpdateCount();
	}

	public void clearParameters() throws SQLException {
		delegate.clearParameters();
	}

	public boolean getMoreResults() throws SQLException {
		return delegate.getMoreResults();
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
		delegate.setObject(parameterIndex, x, targetSqlType, scale);
	}

	public void setFetchDirection(int direction) throws SQLException {
		delegate.setFetchDirection(direction);
	}

	public int getFetchDirection() throws SQLException {
		return delegate.getFetchDirection();
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		delegate.setObject(parameterIndex, x, targetSqlType);
	}

	public void setFetchSize(int rows) throws SQLException {
		delegate.setFetchSize(rows);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		delegate.setObject(parameterIndex, x);
	}

	public int getFetchSize() throws SQLException {
		return delegate.getFetchSize();
	}

	public int getResultSetConcurrency() throws SQLException {
		return delegate.getResultSetConcurrency();
	}

	public int getResultSetType() throws SQLException {
		return delegate.getResultSetType();
	}

	public boolean execute() throws SQLException {
		return delegate.execute();
	}

	public void addBatch(String sql) throws SQLException {
		delegate.addBatch(sql);
	}

	public void clearBatch() throws SQLException {
		delegate.clearBatch();
	}

	public void addBatch() throws SQLException {
		delegate.addBatch();
	}

	public int[] executeBatch() throws SQLException {
		return delegate.executeBatch();
	}

	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		delegate.setCharacterStream(parameterIndex, reader, length);
	}

	public void setRef(int i, Ref x) throws SQLException {
		delegate.setRef(i, x);
	}

	public void setBlob(int i, Blob x) throws SQLException {
		delegate.setBlob(i, x);
	}

	public void setClob(int i, Clob x) throws SQLException {
		delegate.setClob(i, x);
	}

	public Connection getConnection() throws SQLException {
		return delegate.getConnection();
	}

	public void setArray(int i, Array x) throws SQLException {
		delegate.setArray(i, x);
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return delegate.getMetaData();
	}

	public boolean getMoreResults(int current) throws SQLException {
		return delegate.getMoreResults(current);
	}

	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		delegate.setDate(parameterIndex, x, cal);
	}

	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		delegate.setTime(parameterIndex, x, cal);
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return delegate.getGeneratedKeys();
	}

	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		delegate.setTimestamp(parameterIndex, x, cal);
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.executeUpdate(sql, autoGeneratedKeys);
	}

	public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
		delegate.setNull(paramIndex, sqlType, typeName);
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return delegate.executeUpdate(sql, columnIndexes);
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		delegate.setURL(parameterIndex, x);
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return delegate.executeUpdate(sql, columnNames);
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		return delegate.getParameterMetaData();
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.execute(sql, autoGeneratedKeys);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return delegate.execute(sql, columnIndexes);
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return delegate.execute(sql, columnNames);
	}

	public int getResultSetHoldability() throws SQLException {
		return delegate.getResultSetHoldability();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
}
