package bitronix.tm.resource.jms;

import javax.jms.*;

/**
 * Disposable Connection handle.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class JmsConnectionHandle implements Connection {

    private XAConnection xaConnection;
    private JmsPooledConnection pooledConnection;

    public JmsConnectionHandle(JmsPooledConnection pooledConnection, XAConnection xaConnection) {
        this.pooledConnection = pooledConnection;
        this.xaConnection = xaConnection;
    }

    public XAConnection getXAConnection() throws JMSException {
        if (xaConnection == null)
            throw new JMSException("XA connection handle has been closed");
        return xaConnection;
    }

    public JmsPooledConnection getPooledConnection() {
        return pooledConnection;
    }

    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return pooledConnection.createSession(transacted, acknowledgeMode);
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
