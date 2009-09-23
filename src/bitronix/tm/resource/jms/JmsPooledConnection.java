package bitronix.tm.resource.jms;

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jms.lrc.LrcXAConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.*;

/**
 * Implementation of a JMS pooled connection wrapping vendor's {@link XAConnection} implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 * TODO: how can the JMS connection properly be tested ?
 */
public class JmsPooledConnection extends AbstractXAStatefulHolder implements JmsPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JmsPooledConnection.class);

    private XAConnection xaConnection;
    private PoolingConnectionFactory poolingConnectionFactory;
    private final List sessions = Collections.synchronizedList(new ArrayList());

    /* management */
    private String jmxName;
    private Date acquisitionDate;
    private Date lastReleaseDate;

    protected JmsPooledConnection(PoolingConnectionFactory poolingConnectionFactory, XAConnection connection) {
        this.poolingConnectionFactory = poolingConnectionFactory;
        this.xaConnection = connection;
        addStateChangeEventListener(new JmsPooledConnectionStateChangeListener());
        
        if (poolingConnectionFactory.getClassName().equals(LrcXAConnectionFactory.class.getName())) {
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing twoPcOrderingPosition to " + Scheduler.ALWAYS_LAST_POSITION);
            poolingConnectionFactory.setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing deferConnectionRelease to true");
            poolingConnectionFactory.setDeferConnectionRelease(true);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing useTmJoin to true");
            poolingConnectionFactory.setUseTmJoin(true);
        }
        
        this.jmxName = "bitronix.tm:type=JmsPooledConnection,UniqueName=" + ManagementRegistrar.makeValidName(poolingConnectionFactory.getUniqueName()) + ",Id=" + poolingConnectionFactory.incCreatedResourcesCounter();
        ManagementRegistrar.register(jmxName, this);
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

    public PoolingConnectionFactory getPoolingConnectionFactory() {
        return poolingConnectionFactory;
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
        if (log.isDebugEnabled()) log.debug("getting connection handle from " + this);
        int oldState = getState();

        setState(STATE_ACCESSIBLE);

        if (oldState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("connection " + xaConnection + " was in state IN_POOL, testing it");
            testXAConnection();
        }
        else {
            if (log.isDebugEnabled()) log.debug("connection " + xaConnection + " was in state " + Decoder.decodeXAStatefulHolderState(oldState) + ", no need to test it");
        }

        if (log.isDebugEnabled()) log.debug("got connection handle from " + this);
        return new JmsConnectionHandle(this, xaConnection);
    }

    private void testXAConnection() throws JMSException {
        if (!poolingConnectionFactory.getTestConnections()) {
            if (log.isDebugEnabled()) log.debug("not testing connection of " + this);
            return;
        }

        if (log.isDebugEnabled()) log.debug("testing connection of " + this);
        XASession xaSession = xaConnection.createXASession();
        try {
            TemporaryQueue tq = xaSession.createTemporaryQueue();
            tq.delete();
        } finally {
            xaSession.close();
        }
    }

    protected void release() throws JMSException {
        if (log.isDebugEnabled()) log.debug("releasing to pool " + this);
        closePendingSessions();

        // requeuing
        try {
            TransactionContextHelper.requeue(this, poolingConnectionFactory);
        } catch (BitronixSystemException ex) {
            throw (JMSException) new JMSException("error requeueing " + this).initCause(ex);
        }

        if (log.isDebugEnabled()) log.debug("released to pool " + this);
    }

    private void closePendingSessions() {
        synchronized (sessions) {
            for (int i = 0; i < sessions.size(); i++) {
                DualSessionWrapper dualSessionWrapper = (DualSessionWrapper) sessions.get(i);
                if (dualSessionWrapper.getState() != STATE_ACCESSIBLE)
                    continue;

                try {
                    if (log.isDebugEnabled()) log.debug("trying to close pending session " + dualSessionWrapper);
                    dualSessionWrapper.close();
                } catch (JMSException ex) {
                    log.warn("error closing pending session " + dualSessionWrapper, ex);
                }
            }
        }
    }

    protected Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        DualSessionWrapper sessionHandle = getNotAccessibleSession();

        if (sessionHandle == null) {
            if (log.isDebugEnabled()) log.debug("no session handle found in NOT_ACCESSIBLE state, creating new session");
            sessionHandle = new DualSessionWrapper(this, transacted, acknowledgeMode);
            sessionHandle.addStateChangeEventListener(new JmsConnectionHandleStateChangeListener());
            synchronized (sessions) {
                sessions.add(sessionHandle);
            }
        }
        else {
            if (log.isDebugEnabled()) log.debug("found session handle in NOT_ACCESSIBLE state, recycling it: " + sessionHandle);
            sessionHandle.setState(XAResourceHolder.STATE_ACCESSIBLE);
        }

        return sessionHandle;
    }

     private DualSessionWrapper getNotAccessibleSession() {
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

    public Date getLastReleaseDate() {
        return lastReleaseDate;
    }

    public String toString() {
        synchronized (sessions) {
            return "a JmsPooledConnection of pool " + poolingConnectionFactory.getUniqueName() + " in state " +
                    Decoder.decodeXAStatefulHolderState(getState()) + " with underlying connection " + xaConnection +
                    " with " + sessions.size() + " opened session(s)";
        }
    }

    /* management */

    public String getStateDescription() {
        return Decoder.decodeXAStatefulHolderState(getState());
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public Collection getTransactionGtridsCurrentlyHoldingThis() {
        synchronized (sessions) {
            List result = new ArrayList();
            for (int i = 0; i < sessions.size(); i++) {
                DualSessionWrapper dsw = (DualSessionWrapper) sessions.get(i);
                String gtrid = dsw.getXAResourceHolderState().getXid().getGlobalTransactionIdUid().toString();
                result.add(gtrid);
            }
            return result;
        }
    }

    /**
     * {@link JmsPooledConnection} {@link bitronix.tm.resource.common.StateChangeListener}.
     * When state changes to STATE_CLOSED, the conenction is unregistered from
     * {@link bitronix.tm.utils.ManagementRegistrar}.
     */
    private class JmsPooledConnectionStateChangeListener implements StateChangeListener {
        public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
            if (newState == STATE_IN_POOL) {
                if (log.isDebugEnabled()) log.debug("requeued JMS connection of " + poolingConnectionFactory);
                lastReleaseDate = new Date();
            }
            if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
                acquisitionDate = new Date();
            }
            if (newState == STATE_CLOSED) {
                ManagementRegistrar.unregister(jmxName);
            }
        }

        public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
        }
    }

    /**
     * {@link JmsConnectionHandle} {@link bitronix.tm.resource.common.StateChangeListener}.
     * When state changes to STATE_CLOSED, the session is removed from the list of opened sessions.
     */
    private class JmsConnectionHandleStateChangeListener implements StateChangeListener {
        public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
            if (newState == XAResourceHolder.STATE_CLOSED) {
                synchronized (sessions) {
                    sessions.remove(source);
                    if (log.isDebugEnabled()) log.debug("DualSessionWrapper has been closed, " + sessions.size() + " session(s) left open in pooled connection");
                }
            }
        }

        public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
        }
    }
}
