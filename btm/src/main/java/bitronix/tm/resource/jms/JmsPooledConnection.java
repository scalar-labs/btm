/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.resource.jms;

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.resource.common.AbstractXAStatefulHolder;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.resource.jms.lrc.LrcXAConnectionFactory;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.XAConnection;
import javax.jms.XASession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of a JMS pooled connection wrapping vendor's {@link XAConnection} implementation.
 *
 * @author lorban
 * TODO: how can the JMS connection be accurately tested?
 */
public class JmsPooledConnection extends AbstractXAStatefulHolder implements JmsPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JmsPooledConnection.class);

    private final XAConnection xaConnection;
    private final PoolingConnectionFactory poolingConnectionFactory;
    private final List<DualSessionWrapper> sessions = Collections.synchronizedList(new ArrayList<DualSessionWrapper>());
    private boolean closed = false;

    /* management */
    private final String jmxName;
    private volatile Date acquisitionDate;
    private volatile Date lastReleaseDate;

    protected JmsPooledConnection(PoolingConnectionFactory poolingConnectionFactory, XAConnection connection) {
        this.poolingConnectionFactory = poolingConnectionFactory;
        this.xaConnection = connection;
        this.lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
        addStateChangeEventListener(new JmsPooledConnectionStateChangeListener());
        
        if (poolingConnectionFactory.getClassName().equals(LrcXAConnectionFactory.class.getName())) {
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing twoPcOrderingPosition to ALWAYS_LAST_POSITION");
            poolingConnectionFactory.setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing deferConnectionRelease to true");
            poolingConnectionFactory.setDeferConnectionRelease(true);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingConnectionFactory.getUniqueName() + " - changing useTmJoin to true");
            poolingConnectionFactory.setUseTmJoin(true);
        }
        
        this.jmxName = "bitronix.tm:type=JMS,UniqueName=" + ManagementRegistrar.makeValidName(poolingConnectionFactory.getUniqueName()) + ",Id=" + poolingConnectionFactory.incCreatedResourcesCounter();
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
        if (!closed) {
            setState(STATE_CLOSED);
            xaConnection.close();
        }
        closed = true;
    }

    public List<XAResourceHolder> getXAResourceHolders() {
        synchronized (sessions) {
            return new ArrayList<XAResourceHolder>(sessions);
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
            for (DualSessionWrapper dualSessionWrapper : sessions) {
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
            for (DualSessionWrapper sessionHandle : sessions) {
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
                    Decoder.decodeXAStatefulHolderState(getState()) + " with underlying connection " + xaConnection;
        }
    }

    /* management */

    public String getStateDescription() {
        return Decoder.decodeXAStatefulHolderState(getState());
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public Collection<String> getTransactionGtridsCurrentlyHoldingThis() {
        synchronized (sessions) {
            Set<String> result = new HashSet<String>();
            for (DualSessionWrapper dsw : sessions) {
                result.addAll(dsw.getXAResourceHolderStateGtrids());
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
                lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
            }
            if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
                acquisitionDate = new Date(MonotonicClock.currentTimeMillis());
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
