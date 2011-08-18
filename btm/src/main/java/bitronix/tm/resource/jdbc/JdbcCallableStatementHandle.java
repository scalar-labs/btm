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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * CallableStatement {@link Statement} wrapper.
 * <p/>
 * This class is a proxy handler for a CallableStatement. It does not implement
 * the CallableStatement interface or extend a class directly, but you methods
 * implemented here will override those of the underlying delegate. Simply
 * implement a method with the same signature, and the local method will be
 * called rather than the delegate.
 * <p/>
 * 
 * @author brettw
 */
public class JdbcCallableStatementHandle implements CallableStatement {

	// The 'parent' connection. Used to remove this statement delegate
	// from the un-closed statements list when close() is called.
	private final JdbcPooledConnection parentConnection;

	private final CallableStatement delegate;

	public JdbcCallableStatementHandle(CallableStatement delegate, JdbcPooledConnection pooledConnection) {
		this.delegate = delegate;
		this.parentConnection = pooledConnection;
	}

	protected CallableStatement getDelegate() {
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

	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
		delegate.registerOutParameter(parameterIndex, sqlType);
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

	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
		delegate.registerOutParameter(parameterIndex, sqlType, scale);
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

	public boolean wasNull() throws SQLException {
		return delegate.wasNull();
	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		delegate.setInt(parameterIndex, x);
	}

	public String getString(int parameterIndex) throws SQLException {
		return delegate.getString(parameterIndex);
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

	public boolean getBoolean(int parameterIndex) throws SQLException {
		return delegate.getBoolean(parameterIndex);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		delegate.setDouble(parameterIndex, x);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		delegate.setEscapeProcessing(enable);
	}

	public byte getByte(int parameterIndex) throws SQLException {
		return delegate.getByte(parameterIndex);
	}

	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		delegate.setBigDecimal(parameterIndex, x);
	}

	public int getQueryTimeout() throws SQLException {
		return delegate.getQueryTimeout();
	}

	public short getShort(int parameterIndex) throws SQLException {
		return delegate.getShort(parameterIndex);
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		delegate.setString(parameterIndex, x);
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		delegate.setQueryTimeout(seconds);
	}

	public int getInt(int parameterIndex) throws SQLException {
		return delegate.getInt(parameterIndex);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		delegate.setBytes(parameterIndex, x);
	}

	public void cancel() throws SQLException {
		delegate.cancel();
	}

	public long getLong(int parameterIndex) throws SQLException {
		return delegate.getLong(parameterIndex);
	}

	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	public void setDate(int parameterIndex, Date x) throws SQLException {
		delegate.setDate(parameterIndex, x);
	}

	public float getFloat(int parameterIndex) throws SQLException {
		return delegate.getFloat(parameterIndex);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		delegate.setTime(parameterIndex, x);
	}

	public double getDouble(int parameterIndex) throws SQLException {
		return delegate.getDouble(parameterIndex);
	}

	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		delegate.setTimestamp(parameterIndex, x);
	}

	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		return delegate.getBigDecimal(parameterIndex, scale);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setAsciiStream(parameterIndex, x, length);
	}

	public void setCursorName(String name) throws SQLException {
		delegate.setCursorName(name);
	}

	public byte[] getBytes(int parameterIndex) throws SQLException {
		return delegate.getBytes(parameterIndex);
	}

	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setUnicodeStream(parameterIndex, x, length);
	}

	public Date getDate(int parameterIndex) throws SQLException {
		return delegate.getDate(parameterIndex);
	}

	public boolean execute(String sql) throws SQLException {
		return delegate.execute(sql);
	}

	public Time getTime(int parameterIndex) throws SQLException {
		return delegate.getTime(parameterIndex);
	}

	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		return delegate.getTimestamp(parameterIndex);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		delegate.setBinaryStream(parameterIndex, x, length);
	}

	public ResultSet getResultSet() throws SQLException {
		return delegate.getResultSet();
	}

	public Object getObject(int parameterIndex) throws SQLException {
		return delegate.getObject(parameterIndex);
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

	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		return delegate.getBigDecimal(parameterIndex);
	}

	public void setFetchDirection(int direction) throws SQLException {
		delegate.setFetchDirection(direction);
	}

	public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(i, map);
	}

	public int getFetchDirection() throws SQLException {
		return delegate.getFetchDirection();
	}

	public Ref getRef(int i) throws SQLException {
		return delegate.getRef(i);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		delegate.setObject(parameterIndex, x, targetSqlType);
	}

	public void setFetchSize(int rows) throws SQLException {
		delegate.setFetchSize(rows);
	}

	public Blob getBlob(int i) throws SQLException {
		return delegate.getBlob(i);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		delegate.setObject(parameterIndex, x);
	}

	public Clob getClob(int i) throws SQLException {
		return delegate.getClob(i);
	}

	public int getFetchSize() throws SQLException {
		return delegate.getFetchSize();
	}

	public Array getArray(int i) throws SQLException {
		return delegate.getArray(i);
	}

	public int getResultSetConcurrency() throws SQLException {
		return delegate.getResultSetConcurrency();
	}

	public int getResultSetType() throws SQLException {
		return delegate.getResultSetType();
	}

	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		return delegate.getDate(parameterIndex, cal);
	}

	public boolean execute() throws SQLException {
		return delegate.execute();
	}

	public void addBatch(String sql) throws SQLException {
		delegate.addBatch(sql);
	}

	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		return delegate.getTime(parameterIndex, cal);
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

	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		return delegate.getTimestamp(parameterIndex, cal);
	}

	public void setRef(int i, Ref x) throws SQLException {
		delegate.setRef(i, x);
	}

	public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException {
		delegate.registerOutParameter(paramIndex, sqlType, typeName);
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

	public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
		delegate.registerOutParameter(parameterName, sqlType);
	}

	public boolean getMoreResults(int current) throws SQLException {
		return delegate.getMoreResults(current);
	}

	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		delegate.setDate(parameterIndex, x, cal);
	}

	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
		delegate.registerOutParameter(parameterName, sqlType, scale);
	}

	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		delegate.setTime(parameterIndex, x, cal);
	}

	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
		delegate.registerOutParameter(parameterName, sqlType, typeName);
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

	public URL getURL(int parameterIndex) throws SQLException {
		return delegate.getURL(parameterIndex);
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		return delegate.executeUpdate(sql, columnIndexes);
	}

	public void setURL(String parameterName, URL val) throws SQLException {
		delegate.setURL(parameterName, val);
	}

	public void setNull(String parameterName, int sqlType) throws SQLException {
		delegate.setNull(parameterName, sqlType);
	}

	public void setURL(int parameterIndex, URL x) throws SQLException {
		delegate.setURL(parameterIndex, x);
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		return delegate.executeUpdate(sql, columnNames);
	}

	public void setBoolean(String parameterName, boolean x) throws SQLException {
		delegate.setBoolean(parameterName, x);
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		return delegate.getParameterMetaData();
	}

	public void setByte(String parameterName, byte x) throws SQLException {
		delegate.setByte(parameterName, x);
	}

	public void setShort(String parameterName, short x) throws SQLException {
		delegate.setShort(parameterName, x);
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.execute(sql, autoGeneratedKeys);
	}

	public void setInt(String parameterName, int x) throws SQLException {
		delegate.setInt(parameterName, x);
	}

	public void setLong(String parameterName, long x) throws SQLException {
		delegate.setLong(parameterName, x);
	}

	public void setFloat(String parameterName, float x) throws SQLException {
		delegate.setFloat(parameterName, x);
	}

	public void setDouble(String parameterName, double x) throws SQLException {
		delegate.setDouble(parameterName, x);
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return delegate.execute(sql, columnIndexes);
	}

	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
		delegate.setBigDecimal(parameterName, x);
	}

	public void setString(String parameterName, String x) throws SQLException {
		delegate.setString(parameterName, x);
	}

	public void setBytes(String parameterName, byte[] x) throws SQLException {
		delegate.setBytes(parameterName, x);
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return delegate.execute(sql, columnNames);
	}

	public void setDate(String parameterName, Date x) throws SQLException {
		delegate.setDate(parameterName, x);
	}

	public void setTime(String parameterName, Time x) throws SQLException {
		delegate.setTime(parameterName, x);
	}

	public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
		delegate.setTimestamp(parameterName, x);
	}

	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
		delegate.setAsciiStream(parameterName, x, length);
	}

	public int getResultSetHoldability() throws SQLException {
		return delegate.getResultSetHoldability();
	}

	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
		delegate.setBinaryStream(parameterName, x, length);
	}

	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
		delegate.setObject(parameterName, x, targetSqlType, scale);
	}

	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
		delegate.setObject(parameterName, x, targetSqlType);
	}

	public void setObject(String parameterName, Object x) throws SQLException {
		delegate.setObject(parameterName, x);
	}

	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		delegate.setCharacterStream(parameterName, reader, length);
	}

	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
		delegate.setDate(parameterName, x, cal);
	}

	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
		delegate.setTime(parameterName, x, cal);
	}

	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
		delegate.setTimestamp(parameterName, x, cal);
	}

	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		delegate.setNull(parameterName, sqlType, typeName);
	}

	public String getString(String parameterName) throws SQLException {
		return delegate.getString(parameterName);
	}

	public boolean getBoolean(String parameterName) throws SQLException {
		return delegate.getBoolean(parameterName);
	}

	public byte getByte(String parameterName) throws SQLException {
		return delegate.getByte(parameterName);
	}

	public short getShort(String parameterName) throws SQLException {
		return delegate.getShort(parameterName);
	}

	public int getInt(String parameterName) throws SQLException {
		return delegate.getInt(parameterName);
	}

	public long getLong(String parameterName) throws SQLException {
		return delegate.getLong(parameterName);
	}

	public float getFloat(String parameterName) throws SQLException {
		return delegate.getFloat(parameterName);
	}

	public double getDouble(String parameterName) throws SQLException {
		return delegate.getDouble(parameterName);
	}

	public byte[] getBytes(String parameterName) throws SQLException {
		return delegate.getBytes(parameterName);
	}

	public Date getDate(String parameterName) throws SQLException {
		return delegate.getDate(parameterName);
	}

	public Time getTime(String parameterName) throws SQLException {
		return delegate.getTime(parameterName);
	}

	public Timestamp getTimestamp(String parameterName) throws SQLException {
		return delegate.getTimestamp(parameterName);
	}

	public Object getObject(String parameterName) throws SQLException {
		return delegate.getObject(parameterName);
	}

	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		return delegate.getBigDecimal(parameterName);
	}

	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(parameterName, map);
	}

	public Ref getRef(String parameterName) throws SQLException {
		return delegate.getRef(parameterName);
	}

	public Blob getBlob(String parameterName) throws SQLException {
		return delegate.getBlob(parameterName);
	}

	public Clob getClob(String parameterName) throws SQLException {
		return delegate.getClob(parameterName);
	}

	public Array getArray(String parameterName) throws SQLException {
		return delegate.getArray(parameterName);
	}

	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return delegate.getDate(parameterName, cal);
	}

	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		return delegate.getTime(parameterName, cal);
	}

	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		return delegate.getTimestamp(parameterName, cal);
	}

	public URL getURL(String parameterName) throws SQLException {
		return delegate.getURL(parameterName);
	}
}
