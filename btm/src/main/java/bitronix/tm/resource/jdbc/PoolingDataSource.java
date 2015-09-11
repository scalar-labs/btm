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
package bitronix.tm.resource.jdbc;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.utils.ManagementRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of a JDBC {@link DataSource} wrapping vendor's {@link XADataSource} implementation.
 *
 * @author Ludovic Orban
 * @author Brett Wooldridge
 */
@SuppressWarnings("serial")
public class PoolingDataSource extends ResourceBean implements DataSource, XAResourceProducer<JdbcPooledConnection, JdbcPooledConnection>, PoolingDataSourceMBean {

    private final static Logger log = LoggerFactory.getLogger(PoolingDataSource.class);

    private volatile transient XAPool<JdbcPooledConnection, JdbcPooledConnection> pool;
    private volatile transient XADataSource xaDataSource;
    private volatile transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private volatile transient Connection recoveryConnectionHandle;
    private volatile transient Map<XAResource, JdbcPooledConnection> xaResourceHolderMap;

    private volatile String testQuery;
    private volatile boolean enableJdbc4ConnectionTest;
    private volatile int connectionTestTimeout;
    private volatile int preparedStatementCacheSize = 0;
    private volatile String isolationLevel;
    private volatile String cursorHoldability;
    private volatile String localAutoCommit;
    private volatile String jmxName;
    private final List<ConnectionCustomizer> connectionCustomizers = new CopyOnWriteArrayList<ConnectionCustomizer>();

    public PoolingDataSource() {
        xaResourceHolderMap = new ConcurrentHashMap<XAResource, JdbcPooledConnection>();
    }

    /**
     * Initializes the pool by creating the initial amount of connections.
     */
    @Override
    public synchronized void init() {
    	if (this.pool != null)
    		return;

        try {
            buildXAPool();
            this.jmxName = "bitronix.tm:type=JDBC,UniqueName=" + ManagementRegistrar.makeValidName(getUniqueName());
            ManagementRegistrar.register(jmxName, this);
        } catch (Exception ex) {
            throw new ResourceConfigurationException("cannot create JDBC datasource named " + getUniqueName(), ex);
        }
    }

    private void buildXAPool() throws Exception {
        if (pool != null)
            return;

        if (log.isDebugEnabled()) { log.debug("building XA pool for " + getUniqueName() + " with " + getMinPoolSize() + " connection(s)"); }
        pool = new XAPool<JdbcPooledConnection, JdbcPooledConnection>(this, this, xaDataSource);
        boolean builtXaFactory = false;
        if (xaDataSource == null) {
            xaDataSource = (XADataSource) pool.getXAFactory();
            builtXaFactory = true;
        }
        try {
            ResourceRegistrar.register(this);
        } catch (RecoveryException ex) {
            if (builtXaFactory) xaDataSource = null;
            pool = null;
            throw ex;
        }
    }

    /**
     * @return the wrapped XADataSource.
     */
    public XADataSource getXaDataSource() {
        return xaDataSource;
    }

    /**
     * Inject a pre-configured XADataSource instead of relying on className and driverProperties
     * to build one. Upon deserialization the xaDataSource will be null and will need to be
     * manually re-injected.
     * @param xaDataSource the pre-configured XADataSource.
     */
    public void setXaDataSource(XADataSource xaDataSource) {
        this.xaDataSource = xaDataSource;
    }

    /**
     * @return the query that will be used to test connections.
     */
    public String getTestQuery() {
        return testQuery;
    }

