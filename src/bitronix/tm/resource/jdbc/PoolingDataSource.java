package bitronix.tm.resource.jdbc;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.*;
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
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implementation of a JDBC {@link DataSource} wrapping vendor's {@link XADataSource} implementation.
 * <p>Objects of this class are created by {@link DataSourceBean} instances.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class PoolingDataSource implements DataSource, XAResourceProducer {

    private final static Logger log = LoggerFactory.getLogger(PoolingDataSource.class);

    private DataSourceBean bean;
    private transient XAPool pool;
    private transient XADataSource xaDataSource;
    private transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private transient JdbcConnectionHandle recoveryConnectionHandle;

    public PoolingDataSource(DataSourceBean dataSourceBean) throws Exception {
        this.bean = dataSourceBean;
        buildXAPool();
    }

    private void buildXAPool() throws Exception {
        this.pool = new XAPool(this, bean);
        this.xaDataSource = (XADataSource) pool.getXAFactory();
        ResourceRegistrar.register(this);
    }

    public Connection getConnection() throws SQLException {
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
        // ignore username and password.
        return getConnection();
    }

    public String toString() {
        return "a PoolingDataSource with uniqueName " + bean.getUniqueName() + " and " + pool;
    }


    /* RecoverableResourceProducer implementation */

    public String getUniqueName() {
        return bean.getUniqueName();
    }

    public XAResourceHolderState startRecovery() {
        if (recoveryConnectionHandle == null) {
            try {
                recoveryConnectionHandle = (JdbcConnectionHandle) pool.getConnectionHandle(false);
                recoveryXAResourceHolder = recoveryConnectionHandle.getPooledConnection().createRecoveryXAResourceHolder();
            } catch (Exception ex) {
                throw new RecoveryException("cannot start recovery on " + this, ex);
            }
        }
        return new XAResourceHolderState(recoveryConnectionHandle.getPooledConnection(), bean);
    }

    public void endRecovery() {
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

    public void close() {
        if (pool == null) {
            log.warn("trying to close already closed PoolingDataSource " + bean.getUniqueName());
            return;
        }

        ResourceRegistrar.unregister(this);
        if (log.isDebugEnabled()) log.debug("closing " + this);
        pool.close();
        pool = null;
    }

    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        XADataSource xads = (XADataSource) xaFactory;
        return new JdbcPooledConnection(this, xads.getXAConnection(), (DataSourceBean) bean);
    }

    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        return pool.findXAResourceHolder(xaResource);
    }

    /* Referenceable implementation */

    /**
     * PoolingDataSource must alway have a unique name so this method builds a reference to this object using
     * the unique name as RefAddr.
     * @return a reference to this PoolingDataSource
     */
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) log.debug("creating new JNDI reference of " + this);
        return new Reference(
                PoolingDataSource.class.getName(),
                new StringRefAddr("uniqueName", bean.getUniqueName()),
                ResourceFactory.class.getName(),
                null);
    }

    /* deserialization implementation */

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            buildXAPool();
        } catch (Exception ex) {
            throw (IOException) new IOException("error rebuilding XA pool during deserialization").initCause(ex);
        }
    }

    /* DataSource dumb implementation */

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
