package bitronix.tm.resource.jms;

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.Decoder;
import bitronix.tm.internal.ManagementRegistrar;
import bitronix.tm.resource.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;
import javax.jms.XAConnection;
import javax.jms.XASession;
import java.util.*;

/**
 * Implementation of a JMS pooled connection wrapping vendor's {@link XAConnection} implementation.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 * TODO: how can the JMS connection be properly tested ?
 */
public class JmsPooledConnection extends AbstractXAStatefulHolder implements StateChangeListener, JmsPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JmsPooledConnection.class);

    private XAConnection xaConnection;
    private ConnectionFactoryBean bean;
    private PoolingConnectionFactory poolingConnectionFactory;
    private final List sessions = Collections.synchronizedList(new ArrayList());

    /* management */
    private String jmxName;
    private Date acquisitionDate;

    protected JmsPooledConnection(PoolingConnectionFactory poolingConnectionFactory, XAConnection connection, ConnectionFactoryBean bean) {
        this.poolingConnectionFactory = poolingConnectionFactory;
        this.xaConnection = connection;
        this.bean = bean;
        addStateChangeEventListener(this);
        this.jmxName = "bitronix.tm:type=JmsPooledConnection,UniqueName=" + bean.getUniqueName() + ",Id=" + bean.incCreatedResourcesCounter();
        ManagementRegistrar.register(jmxName, this);
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

    public ConnectionFactoryBean getBean() {
        return bean;
    }

    public synchronized RecoveryXAResourceHolder createRecoveryXAResourceHolder() throws JMSException {
        DualSessionWrapper dualSessionWrapper = new DualSessionWrapper(this, false, 0);
        dualSessionWrapper.getSession(true); // force creation of XASession to allow access to XAResource
        return new RecoveryXAResourceHolder(dualSessionWrapper);
    }

    public synchronized void close() throws JMSException {
        if (xaConnection != null) {
            setState(STATE_CLOSED);
            xaConnection.close();
        }
        xaConnection = null;
    }

    public List getXAResourceHolders() {
        synchronized (sessions) {
            return new ArrayList(sessions);
        }
    }

    public Object getConnectionHandle() throws Exception {
        setState(STATE_ACCESSIBLE);
        testXAConnection();
        return new JmsConnectionHandle(this, xaConnection);
    }

    private void testXAConnection() throws JMSException {
        boolean testConnection = bean.getTestConnections();
         if (!testConnection) {
            if (log.isDebugEnabled()) log.debug("not testing connection of " + this);
            return;
        }

        if (log.isDebugEnabled()) log.debug("testing connection of " + this);
        XASession xaSession = xaConnection.createXASession();
        TemporaryQueue tq = xaSession.createTemporaryQueue();
        tq.delete();
        xaSession.close();
    }

    protected void release() throws JMSException {
        // sessions cleanup
        closePendingSessions();

        // requeuing
        if (log.isDebugEnabled()) log.debug("releasing to pool " + this);
        try {
            TransactionContextHelper.requeue(this, bean);
        } catch (BitronixSystemException ex) {
            throw (JMSException) new JMSException("error requeueing " + this).initCause(ex);
        }

        if (log.isDebugEnabled()) log.debug("released to pool " + this);
    }

    private void closePendingSessions() {
        for (int i = 0; i < sessions.size(); i++) {
            DualSessionWrapper dualSessionWrapper = (DualSessionWrapper) sessions.get(i);
            if (dualSessionWrapper.getState() != STATE_ACCESSIBLE)
                continue;

            if (log.isDebugEnabled()) log.debug("closing pending session " + dualSessionWrapper);
            try {
                dualSessionWrapper.close();
            } catch (JMSException ex) {
                log.warn("error closing pending session " + dualSessionWrapper, ex);
            }
        }
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("requeued JMS connection of " + poolingConnectionFactory);
            sessions.clear();
        }
        if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
            acquisitionDate = new Date();
        }
        if (newState == STATE_CLOSED) {
            ManagementRegistrar.unregister(jmxName);
        }
    }

    public String toString() {
        return "a JmsPooledConnection of pool " + bean.getUniqueName() + " in state " +
                Decoder.decodeXAStatefulHolderState(getState()) + " with underlying connection " + xaConnection;
    }

    /* management */

    public String getStateDescription() {
        return Decoder.decodeXAStatefulHolderState(state);
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public Collection getTransactionGtridsCurrentlyHoldingThis() {
        List result = new ArrayList();
        synchronized (sessions) {
            for (int i = 0; i < sessions.size(); i++) {
                DualSessionWrapper dsw = (DualSessionWrapper) sessions.get(i);
                String gtrid = ((BitronixXid) dsw.getXAResourceHolderState().getXid()).getGlobalTransactionIdUid().toString();
                result.add(gtrid);
            }
        }
        return result;
    }

    protected DualSessionWrapper createDualSessionWrapper(JmsPooledConnection pooledConnection, boolean transacted, int acknowledgeMode) {
        DualSessionWrapper sessionHandle = new DualSessionWrapper(pooledConnection, transacted, acknowledgeMode);
        sessions.add(sessionHandle);
        return sessionHandle;
    }

    protected DualSessionWrapper getNotAccessibleSession() {
        synchronized (sessions) {
            if (log.isDebugEnabled()) log.debug(sessions.size() + " session(s) open from " + this);
            for (int i = 0; i < sessions.size(); i++) {
                DualSessionWrapper sessionHandle = (DualSessionWrapper) sessions.get(i);
                if (sessionHandle.getState() == XAResourceHolder.STATE_NOT_ACCESSIBLE)
                    return sessionHandle;
            }
            return null;
        }
    }
}
