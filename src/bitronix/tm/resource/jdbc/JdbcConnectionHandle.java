package bitronix.tm.resource.jdbc;

import bitronix.tm.resource.common.TransactionContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.sql.*;
import java.util.Map;

/**
 * Disposable Connection handle.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class JdbcConnectionHandle implements Connection {

    private final static Logger log = LoggerFactory.getLogger(JdbcConnectionHandle.class);

    private JdbcPooledConnection jdbcPooledConnection;
    private Connection connection;

    public JdbcConnectionHandle(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.connection = connection;
    }

    public JdbcPooledConnection getPooledConnection() {
        return jdbcPooledConnection;
    }

    private Connection getConnection() {
        return connection;
    }

    public String toString() {
        return "a JdbcConnectionHandle of " + jdbcPooledConnection + " on " + connection;
    }

    /**
     * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws SQLException
     */
    private void enlistResource() throws SQLException {
        if (jdbcPooledConnection.getPoolingDataSource().getAutomaticEnlistingEnabled()) {
            try {
                TransactionContextHelper.enlistInCurrentTransaction(jdbcPooledConnection, jdbcPooledConnection.getPoolingDataSource());
            } catch (SystemException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    /* wrapped Connection methods that have special XA semantics */

    public void close() throws SQLException {
        if (log.isDebugEnabled()) log.debug("closing " + this);

        // in case the connection has already been closed
        if (jdbcPooledConnection == null)
            return;

        jdbcPooledConnection.release();
        jdbcPooledConnection = null;

        /*
         * connection.close() must happen after jdbcPooledConnection.release() so that the vendor's connection handle
         * doesn't get closed if connection release is vetoed.
         */

        // don't set connection back to null as we want to see JDBC driver error messages when calls to the
        // connection are made after it's been closed.
        connection.close();
    }

    public void commit() throws SQLException {
        if (jdbcPooledConnection == null) {
            getConnection().commit(); // the connection is closed in this case and we want the driver's error message
            throw new SQLException("XA connection handle already closed (JDBC driver should have reported this)");
        }
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot commit a resource enlisted in a global transaction");

        getConnection().commit();
    }

    public void rollback() throws SQLException {
        if (jdbcPooledConnection == null) {
            getConnection().rollback(); // the connection is closed in this case and we want the driver's error message
            throw new SQLException("XA connection handle already closed (JDBC driver should have reported this)");
        }
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getConnection().rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (jdbcPooledConnection == null) {
            getConnection().rollback(savepoint); // the connection is closed in this case and we want the driver's error message
            throw new SQLException("XA connection handle already closed (JDBC driver should have reported this)");
        }
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getConnection().rollback(savepoint);
    }

    public boolean getAutoCommit() throws SQLException {
        if (log.isDebugEnabled()) log.debug("getting autocommit mode of " + this);
        if (jdbcPooledConnection == null) {
            return getConnection().getAutoCommit(); // the connection is closed in this case and we want the driver's error message
        }
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            return false;

        return getConnection().getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (log.isDebugEnabled()) log.debug("setting autocommit mode to " + autoCommit + " on " + this);
        if (jdbcPooledConnection == null) {
            getConnection().setAutoCommit(autoCommit); // the connection is closed in this case and we want the driver's error message
            throw new SQLException("XA connection handle already closed (JDBC driver should have reported this)");
        }
        else if (!jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            getConnection().setAutoCommit(autoCommit);
        else if (autoCommit)
            throw new SQLException("autocommit is not allowed on a resource enlisted in a global transaction");
    }

    public Statement createStatement() throws SQLException {
        enlistResource();
        return getConnection().createStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        return getConnection().createStatement(resultSetType, resultSetConcurrency);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        return getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        enlistResource();
        return getConnection().prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        return getConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        return getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        enlistResource();
        return getConnection().prepareStatement(sql, columnNames);
    }

    /* dumb wrapping of Connection methods */

    public boolean isClosed() throws SQLException {
        return getConnection().isClosed();
    }

    public int getHoldability() throws SQLException {
        return getConnection().getHoldability();
    }

    public int getTransactionIsolation() throws SQLException {
        return getConnection().getTransactionIsolation();
    }

    public void clearWarnings() throws SQLException {
        getConnection().clearWarnings();
    }

    public boolean isReadOnly() throws SQLException {
        return getConnection().isReadOnly();
    }

    public void setHoldability(int holdability) throws SQLException {
        getConnection().setHoldability(holdability);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        getConnection().setTransactionIsolation(level);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        getConnection().setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException {
        return getConnection().getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        getConnection().setCatalog(catalog);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getConnection().getMetaData();
    }

    public SQLWarning getWarnings() throws SQLException {
        return getConnection().getWarnings();
    }

    public Savepoint setSavepoint() throws SQLException {
        return getConnection().setSavepoint();
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        getConnection().releaseSavepoint(savepoint);
    }

    public Map getTypeMap() throws SQLException {
        return getConnection().getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        getConnection().setTypeMap(map);
    }

    public String nativeSQL(String sql) throws SQLException {
        return getConnection().nativeSQL(sql);
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return getConnection().setSavepoint(name);
    }

}
