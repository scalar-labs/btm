package bitronix.tm.resource.jms;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;

/**
 * Implementation of a JMS {@link ConnectionFactory} wrapping vendor's {@link XAConnectionFactory} implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class PoolingConnectionFactory extends ResourceBean implements ConnectionFactory, XAResourceProducer {

    private final static Logger log = LoggerFactory.getLogger(PoolingConnectionFactory.class);

    private transient XAPool pool;
    private transient JmsPooledConnection recoveryPooledConnection;
    private transient RecoveryXAResourceHolder recoveryXAResourceHolder;

    private boolean cacheProducersConsumers = true;
    private boolean testConnections = false;
    private String user;
    private String password;


    public PoolingConnectionFactory() {
    }


    /**
     * Initialize the pool by creating the initial amount of connections.
     */
    public synchronized void init() {
        try {
            buildXAPool();
        }
        catch (Exception ex) {
            throw new ResourceConfigurationException("cannot create JMS connection factory named " + getUniqueName(), ex);
        }
    }

    public boolean getCacheProducersConsumers() {
        return cacheProducersConsumers;
    }

    public void setCacheProducersConsumers(boolean cacheProducersConsumers) {
        this.cacheProducersConsumers = cacheProducersConsumers;
    }

    public boolean getTestConnections() {
        return testConnections;
    }

    public void setTestConnections(boolean testConnections) {
        this.testConnections = testConnections;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    private void buildXAPool() throws Exception {
        if (pool != null)
            return;

        if (log.isDebugEnabled()) log.debug("building JMS XA pool for " + getUniqueName() + " with " + getMinPoolSize() + " connection(s)");
        pool = new XAPool(this, this);
        ResourceRegistrar.register(this);
    }


    public Connection createConnection() throws JMSException {
        try {
            init();
            return (Connection) pool.getConnectionHandle();
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to get a connection from pool of " + this).initCause(ex);
        }
    }

    public Connection createConnection(String userName, String password) throws JMSException {
        if (log.isDebugEnabled()) log.debug("JMS connections are pooled, username and password ignored");
        return createConnection();
    }

    public String toString() {
        return "a PoolingConnectionFactory with " + pool;
    }


    /* XAResourceProducer implementation */

    public XAResourceHolderState startRecovery() throws RecoveryException {
        try {
            init();
            if (recoveryPooledConnection == null) {
                JmsConnectionHandle connectionHandle = (JmsConnectionHandle) pool.getConnectionHandle(false);
                recoveryPooledConnection = connectionHandle.getPooledConnection();
                recoveryXAResourceHolder = recoveryPooledConnection.createRecoveryXAResourceHolder();
            }
            return new XAResourceHolderState(recoveryXAResourceHolder, recoveryPooledConnection.getPoolingConnectionFactory());
        } catch (Exception ex) {
            throw new RecoveryException("error starting recovery", ex);
        }
    }

    public void endRecovery() throws RecoveryException {
        if (recoveryPooledConnection == null)
            return;

        try {
            if (recoveryXAResourceHolder != null) {
                recoveryXAResourceHolder.close();
                recoveryXAResourceHolder = null;
            }
            if (recoveryPooledConnection != null) {
                if (log.isDebugEnabled()) log.debug("releasing recovery connection " + recoveryPooledConnection);
                recoveryPooledConnection.setState(XAStatefulHolder.STATE_IN_POOL);
                recoveryPooledConnection = null;
            }
        } catch (Exception ex) {
            throw new RecoveryException("error ending recovery", ex);
        }
    }

    public void setFailed(boolean failed) {
        pool.setFailed(failed);
    }

    public void close() {
        if (pool == null)
            return;

        if (log.isDebugEnabled()) log.debug("closing " + pool);
        pool.close();
        pool = null;

        ResourceRegistrar.unregister(this);
    }

    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        if (!(xaFactory instanceof XAConnectionFactory))
            throw new IllegalArgumentException("class '" + xaFactory.getClass().getName() + "' does not implement " + XAConnectionFactory.class.getName());
        XAConnectionFactory xaConnectionFactory = (XAConnectionFactory) xaFactory;

        XAConnection xaConnection;
        if (user == null || password == null) {
            if (log.isDebugEnabled()) log.debug("creating new JMS XAConnection with no credentials");
            xaConnection = xaConnectionFactory.createXAConnection();
        }
        else {
            if (log.isDebugEnabled()) log.debug("creating new JMS XAConnection with user <" + user + "> and password <" + password + ">");
            xaConnection = xaConnectionFactory.createXAConnection(user, password);
        }

        return new JmsPooledConnection(this, xaConnection);
    }

    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        return pool.findXAResourceHolder(xaResource);
    }

    /* Referenceable implementation */

    /**
     * {@link PoolingConnectionFactory} must alway have a unique name so this method builds a reference to this object
     * using the unique name as {@link javax.naming.RefAddr}.
     * @return a reference to this {@link PoolingConnectionFactory}.
     */
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) log.debug("creating new JNDI reference of " + this);
        return new Reference(
                PoolingConnectionFactory.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(),
                null);
    }

}
