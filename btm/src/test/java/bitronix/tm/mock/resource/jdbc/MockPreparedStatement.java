package bitronix.tm.mock.resource.jdbc;

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

public class MockPreparedStatement implements PreparedStatement {

    public void addBatch(String arg0) throws SQLException {
    }

    public void cancel() throws SQLException {
    }

    public void clearBatch() throws SQLException {
    }

    public void clearWarnings() throws SQLException {
    }

    public void close() throws SQLException {
    }

    public boolean execute(String arg0) throws SQLException {
        return false;
    }

    public boolean execute(String arg0, int arg1) throws SQLException {
        return false;
    }

    public boolean execute(String arg0, int[] arg1) throws SQLException {
        return false;
    }

    public boolean execute(String arg0, String[] arg1) throws SQLException {
        return false;
    }

    public int[] executeBatch() throws SQLException {
        return null;
    }

    public ResultSet executeQuery(String arg0) throws SQLException {
        return null;
    }

    public int executeUpdate(String arg0) throws SQLException {
        return 0;
    }

    public int executeUpdate(String arg0, int arg1) throws SQLException {
        return 0;
    }

    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        return 0;
    }

    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        return 0;
    }

    public Connection getConnection() throws SQLException {
        return null;
    }

    public int getFetchDirection() throws SQLException {
        return 0;
    }

    public int getFetchSize() throws SQLException {
        return 0;
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    public int getMaxRows() throws SQLException {
        return 0;
    }

    public boolean getMoreResults() throws SQLException {
        return false;
    }

    public boolean getMoreResults(int arg0) throws SQLException {
        return false;
    }

    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    public ResultSet getResultSet() throws SQLException {
        return null;
    }

    public int getResultSetConcurrency() throws SQLException {
        return 0;
    }

    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    public int getResultSetType() throws SQLException {
        return 0;
    }

    public int getUpdateCount() throws SQLException {
        return 0;
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    public void setCursorName(String arg0) throws SQLException {
    }

    public void setEscapeProcessing(boolean arg0) throws SQLException {
    }

    public void setFetchDirection(int arg0) throws SQLException {
    }

    public void setFetchSize(int arg0) throws SQLException {
    }

    public void setMaxFieldSize(int arg0) throws SQLException {
    }

    public void setMaxRows(int arg0) throws SQLException {
    }

    public void setQueryTimeout(int arg0) throws SQLException {
    }

    public void addBatch() throws SQLException {
    }

    public void clearParameters() throws SQLException {
    }

    public boolean execute() throws SQLException {
        return false;
    }

    public ResultSet executeQuery() throws SQLException {
        return null;
    }

    public int executeUpdate() throws SQLException {
        return 0;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    public ParameterMetaData getParameterMetaData() throws SQLException {
        return null;
    }

    public void setArray(int arg0, Array arg1) throws SQLException {
    }

    public void setAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
    }

    public void setBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
    }

    public void setBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
    }

    public void setBlob(int arg0, Blob arg1) throws SQLException {
    }

    public void setBoolean(int arg0, boolean arg1) throws SQLException {
    }

    public void setByte(int arg0, byte arg1) throws SQLException {
    }

    public void setBytes(int arg0, byte[] arg1) throws SQLException {
    }

    public void setCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
    }

    public void setClob(int arg0, Clob arg1) throws SQLException {
    }

    public void setDate(int arg0, Date arg1) throws SQLException {
    }

    public void setDate(int arg0, Date arg1, Calendar arg2) throws SQLException {
    }

    public void setDouble(int arg0, double arg1) throws SQLException {
    }

    public void setFloat(int arg0, float arg1) throws SQLException {
    }

    public void setInt(int arg0, int arg1) throws SQLException {
    }

    public void setLong(int arg0, long arg1) throws SQLException {
    }

    public void setNull(int arg0, int arg1) throws SQLException {
    }

    public void setNull(int arg0, int arg1, String arg2) throws SQLException {
    }

    public void setObject(int arg0, Object arg1) throws SQLException {
    }

    public void setObject(int arg0, Object arg1, int arg2) throws SQLException {
    }

    public void setObject(int arg0, Object arg1, int arg2, int arg3) throws SQLException {
    }

    public void setRef(int arg0, Ref arg1) throws SQLException {
    }

    public void setShort(int arg0, short arg1) throws SQLException {
    }

    public void setString(int arg0, String arg1) throws SQLException {
    }

    public void setTime(int arg0, Time arg1) throws SQLException {
    }

    public void setTime(int arg0, Time arg1, Calendar arg2) throws SQLException {
    }

    public void setTimestamp(int arg0, Timestamp arg1) throws SQLException {
    }

    public void setTimestamp(int arg0, Timestamp arg1, Calendar arg2) throws SQLException {
    }

    public void setURL(int arg0, URL arg1) throws SQLException {
    }

    public void setUnicodeStream(int arg0, InputStream arg1, int arg2) throws SQLException {
    }
}
