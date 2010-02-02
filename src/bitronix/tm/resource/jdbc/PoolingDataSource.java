package bitronix.tm.resource.jdbc;

import java.io.PrintWriter;
import java.lang.reflect.*;
import java.sql.*;

import javax.naming.*;
import javax.sql.*;
import javax.transaction.xa.XAResource;

import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.ManagementRegistrar;
import org.slf4j.*;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.*;
import bitronix.tm.resource.common.*;

/**
 * Implementation of a JDBC {@link DataSource} wrapping vendor's {@link XADataSource} implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban, brettw
 */
public class PoolingDataSource extends ResourceBean implements DataSource, XAResourceProducer, PoolingDataSourceMBean {

    private final static Logger log = LoggerFactory.getLogger(PoolingDataSource.class);

    private transient XAPool pool;
    private transient XADataSource xaDataSource;
    private transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private transient JdbcConnectionHandle recoveryConnectionHandle;
    private String testQuery;
    private boolean enableJdbc4ConnectionTest;
    private int preparedStatementCacheSize = 0;
    private String isolationLevel;
	private String cursorHoldability;
	private String localAutoCommit;
    private String jmxName;

    public PoolingDataSource() {
    }

    /**
     * Initializes the pool by creating the initial amount of connections.
     */
    public synchronized void init() {
        try {
            if (this.pool != null)
                return;

            buildXAPool();
            this.jmxName = "bitronix.tm:type=JDBC,UniqueName=" + ManagementRegistrar.makeValidName(getUniqueName());
            ManagementRegistrar.register(jmxName, this);
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

    /**
     * When set and the underlying JDBC driver supports JDBC 4 isValid(), a {@link Connection#isValid(int)} call
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


    /* Implementation of DataSource interface */

    public Connection getConnection() throws SQLException {
        init();
        if (log.isDebugEnabled()) log.debug("acquiring connection from " + this);
        if (pool == null) {
            if (log.isDebugEnabled()) log.debug("pool is closed, returning null connection");
            return null;
        }

        try {
        	InvocationHandler connectionHandle = (InvocationHandler) pool.getConnectionHandle();
            if (log.isDebugEnabled()) log.debug("acquired connection from " + this);
            return (Connection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { Connection.class }, connectionHandle);
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
        if (recoveryConnectionHandle != null)
            throw new RecoveryException("recovery already in progress on " + this);

        try {
            recoveryConnectionHandle = (JdbcConnectionHandle) pool.getConnectionHandle(false);
            recoveryXAResourceHolder = recoveryConnectionHandle.getPooledConnection().createRecoveryXAResourceHolder();
            return new XAResourceHolderState(recoveryConnectionHandle.getPooledConnection(), this);
        } catch (Exception ex) {
            throw new RecoveryException("cannot start recovery on " + this, ex);
        }
    }

    public void endRecovery() throws RecoveryException {
        if (recoveryConnectionHandle == null)
            return;

        try {
            if (log.isDebugEnabled()) log.debug("recovery xa resource is being closed: " + recoveryXAResourceHolder);
            recoveryConnectionHandle.close();
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

        if (log.isDebugEnabled()) log.debug("closing " + this);
        pool.close();
        pool = null;

        ManagementRegistrar.unregister(jmxName);
        jmxName = null;

        ResourceRegistrar.unregister(this);
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

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
	    if (xaDataSource instanceof Wrapper) {
	        Wrapper wrapper = (Wrapper) xaDataSource;
	        return wrapper.isWrapperFor(iface);
	    }
		return false;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
	    if (xaDataSource instanceof Wrapper) {
            Wrapper wrapper = (Wrapper) xaDataSource;
            return wrapper.unwrap(iface);
	    }
	    throw new SQLException(xaDataSource + " is not a wrapper for interface " + iface.getCanonicalName());
	}

	/* management */

    public long getInPoolSize() {
        return pool.inPoolSize();
    }

    public long getTotalPoolSize() {
        return pool.totalPoolSize();
    }

    public void reset() throws Exception {
        pool.reset();
    }
}
