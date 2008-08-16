package bitronix.tm.resource.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;

/**
 * Caching {@link PreparedStatement} wrapper.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class JdbcPreparedStatementHandle implements PreparedStatement {

    private final static Logger log = LoggerFactory.getLogger(JdbcPreparedStatementHandle.class);

    private PreparedStatement delegate;
    private boolean pretendClosed = false;

    public JdbcPreparedStatementHandle(PreparedStatement delegate) {
        this.delegate = delegate;
    }

    private PreparedStatement getDelegate() throws SQLException {
        if (pretendClosed)
            throw new SQLException("prepared statement closed");
        return delegate;
    }

    public void close() throws SQLException {
        if (log.isDebugEnabled()) log.debug("marking prepared statement handle as closed");
        pretendClosed = true;
    }


    /* delegates of PreparedStatement methods */

    public int executeUpdate() throws SQLException {
        return getDelegate().executeUpdate();
    }

    public void addBatch() throws SQLException {
        getDelegate().addBatch();
    }

    public void clearParameters() throws SQLException {
        getDelegate().clearParameters();
    }

    public boolean execute() throws SQLException {
        return getDelegate().execute();
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        getDelegate().setByte(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        getDelegate().setDouble(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        getDelegate().setFloat(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        getDelegate().setInt(parameterIndex, x);
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        getDelegate().setNull(parameterIndex, sqlType);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        getDelegate().setLong(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        getDelegate().setShort(parameterIndex, x);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        getDelegate().setBoolean(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        getDelegate().setBytes(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getDelegate().setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getDelegate().setBinaryStream(parameterIndex, x, length);
    }

    /**
     * @deprecated
     */
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        getDelegate().setUnicodeStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        getDelegate().setCharacterStream(parameterIndex, reader, length);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        getDelegate().setObject(parameterIndex, x);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        getDelegate().setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        getDelegate().setObject(parameterIndex, x, targetSqlType, scale);
    }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        getDelegate().setNull(paramIndex, sqlType, typeName);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        getDelegate().setString(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        getDelegate().setBigDecimal(parameterIndex, x);
    }

    public void setURL(int parameterIndex, URL x) throws SQLException {
        getDelegate().setURL(parameterIndex, x);
    }

    public void setArray(int i, Array x) throws SQLException {
        getDelegate().setArray(i, x);
    }

    public void setBlob(int i, Blob x) throws SQLException {
        getDelegate().setBlob(i, x);
    }

    public void setClob(int i, Clob x) throws SQLException {
        getDelegate().setClob(i, x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        getDelegate().setDate(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return getDelegate().getParameterMetaData();
    }

    public void setRef(int i, Ref x) throws SQLException {
        getDelegate().setRef(i, x);
    }

    public ResultSet executeQuery() throws SQLException {
        return getDelegate().executeQuery();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return getDelegate().getMetaData();
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        getDelegate().setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        getDelegate().setTimestamp(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        getDelegate().setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        getDelegate().setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        getDelegate().setTimestamp(parameterIndex, x, cal);
    }

    public int getFetchDirection() throws SQLException {
        return getDelegate().getFetchDirection();
    }

    public int getFetchSize() throws SQLException {
        return getDelegate().getFetchSize();
    }

    public int getMaxFieldSize() throws SQLException {
        return getDelegate().getMaxFieldSize();
    }

    public int getMaxRows() throws SQLException {
        return getDelegate().getMaxRows();
    }

    public int getQueryTimeout() throws SQLException {
        return getDelegate().getQueryTimeout();
    }

    public int getResultSetConcurrency() throws SQLException {
        return getDelegate().getResultSetConcurrency();
    }

    public int getResultSetHoldability() throws SQLException {
        return getDelegate().getResultSetHoldability();
    }

    public int getResultSetType() throws SQLException {
        return getDelegate().getResultSetType();
    }

    public int getUpdateCount() throws SQLException {
        return getDelegate().getUpdateCount();
    }

    public void cancel() throws SQLException {
        getDelegate().cancel();
    }

    public void clearBatch() throws SQLException {
        getDelegate().clearBatch();
    }

    public void clearWarnings() throws SQLException {
        getDelegate().clearWarnings();
    }

    public boolean getMoreResults() throws SQLException {
        return getDelegate().getMoreResults();
    }

    public int[] executeBatch() throws SQLException {
        return getDelegate().executeBatch();
    }

    public void setFetchDirection(int direction) throws SQLException {
        getDelegate().setFetchDirection(direction);
    }

    public void setFetchSize(int rows) throws SQLException {
        getDelegate().setFetchSize(rows);
    }

    public void setMaxFieldSize(int max) throws SQLException {
        getDelegate().setMaxFieldSize(max);
    }

    public void setMaxRows(int max) throws SQLException {
        getDelegate().setMaxRows(max);
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        getDelegate().setQueryTimeout(seconds);
    }

    public boolean getMoreResults(int current) throws SQLException {
        return getDelegate().getMoreResults(current);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        getDelegate().setEscapeProcessing(enable);
    }

    public int executeUpdate(String sql) throws SQLException {
        return getDelegate().executeUpdate(sql);
    }

    public void addBatch(String sql) throws SQLException {
        getDelegate().addBatch(sql);
    }

    public void setCursorName(String name) throws SQLException {
        getDelegate().setCursorName(name);
    }

    public boolean execute(String sql) throws SQLException {
        return getDelegate().execute(sql);
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return getDelegate().executeUpdate(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return getDelegate().execute(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return getDelegate().executeUpdate(sql, columnIndexes);
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return getDelegate().execute(sql, columnIndexes);
    }

    public Connection getConnection() throws SQLException {
        return getDelegate().getConnection();
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return getDelegate().getGeneratedKeys();
    }

    public ResultSet getResultSet() throws SQLException {
        return getDelegate().getResultSet();
    }

    public SQLWarning getWarnings() throws SQLException {
        return getDelegate().getWarnings();
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return getDelegate().executeUpdate(sql, columnNames);
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return getDelegate().execute(sql, columnNames);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        return getDelegate().executeQuery(sql);
    }
}
