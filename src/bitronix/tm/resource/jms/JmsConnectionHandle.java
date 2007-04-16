package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.XAStatefulHolder;

import javax.jms.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disposable Connection handle.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class JmsConnectionHandle implements Connection, StateChangeListener {

    private final static Logger log = LoggerFactory.getLogger(JmsConnectionHandle.class);

    private XAConnection xaConnection;
    private JmsPooledConnection pooledConnection;

    public JmsConnectionHandle(JmsPooledConnection pooledConnection, XAConnection xaConnection) {
        this.pooledConnection = pooledConnection;
        this.xaConnection = xaConnection;
    }

    private XAConnection getXAConnection() throws JMSException {
        if (xaConnection == null)
            throw new JMSException("XA connection handle has been closed");
        return xaConnection;
    }

    public JmsPooledConnection getPooledConnection() {
        return pooledConnection;
    }

    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        DualSessionWrapper sessionHandle = getNotAccessibleSession();

        if (sessionHandle == null) {
            if (log.isDebugEnabled()) log.debug("no session handle found in NOT_ACCESSIBLE state, creating new session");
            sessionHandle = new DualSessionWrapper(pooledConnection, transacted, acknowledgeMode);
            sessionHandle.addStateChangeEventListener(this);
            pooledConnection.sessions.add(sessionHandle);
        }
        else {
            if (log.isDebugEnabled()) log.debug("found session handle in NOT_ACCESSIBLE state, recycling it: " + sessionHandle);
            sessionHandle.setState(XAResourceHolder.STATE_ACCESSIBLE);
        }

        return sessionHandle;
    }

    private DualSessionWrapper getNotAccessibleSession() {
        synchronized (pooledConnection.sessions) {
            if (log.isDebugEnabled()) log.debug(pooledConnection.sessions.size() + " session(s) open from " + pooledConnection);
            for (int i = 0; i < pooledConnection.sessions.size(); i++) {
                DualSessionWrapper sessionHandle = (DualSessionWrapper) pooledConnection.sessions.get(i);
                if (sessionHandle.getState() == XAResourceHolder.STATE_NOT_ACCESSIBLE)
                    return sessionHandle;
            }
            return null;
        }
    }

    public void close() throws JMSException {
        if (xaConnection == null)
            return;

        xaConnection = null;
        pooledConnection.release();
    }

    // DualSessionWrapper state change listener
    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == XAResourceHolder.STATE_CLOSED) {
            pooledConnection.sessions.remove(source);
            if (log.isDebugEnabled()) log.debug("DualSessionWrapper has been closed, " + pooledConnection.sessions.size() + " session(s) left open in pooled connection");
        }
    }

    public String toString() {
        return "a JmsConnectionHandle of " + pooledConnection;
    }


    /* Connection implementation */

    public String getClientID() throws JMSException {
        return getXAConnection().getClientID();
    }

    public void setClientID(String jndiName) throws JMSException {
        getXAConnection().setClientID(jndiName);
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        return getXAConnection().getMetaData();
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        return getXAConnection().getExceptionListener();
    }

    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        getXAConnection().setExceptionListener(listener);
    }

    public void start() throws JMSException {
        getXAConnection().start();
    }

    public void stop() throws JMSException {
        getXAConnection().stop();
    }

    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return getXAConnection().createConnectionConsumer(destination, messageSelector, sessionPool, maxMessages);
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
        return getXAConnection().createDurableConnectionConsumer(topic, subscriptionName, messageSelector, sessionPool, maxMessages);
    }

}
