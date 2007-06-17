package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.XAResourceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * Disposable Connection handle.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class JmsConnectionHandle implements Connection {

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
        DualSessionWrapper sessionHandle = pooledConnection.getNotAccessibleSession();

        if (sessionHandle == null) {
            if (log.isDebugEnabled()) log.debug("no session handle found in NOT_ACCESSIBLE state, creating new session");
            sessionHandle = pooledConnection.createDualSessionWrapper(pooledConnection, transacted, acknowledgeMode);
        }
        else {
            if (log.isDebugEnabled()) log.debug("found session handle in NOT_ACCESSIBLE state, recycling it: " + sessionHandle);
            sessionHandle.setState(XAResourceHolder.STATE_ACCESSIBLE);
        }

        return sessionHandle;
    }

    public void close() throws JMSException {
        if (xaConnection == null)
            return;

        xaConnection = null;
        pooledConnection.release();
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