    /**
     * When set, the specified query will be executed on the connection acquired from the pool before being handed to
     * the caller. The connections won't be tested when not set. Default value is null.
     * @param testQuery the query that will be used to test connections.
     */
    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }

    /**
     * When set and the underlying JDBC driver supports JDBC 4 isValid(), a Connection.isValid() call
     * is performed to test the connection before handing it to the caller.
     * If both testQuery and enableJdbc4ConnectionTest are set, enableJdbc4ConnectionTest takes precedence.
     * @param enableJdbc4ConnectionTest  true if JDBC 4 isValid() testing should be performed, false otherwise.
     */
    public void setEnableJdbc4ConnectionTest(boolean enableJdbc4ConnectionTest) {
        this.enableJdbc4ConnectionTest = enableJdbc4ConnectionTest;
    }

    /**
     * @return true if JDBC 4 isValid() testing should be performed, false otherwise.
     */
    public boolean isEnableJdbc4ConnectionTest() {
        return enableJdbc4ConnectionTest;
    }

    /**
     * Determines how many seconds the connection test logic
     * will wait for a response from the database.
     * @param connectionTestTimeout connection timeout
     */
    public void setConnectionTestTimeout(int connectionTestTimeout) {
        this.connectionTestTimeout = connectionTestTimeout;
    }

    /**
     * @return how many seconds each connection test will wait for a response.
     */
    public int getConnectionTestTimeout() {
        return connectionTestTimeout;
    }

    /**
     * @return how many seconds each connection test will wait for a response,
     * bounded above by the acquisition timeout.
     */
    public int getEffectiveConnectionTestTimeout() {
        int t1 = getConnectionTestTimeout();
        int t2 = getAcquisitionTimeout();

        if ((t1 > 0) && (t2 > 0)) {
            return Math.min(t1, t2);
        } else {
            return Math.max(t1, t2);
        }
    }

    /**
     * @return the target maximum prepared statement cache size.
     */
    public int getPreparedStatementCacheSize() {
        return preparedStatementCacheSize;
    }

    /**
     * Set the target maximum size of the prepared statement cache.  In
     * reality under certain unusual conditions the cache may temporarily
     * drift higher in size.
     * @param preparedStatementCacheSize the target maximum prepared statement cache size.
     */
    public void setPreparedStatementCacheSize(int preparedStatementCacheSize) {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    /**
     * @return the default isolation level.
     */
    public String getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * Set the default isolation level for connections.
     * @param isolationLevel the default isolation level.
     */
    public void setIsolationLevel(String isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    /**
     * @return cursorHoldability the default cursor holdability.
     */
    public String getCursorHoldability() {
    	return cursorHoldability;
    }

    /**
     * Set the default cursor holdability for connections.
     * @param cursorHoldability the default cursor holdability.
     */
    public void setCursorHoldability(String cursorHoldability) {
    	this.cursorHoldability = cursorHoldability;
    }

    /**
     * @return localAutoCommit the default local transactions autocommit mode.
     */
    public String getLocalAutoCommit() {
    	return localAutoCommit;
    }

    /**
     * Set the default local transactions autocommit mode.
     * @param localAutoCommit the default local transactions autocommit mode.
     */
    public void setLocalAutoCommit(String localAutoCommit) {
    	this.localAutoCommit = localAutoCommit;
    }

    public void addConnectionCustomizer(ConnectionCustomizer connectionCustomizer) {
        connectionCustomizers.add(connectionCustomizer);
    }

    public void removeConnectionCustomizer(ConnectionCustomizer connectionCustomizer) {
        Iterator<ConnectionCustomizer> it = connectionCustomizers.iterator();
        while (it.hasNext()) {
            ConnectionCustomizer customizer = it.next();
            if (customizer == connectionCustomizer) {
                it.remove();
                return;
            }
        }
    }

    void fireOnAcquire(Connection connection) {
        for (ConnectionCustomizer connectionCustomizer : connectionCustomizers) {
            try {
                connectionCustomizer.onAcquire(connection, getUniqueName());
            } catch (Exception ex) {
                log.warn("ConnectionCustomizer.onAcquire() failed for " + connectionCustomizer, ex);
            }
        }
    }

    void fireOnLease(Connection connection){
        for (ConnectionCustomizer connectionCustomizer : connectionCustomizers) {
            try {
                connectionCustomizer.onLease(connection, getUniqueName());
            } catch (Exception ex){
                log.warn("ConnectionCustomizer.onLease() failed for " + connectionCustomizer, ex);
            }
        }
    }

    void fireOnRelease(Connection connection){
        for (ConnectionCustomizer connectionCustomizer : connectionCustomizers) {
            try {
                connectionCustomizer.onRelease(connection, getUniqueName());
            } catch (Exception ex){
                log.warn("ConnectionCustomizer.onRelease() failed for " + connectionCustomizer, ex);
            }
        }
    }

    void fireOnDestroy(Connection connection) {
        for (ConnectionCustomizer connectionCustomizer : connectionCustomizers) {
            try {
                connectionCustomizer.onDestroy(connection, getUniqueName());
            } catch (Exception ex) {
                log.warn("ConnectionCustomizer.onDestroy() failed for " + connectionCustomizer, ex);
            }
        }
    }

    /* Implementation of DataSource interface */
    @Override
    public Connection getConnection() throws SQLException {
        if (isDisabled()) {
            throw new SQLException("JDBC connection pool '" + getUniqueName() + "' is disabled, cannot get a connection from it");
        }

        init();
        if (log.isDebugEnabled()) { log.debug("acquiring connection from " + this); }
        if (pool == null) {
            if (log.isDebugEnabled()) { log.debug("pool is closed, returning null connection"); }
            return null;
        }

        try {
            Connection conn = (Connection) pool.getConnectionHandle();
            if (log.isDebugEnabled()) { log.debug("acquired connection from " + this); }
            return conn;
        } catch (Exception ex) {
            throw new SQLException("unable to get a connection from pool of " + this, ex);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (log.isDebugEnabled()) { log.debug("JDBC connections are pooled, username and password ignored"); }
        return getConnection();
    }

    @Override
	public String toString() {
        return "a PoolingDataSource containing " + pool;
    }


    /* XAResourceProducer implementation */
    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException {
        init();
        if (recoveryConnectionHandle != null)
            throw new RecoveryException("recovery already in progress on " + this);

        try {
            recoveryConnectionHandle = (Connection) pool.getConnectionHandle(false);
            PooledConnectionProxy pooledConnection = (PooledConnectionProxy) recoveryConnectionHandle;
            recoveryXAResourceHolder = pooledConnection.getPooledConnection().createRecoveryXAResourceHolder();
            return new XAResourceHolderState(pooledConnection.getPooledConnection(), this);
        } catch (Exception ex) {
            throw new RecoveryException("cannot start recovery on " + this, ex);
        }
    }

    @Override
    public void endRecovery() throws RecoveryException {
        if (recoveryConnectionHandle == null)
            return;

        try {
            if (log.isDebugEnabled()) { log.debug("recovery xa resource is being closed: " + recoveryXAResourceHolder); }
            recoveryConnectionHandle.close();
        } catch (Exception ex) {
            throw new RecoveryException("error ending recovery on " + this, ex);
        }
        finally {
            recoveryConnectionHandle = null;

            // the recoveryXAResourceHolder actually wraps the recoveryConnectionHandle so closing it
            // would close the recoveryConnectionHandle twice which must not happen
            recoveryXAResourceHolder = null;
        }
    }

    @Override
    public void setFailed(boolean failed) {
        if (pool != null) {
            pool.setFailed(failed);
        }
    }

    @Override
    public boolean isFailed() {
        return (pool != null ? pool.isFailed() : false);
    }

    @Override
    public void close() {
        if (pool == null) {
            if (log.isDebugEnabled()) { log.debug("trying to close already closed PoolingDataSource " + getUniqueName()); }
            return;
        }

        if (log.isDebugEnabled()) { log.debug("closing " + this); }
        pool.close();
        pool = null;

        xaResourceHolderMap.clear();

        connectionCustomizers.clear();

        ManagementRegistrar.unregister(jmxName);
        jmxName = null;

        ResourceRegistrar.unregister(this);
    }

    @Override
    public JdbcPooledConnection createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        if (!(xaFactory instanceof XADataSource))
            throw new IllegalArgumentException("class '" + xaFactory.getClass().getName() + "' does not implement " + XADataSource.class.getName());
        XADataSource xads = (XADataSource) xaFactory;
        JdbcPooledConnection pooledConnection = new JdbcPooledConnection(this, xads.getXAConnection());
        xaResourceHolderMap.put(pooledConnection.getXAResource(), pooledConnection);
        return pooledConnection;
    }

    @Override
    public JdbcPooledConnection findXAResourceHolder(XAResource xaResource) {
        return xaResourceHolderMap.get(xaResource);
    }


    /**
     * {@link PoolingDataSource} must alway have a unique name so this method builds a reference to this object using
     * the unique name as {@link javax.naming.RefAddr}.
     * @return a reference to this {@link PoolingDataSource}.
     */
    @Override
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) { log.debug("creating new JNDI reference of " + this); }
        return new Reference(
                PoolingDataSource.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(),
                null);
    }

    /* DataSource implementation */

    @Override
    public int getLoginTimeout() throws SQLException {
        return xaDataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        xaDataSource.setLoginTimeout(seconds);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return xaDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        xaDataSource.setLogWriter(out);
    }

    /* java.sql.Wrapper implementation */

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(xaDataSource.getClass());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) xaDataSource;
        }
        throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
	}

	/* management */

    @Override
    public int getInPoolSize() {
        return pool.inPoolSize();
    }

    @Override
    public int getTotalPoolSize() {
        return pool.totalPoolSize();
    }

    @Override
    public void reset() throws Exception {
        pool.reset();
    }

    public void unregister(JdbcPooledConnection xaResourceHolder) {
        xaResourceHolderMap.remove(xaResourceHolder.getXAResource());

    }
}
