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
package bitronix.tm.resource.jms;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.utils.ManagementRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of a JMS {@link ConnectionFactory} wrapping vendor's {@link XAConnectionFactory} implementation.
 *
 * @author Ludovic Orban
 */
@SuppressWarnings("serial")
public class PoolingConnectionFactory extends ResourceBean implements ConnectionFactory, XAResourceProducer, PoolingConnectionFactoryMBean {

    private final static Logger log = LoggerFactory.getLogger(PoolingConnectionFactory.class);

    private volatile transient XAPool pool;
    private volatile transient JmsPooledConnection recoveryPooledConnection;
    private volatile transient RecoveryXAResourceHolder recoveryXAResourceHolder;
    private volatile transient List<JmsPooledConnection> xaStatefulHolders;

    private volatile boolean cacheProducersConsumers = true;
    private volatile boolean testConnections = false;
    private volatile String user;
    private volatile String password;
    private volatile JmsConnectionHandle recoveryConnectionHandle;
    private volatile String jmxName;

    public PoolingConnectionFactory() {
        xaStatefulHolders = Collections.synchronizedList(new ArrayList<JmsPooledConnection>());
    }

    /**
     * Initialize the pool by creating the initial amount of connections.
     */
    public synchronized void init() {
        try {
            if (pool != null)
                return;

            buildXAPool();
            this.jmxName = "bitronix.tm:type=JMS,UniqueName=" + ManagementRegistrar.makeValidName(getUniqueName());
            ManagementRegistrar.register(jmxName, this);
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

        if (log.isDebugEnabled()) { log.debug("building JMS XA pool for " + getUniqueName() + " with " + getMinPoolSize() + " connection(s)"); }
        pool = new XAPool(this, this);
        try {
            ResourceRegistrar.register(this);
        } catch (RecoveryException ex) {
            pool = null;
            throw ex;
        }
    }


    public Connection createConnection() throws JMSException {
        if (isDisabled()) {
            throw new JMSException("JMS connection pool '" + getUniqueName() + "' is disabled, cannot get a connection from it");
        }

        try {
            init();
            return (Connection) pool.getConnectionHandle();
        } catch (Exception ex) {
            throw (JMSException) new JMSException("unable to get a connection from pool of " + this).initCause(ex);
        }
    }

    public Connection createConnection(String userName, String password) throws JMSException {
        if (log.isDebugEnabled()) { log.debug("JMS connections are pooled, username and password ignored"); }
        return createConnection();
    }

    public String toString() {
        return "a PoolingConnectionFactory with " + pool;
    }


    /* XAResourceProducer implementation */

    public XAResourceHolderState startRecovery() throws RecoveryException {
        init();
        if (recoveryPooledConnection != null)
            throw new RecoveryException("recovery already in progress on " + this);

        try {
            recoveryConnectionHandle = (JmsConnectionHandle) pool.getConnectionHandle(false);
            recoveryPooledConnection = recoveryConnectionHandle.getPooledConnection();
            recoveryXAResourceHolder = recoveryPooledConnection.createRecoveryXAResourceHolder();
            return new XAResourceHolderState(recoveryXAResourceHolder, recoveryPooledConnection.getPoolingConnectionFactory());
        } catch (Exception ex) {
            throw new RecoveryException("error starting recovery", ex);
        }
    }

    public void endRecovery() throws RecoveryException {
        if (recoveryPooledConnection == null)
            return;

        try {
            if (recoveryConnectionHandle != null) {
                try {
                    if (log.isDebugEnabled()) { log.debug("recovery connection handle is being closed: " + recoveryConnectionHandle); }
                    recoveryConnectionHandle.close();
                } catch (Exception ex) {
                    throw new RecoveryException("error ending recovery", ex);
                }
            }

            if (recoveryXAResourceHolder != null) {
                try {
                    if (log.isDebugEnabled()) { log.debug("recovery xa resource is being closed: " + recoveryXAResourceHolder); }
                    recoveryXAResourceHolder.close();
                } catch (Exception ex) {
                    throw new RecoveryException("error ending recovery", ex);
                }
            }
        }
        finally {
            recoveryConnectionHandle = null;
            recoveryXAResourceHolder = null;
            recoveryPooledConnection = null;
        }
    }

    public void setFailed(boolean failed) {
        pool.setFailed(failed);
    }

    public boolean isFailed() {
        return pool.isFailed();
    }

    public void close() {
        if (pool == null)
            return;

        if (log.isDebugEnabled()) { log.debug("closing " + pool); }
        pool.close();
        pool = null;

        ManagementRegistrar.unregister(jmxName);
        jmxName = null;

        ResourceRegistrar.unregister(this);
    }

    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        if (!(xaFactory instanceof XAConnectionFactory))
            throw new IllegalArgumentException("class '" + xaFactory.getClass().getName() + "' does not implement " + XAConnectionFactory.class.getName());
        XAConnectionFactory xaConnectionFactory = (XAConnectionFactory) xaFactory;

        XAConnection xaConnection;
        if (user == null || password == null) {
            if (log.isDebugEnabled()) { log.debug("creating new JMS XAConnection with no credentials"); }
            xaConnection = xaConnectionFactory.createXAConnection();
        }
        else {
            if (log.isDebugEnabled()) { log.debug("creating new JMS XAConnection with user <" + user + "> and password <" + password + ">"); }
            xaConnection = xaConnectionFactory.createXAConnection(user, password);
        }

        JmsPooledConnection jmsPooledConnection = new JmsPooledConnection(this, xaConnection);
        xaStatefulHolders.add(jmsPooledConnection);
        return jmsPooledConnection;
    }

    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        synchronized (xaStatefulHolders) {
            for (JmsPooledConnection jmsPooledConnection : xaStatefulHolders) {
                XAResourceHolder xaResourceHolder = jmsPooledConnection.getXAResourceHolderForXaResource(xaResource);
                if (xaResourceHolder != null) {
                    return xaResourceHolder;
                }
            }
            return null;
        }
    }

    /* Referenceable implementation */

    /**
     * {@link PoolingConnectionFactory} must alway have a unique name so this method builds a reference to this object
     * using the unique name as {@link javax.naming.RefAddr}.
     * @return a reference to this {@link PoolingConnectionFactory}.
     */
    public Reference getReference() throws NamingException {
        if (log.isDebugEnabled()) { log.debug("creating new JNDI reference of " + this); }
        return new Reference(
                PoolingConnectionFactory.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(),
                null);
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

    public void unregister(JmsPooledConnection jmsPooledConnection) {
        xaStatefulHolders.remove(jmsPooledConnection);
    }
}
