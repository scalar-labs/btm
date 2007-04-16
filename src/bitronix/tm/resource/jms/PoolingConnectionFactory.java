package bitronix.tm.resource.jms;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.*;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jms.inbound.asf.BitronixServerSessionPool;

import javax.jms.*;
import javax.naming.Reference;
import javax.naming.NamingException;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.Properties;
import java.util.List;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a JMS {@link ConnectionFactory} wrapping vendor's {@link XAConnectionFactory} implementation.
 * <p>Objects of this class are created by {@link ConnectionFactoryBean} instances.</p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class PoolingConnectionFactory implements ConnectionFactory, XAResourceProducer {

    private final static Logger log = LoggerFactory.getLogger(PoolingConnectionFactory.class);

    private ConnectionFactoryBean bean;
    private transient XAPool pool;
    private transient JmsPooledConnection recoveryPooledConnection;
    private transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private transient BitronixServerSessionPool serverSessionPool;

    public PoolingConnectionFactory(ConnectionFactoryBean connectionFactoryBean) throws Exception {
        this.bean = connectionFactoryBean;

        Properties serverSessionPool = connectionFactoryBean.getServerSessionPool();
        if (serverSessionPool.size() > 0) {
            bean.setPoolSize(bean.getPoolSize() + 1);
            if (log.isDebugEnabled()) log.debug("configuring server session pool, increasing connection pool size by 1 to " + bean.getPoolSize());
        }

        int inboundPoolSize = 0;
        String poolSizeString = connectionFactoryBean.getServerSessionPool().getProperty("poolSize");
        if (poolSizeString != null)
            inboundPoolSize = Integer.parseInt(poolSizeString);

        if (inboundPoolSize > 0) {
            String listenerClassName = serverSessionPool.getProperty("listenerClassName");
            if (log.isDebugEnabled()) log.debug("configuring server session pool, listenerClassName=" + listenerClassName);
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(listenerClassName);
            this.serverSessionPool = new BitronixServerSessionPool(this, clazz, inboundPoolSize);
        }

        buildXAPool();
    }

    private void buildXAPool() throws Exception {
        pool = new XAPool(this, bean);
        ResourceRegistrar.register(this);
    }


    public Connection createConnection() throws JMSException {
        try {
            return (Connection) pool.getConnectionHandle();
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to get a connection from pool of " + this).initCause(ex);
        }
    }

    public Connection createConnection(String userName, String password) throws JMSException {
        // ignore username & password
        return createConnection();
    }

    public String toString() {
        return "a PoolingConnectionFactory with uniqueName " + bean.getUniqueName() + " and " + pool;
    }


    /* RecoverableResourceProducer implementation */

    public String getUniqueName() {
        return bean.getUniqueName();
    }

    public XAResourceHolderState startRecovery() {
        try {
            if (recoveryPooledConnection == null) {
                JmsConnectionHandle connectionHandle = (JmsConnectionHandle) pool.getConnectionHandle(false);
                recoveryPooledConnection = connectionHandle.getPooledConnection();
                recoveryXAResourceHolder = recoveryPooledConnection.createRecoveryXAResourceHolder();
            }
            return new XAResourceHolderState(recoveryXAResourceHolder, recoveryPooledConnection.getBean());
        } catch (Exception ex) {
            throw new RecoveryException("error starting recovery", ex);
        }
    }

    public void endRecovery() {
        if (recoveryPooledConnection == null)
            return;

        try {
            recoveryXAResourceHolder.close();
            recoveryXAResourceHolder = null;
            if (log.isDebugEnabled()) log.debug("releasing recovery connection " + recoveryPooledConnection);
            recoveryPooledConnection.setState(XAStatefulHolder.STATE_IN_POOL);
            recoveryPooledConnection = null;
        } catch (Exception ex) {
            throw new RecoveryException("error ending recovery", ex);
        }
    }

    public void close() {
        if (pool == null)
            return;

        if (log.isDebugEnabled()) log.debug("closing " + pool);
        pool.close();
        pool = null;

        if (serverSessionPool != null) {
            try {
                if (log.isDebugEnabled()) log.debug("closing " + serverSessionPool);
                serverSessionPool.close();
            } catch (JMSException ex) {
                log.error("error closing server session pool", ex);
            }
            serverSessionPool = null;
        }

        ResourceRegistrar.unregister(this);
    }

    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        XAConnectionFactory xaConnectionFactory = (XAConnectionFactory) xaFactory;
        return new JmsPooledConnection(this, xaConnectionFactory.createXAConnection(), (ConnectionFactoryBean) bean);
    }

    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        return pool.findXAResourceHolder(xaResource);
    }

    public List getXAResourceHolders() {
        return pool.getXAResourceHolders();
    }

    /* Referenceable implementation */

    /**
     * PoolingConnectionFactory must alway have a unique name so this method builds a reference to this object using
     * the unique name as RefAddr.
     * @return a reference to this PoolingConnectionFactory.
     */
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) log.debug("creating new JNDI reference of " + this);
        return new Reference(
                PoolingConnectionFactory.class.getName(),
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
    
}
