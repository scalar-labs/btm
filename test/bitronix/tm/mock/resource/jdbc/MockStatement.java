package bitronix.tm.mock.resource.jdbc;

import java.sql.*;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockStatement implements Statement {
    public ResultSet executeQuery(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int executeUpdate(String sql) throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getMaxFieldSize() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMaxFieldSize(int max) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getMaxRows() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMaxRows(int max) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getQueryTimeout() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cancel() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearWarnings() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCursorName(String name) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean execute(String sql) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResultSet getResultSet() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getUpdateCount() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getMoreResults() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFetchDirection(int direction) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFetchDirection() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFetchSize(int rows) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFetchSize() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getResultSetConcurrency() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getResultSetType() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addBatch(String sql) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearBatch() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int[] executeBatch() throws SQLException {
        return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Connection getConnection() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getMoreResults(int current) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean execute(String sql, String columnNames[]) throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getResultSetHoldability() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
