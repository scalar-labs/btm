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

import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.utils.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * Disposable Connection handle.
 *
 * @author lorban, brettw
 */
public class JdbcConnectionHandle extends BaseProxyHandlerClass { // implements Connection

    private final static Logger log = LoggerFactory.getLogger(JdbcConnectionHandle.class);

    private final JdbcPooledConnection jdbcPooledConnection;
    private final Connection delegate;
    private volatile boolean closed = false;

    public JdbcConnectionHandle(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = connection;
    }

    public JdbcPooledConnection getPooledConnection() {
        return jdbcPooledConnection;
    }

    private Connection getDelegate() throws SQLException {
        if (closed)
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
        if (closed)
            throw new SQLException("connection handle already closed");

        if (jdbcPooledConnection.getPoolingDataSource().getAutomaticEnlistingEnabled()) {
            try {
                TransactionContextHelper.enlistInCurrentTransaction(jdbcPooledConnection);
            } catch (SystemException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    /* Overridden methods of java.sql.Connection */

    public void close() throws SQLException {
        if (log.isDebugEnabled()) log.debug("closing " + this);

        // in case the connection has already been closed
        if (closed)
            return;

        jdbcPooledConnection.release();
        closed = true;
    }

    public void commit() throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot commit a resource enlisted in a global transaction");

        getDelegate().commit();
    }

    public void rollback() throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getDelegate().rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        getDelegate().rollback(savepoint);
    }

    public Savepoint setSavepoint() throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return getDelegate().setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return getDelegate().setSavepoint(name);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot release a savepoint on a resource enlisted in a global transaction");

        getDelegate().releaseSavepoint(savepoint);
    }

    public boolean getAutoCommit() throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");

        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            return false;

        return getDelegate().getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (closed)
            throw new SQLException("connection handle already closed");

        if (!jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            getDelegate().setAutoCommit(autoCommit);
        else if (autoCommit)
            throw new SQLException("autocommit is not allowed on a resource enlisted in a global transaction");
    }

    public boolean isClosed() throws SQLException {
        return closed || getDelegate().isClosed();
    }

    public Statement createStatement() throws SQLException {
        enlistResource();

        Statement statement = getDelegate().createStatement();
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcStatementHandle statementHandle = new JdbcStatementHandle(statement, jdbcPooledConnection);
        return (Statement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{Statement.class}, statementHandle);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        Statement statement = getDelegate().createStatement(resultSetType, resultSetConcurrency);
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcStatementHandle statementHandle = new JdbcStatementHandle(statement, jdbcPooledConnection);
        return (Statement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{Statement.class}, statementHandle);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        Statement statement = getDelegate().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcStatementHandle statementHandle = new JdbcStatementHandle(statement, jdbcPooledConnection);
        return (Statement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{Statement.class}, statementHandle);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        enlistResource();

        CallableStatement statement = getDelegate().prepareCall(sql);
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcCallableStatementHandle statementHandle = new JdbcCallableStatementHandle(statement, jdbcPooledConnection);
        return (CallableStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{CallableStatement.class}, statementHandle);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        CallableStatement statement = getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency);
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcCallableStatementHandle statementHandle = new JdbcCallableStatementHandle(statement, jdbcPooledConnection);
        return (CallableStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{CallableStatement.class}, statementHandle);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        CallableStatement statement = getDelegate().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        jdbcPooledConnection.registerUncachedStatement(statement);
        JdbcCallableStatementHandle statementHandle = new JdbcCallableStatementHandle(statement, jdbcPooledConnection);
        return (CallableStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{CallableStatement.class}, statementHandle);
    }

    /* PreparedStatement cache aware methods */

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql, autoGeneratedKeys);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql, autoGeneratedKeys);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql, autoGeneratedKeys);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql, resultSetType, resultSetConcurrency);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql, columnIndexes);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql, columnIndexes);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql, columnIndexes);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        enlistResource();

        boolean useStatementCache = getPooledConnection().getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        if (useStatementCache) {
            JdbcPreparedStatementHandle proposedStmt = new JdbcPreparedStatementHandle(sql, columnNames);
            JdbcPreparedStatementHandle cachedStmt = getPooledConnection().getCachedStatement(proposedStmt);
            if (cachedStmt == null) {
                PreparedStatement stmt = getDelegate().prepareStatement(sql, columnNames);
                proposedStmt.setDelegate(stmt);
                cachedStmt = getPooledConnection().putCachedStatement(proposedStmt);
            }
            cachedStmt.setPooledConnection(getPooledConnection());
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, cachedStmt);
        }
        else {
            PreparedStatement stmt = getDelegate().prepareStatement(sql, columnNames);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            JdbcUncachedPreparedStatementHandle statementHandle = new JdbcUncachedPreparedStatementHandle(stmt, jdbcPooledConnection);
            return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{PreparedStatement.class}, statementHandle);
        }
    }

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(delegate.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(delegate.getClass())) {
            return (T) delegate;
	    }
	    throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
	}

    /* BaseProxyHandler implementation */

    public Object getProxiedDelegate() throws Exception {
        return delegate;
    }

}
