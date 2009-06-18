package bitronix.tm.resource.jdbc;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
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

/**
 * Implementation of a JDBC {@link DataSource} wrapping vendor's {@link XADataSource} implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class PoolingDataSource extends ResourceBean implements DataSource, XAResourceProducer {

    private final static Logger log = LoggerFactory.getLogger(PoolingDataSource.class);

    private transient XAPool pool;
    private transient XADataSource xaDataSource;
    private transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private transient JdbcConnectionHandle recoveryConnectionHandle;
    private String testQuery;
    private int preparedStatementCacheSize = 0;
    private String isolationLevel;

    public PoolingDataSource() {
    }

    /**
     * Initializes the pool by creating the initial amount of connections.
     */
    public synchronized void init() {
        try {
            buildXAPool();
        } catch (Exception ex) {
            throw new ResourceConfigurationException("cannot create JDBC datasource named " + getUniqueName(), ex);
        }
    }

    private void buildXAPool() throws Exception {
        if (this.pool != null)
            return;

        if (log.isDebugEnabled()) log.debug("building XA pool for " + getUniqueName() + " with " + getMinPoolSize() + " connection(s)");
        this.pool = new XAPool(this, this);
        this.xaDataSource = (XADataSource) pool.getXAFactory();
        ResourceRegistrar.register(this);
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

    public int getPreparedStatementCacheSize() {
        return preparedStatementCacheSize;
    }

    public void setPreparedStatementCacheSize(int preparedStatementCacheSize) {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    public String getIsolationLevel() {
        return isolationLevel;
    }

    public void setIsolationLevel(String isolationLevel) {
        this.isolationLevel = isolationLevel;
    }

    public Connection getConnection() throws SQLException {
        init();
        if (log.isDebugEnabled()) log.debug("acquiring connection from " + this);
        if (pool == null) {
            if (log.isDebugEnabled()) log.debug("pool is closed, returning null connection");
            return null;
        }

        try {
            Connection connectionHandle = (Connection) pool.getConnectionHandle();
            if (log.isDebugEnabled()) log.debug("acquired connection from " + this);
            return connectionHandle;
        } catch (Exception ex) {
            throw (SQLException) new SQLException("unable to get a connection from pool of " + this).initCause(ex);
        }
    }

    public Connection getConnection(String username, String password) throws SQLException {
        if (log.isDebugEnabled()) log.debug("JDBC connections are pooled, username and password ignored");
        return getConnection();
    }

    public String toString() {
        return "a PoolingDataSource containing " + pool;
    }


    /* XAResourceProducer implementation */

    public XAResourceHolderState startRecovery() throws RecoveryException {
        init();
        if (recoveryConnectionHandle == null) {
            try {
                recoveryConnectionHandle = (JdbcConnectionHandle) pool.getConnectionHandle(false);
                recoveryXAResourceHolder = recoveryConnectionHandle.getPooledConnection().createRecoveryXAResourceHolder();
            } catch (Exception ex) {
                throw new RecoveryException("cannot start recovery on " + this, ex);
            }
        }
        return new XAResourceHolderState(recoveryConnectionHandle.getPooledConnection(), this);
    }

    public void endRecovery() throws RecoveryException {
        if (recoveryConnectionHandle == null)
            return;

        try {
            recoveryXAResourceHolder.close();
            recoveryXAResourceHolder = null;
            recoveryConnectionHandle = null;
        } catch (Exception ex) {
            throw new RecoveryException("error ending recovery on " + this, ex);
        }
    }

    public void setFailed(boolean failed) {
        pool.setFailed(failed);
    }

    public void close() {
        if (pool == null) {
            if (log.isDebugEnabled()) log.debug("trying to close already closed PoolingDataSource " + getUniqueName());
            return;
        }

        ResourceRegistrar.unregister(this);
        if (log.isDebugEnabled()) log.debug("closing " + this);
        pool.close();
        pool = null;
    }

    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        if (!(xaFactory instanceof XADataSource))
            throw new IllegalArgumentException("class '" + xaFactory.getClass().getName() + "' does not implement " + XADataSource.class.getName());
        XADataSource xads = (XADataSource) xaFactory;
        return new JdbcPooledConnection(this, xads.getXAConnection());
    }

    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        return pool.findXAResourceHolder(xaResource);
    }


    /**
     * {@link PoolingDataSource} must alway have a unique name so this method builds a reference to this object using
     * the unique name as {@link javax.naming.RefAddr}.
     * @return a reference to this {@link PoolingDataSource}.
     */
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) log.debug("creating new JNDI reference of " + this);
        return new Reference(
                PoolingDataSource.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(),
                null);
    }

    /* DataSource implementation */

    public int getLoginTimeout() throws SQLException {
        return xaDataSource.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        xaDataSource.setLoginTimeout(seconds);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return xaDataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        xaDataSource.setLogWriter(out);
    }

}
