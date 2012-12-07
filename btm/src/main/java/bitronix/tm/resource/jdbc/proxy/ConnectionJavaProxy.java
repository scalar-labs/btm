/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.resource.jdbc.proxy;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;

/**
 * @author Brett Wooldridge
 */
public class ConnectionJavaProxy extends JavaProxyBase<Connection> implements PooledConnectionProxy {

    private final static Logger log = LoggerFactory.getLogger(ConnectionJavaProxy.class);

    private final static Map<String, Method> selfMethodMap = createMethodMap(ConnectionJavaProxy.class);

    private JdbcPooledConnection jdbcPooledConnection;
    private boolean useStatementCache;

    public ConnectionJavaProxy() {
        // Default constructor
    }

    public ConnectionJavaProxy(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        initialize(jdbcPooledConnection, connection);
    }

    void initialize(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = connection;

        if (jdbcPooledConnection != null) {
            useStatementCache = jdbcPooledConnection.getPoolingDataSource().getPreparedStatementCacheSize() > 0;
        }
    }

    public String toString() {
        return "a ConnectionJavaProxy of " + jdbcPooledConnection + " on " + delegate;
    }

    /* PooledConnectionProxy interface methods */

    public JdbcPooledConnection getPooledConnection() {
        return jdbcPooledConnection;
    }

    public Connection getProxiedDelegate() {
        return delegate;
    }

    /* Overridden methods of java.sql.Connection */

    public void close() throws SQLException {
        if (log.isDebugEnabled()) { log.debug("closing " + this); }

        // in case the connection has already been closed
        if (jdbcPooledConnection == null)
            return;

        jdbcPooledConnection.release();
        jdbcPooledConnection = null;
    }

    public void commit() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot commit a resource enlisted in a global transaction");

        delegate.commit();
    }

    public void rollback() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        delegate.rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot rollback a resource enlisted in a global transaction");

        delegate.rollback(savepoint);
    }

    public Savepoint setSavepoint() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return delegate.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");

        return delegate.setSavepoint(name);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");
        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            throw new SQLException("cannot release a savepoint on a resource enlisted in a global transaction");

        delegate.releaseSavepoint(savepoint);
    }

    public boolean getAutoCommit() throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");

        if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            return false;

        return delegate.getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (jdbcPooledConnection == null)
            throw new SQLException("connection handle already closed");

        if (!jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
            delegate.setAutoCommit(autoCommit);
        else if (autoCommit)
            throw new SQLException("autocommit is not allowed on a resource enlisted in a global transaction");
    }

    public boolean isClosed() throws SQLException {
        if (jdbcPooledConnection == null)
            return true;
        return delegate.isClosed();
    }

    public Statement createStatement() throws SQLException {
        enlistResource();

        Statement statement = delegate.createStatement();
        jdbcPooledConnection.registerUncachedStatement(statement);
        Statement statementProxy = JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency);
        jdbcPooledConnection.registerUncachedStatement(statement);
        Statement statementProxy = JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        jdbcPooledConnection.registerUncachedStatement(statement);
        Statement statementProxy = JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        enlistResource();

        CallableStatement statement = delegate.prepareCall(sql);
        jdbcPooledConnection.registerUncachedStatement(statement);
        CallableStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        jdbcPooledConnection.registerUncachedStatement(statement);
        CallableStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        jdbcPooledConnection.registerUncachedStatement(statement);
        CallableStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
        return statementProxy;
    }

    /* PreparedStatement cache aware methods */

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }
            
            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql, autoGeneratedKeys);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql, autoGeneratedKeys);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }

            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql, autoGeneratedKeys);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql, resultSetType, resultSetConcurrency);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }

            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }

            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql, columnIndexes);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql, columnIndexes);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }

            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql, columnIndexes);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        enlistResource();

        if (useStatementCache) {
            CacheKey cacheKey = new CacheKey(sql, columnNames);
            PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
            if (cachedStmt == null) {
                PreparedStatement stmt = delegate.prepareStatement(sql, columnNames);
                cachedStmt = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, cacheKey);
                jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
            }

            return cachedStmt;
        }
        else {
            PreparedStatement stmt = delegate.prepareStatement(sql, columnNames);
            jdbcPooledConnection.registerUncachedStatement(stmt);
            PreparedStatement statementProxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
            return statementProxy;
        }
    }

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(delegate.getClass())) {
            return (T) delegate;
        }
        if (isWrapperFor(iface)) {
            return unwrap(delegate, iface);
        }
        throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
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
                TransactionContextHelper.enlistInCurrentTransaction(jdbcPooledConnection);
            } catch (SystemException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (SQLException) new SQLException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
