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
    private Connection delegate;

    public JdbcConnectionHandle(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = connection;
    }

    public JdbcPooledConnection getPooledConnection() {
        return jdbcPooledConnection;
    }

    private Connection getDelegate() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection is closed");
        return delegate;
    }

    public Connection getConnection() {
        return delegate;
    }

    public String toString() {
        return "a JdbcConnectionHandle of " + jdbcPooledConnection + " on " + delegate;
    }

    /**
     * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws SQLException thrown when an error occurs during elistment.
     */
    private void enlistResource() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");

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

        // keep a reference on poolingDataSource for keepConnectionOpenUntilAfter2Pc check
        PoolingDataSource poolingDataSource = jdbcPooledConnection.getPoolingDataSource();

        jdbcPooledConnection.release();
        jdbcPooledConnection = null;
    }

    public void commit() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot commit a resource enlisted in a global transaction");

        getDelegate().commit();
    }

    public void rollback() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getDelegate().rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getDelegate().rollback(savepoint);
    }

    public Savepoint setSavepoint() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return getDelegate().setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return getDelegate().setSavepoint(name);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot release a savepoint on a resource enlisted in a global transaction");

        getDelegate().releaseSavepoint(savepoint);
    }

    public boolean getAutoCommit() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");

        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            return false;

        return getDelegate().getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");

        if (!jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            getDelegate().setAutoCommit(autoCommit);
        else if (autoCommit)
            throw new SQLException("autocommit is not allowed on a resource enlisted in a global transaction");
    }

    public boolean isClosed() throws SQLException {
        if (jdbcPooledConnection == null)
            return true;
        return getDelegate().isClosed();
    }

    public Statement createStatement() throws SQLException {
        enlistResource();
        return getDelegate().createStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        return getDelegate().createStatement(resultSetType, resultSetConcurrency);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        return getDelegate().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        enlistResource();
        return getDelegate().prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        return getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        return getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /* PreparedStatement cache aware methods */

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql, autoGeneratedKeys);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql, columnIndexes);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        enlistResource();
        PreparedStatement stmt = getPooledConnection().getCachedStatement(sql);
        if (stmt != null)
            return new JdbcPreparedStatementHandle(stmt, true);

        stmt = getDelegate().prepareStatement(sql, columnNames);
        boolean cached = getPooledConnection().putCachedStatement(sql, stmt);
        return new JdbcPreparedStatementHandle(stmt, cached);
    }

    /* delegates of Connection methods */

    public int getHoldability() throws SQLException {
        return getDelegate().getHoldability();
    }

    public int getTransactionIsolation() throws SQLException {
        return getDelegate().getTransactionIsolation();
    }

    public void clearWarnings() throws SQLException {
        getDelegate().clearWarnings();
    }

    public boolean isReadOnly() throws SQLException {
        return getDelegate().isReadOnly();
    }

    public void setHoldability(int holdability) throws SQLException {
        getDelegate().setHoldability(holdability);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        getDelegate().setTransactionIsolation(level);
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        getDelegate().setReadOnly(readOnly);
    }

    public String getCatalog() throws SQLException {
        return getDelegate().getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        getDelegate().setCatalog(catalog);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getDelegate().getMetaData();
    }

    public SQLWarning getWarnings() throws SQLException {
        return getDelegate().getWarnings();
    }

    public Map getTypeMap() throws SQLException {
        return getDelegate().getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        getDelegate().setTypeMap(map);
    }

    public String nativeSQL(String sql) throws SQLException {
        return getDelegate().nativeSQL(sql);
    }

}
