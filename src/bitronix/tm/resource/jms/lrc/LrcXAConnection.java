package bitronix.tm.resource.jms.lrc;

import javax.jms.*;

/**
 * XAConnection implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class LrcXAConnection implements XAConnection {

    private Connection nonXaConnection;

    public LrcXAConnection(Connection connection) {
        this.nonXaConnection = connection;
    }

    public XASession createXASession() throws JMSException {
        return new LrcXASession(nonXaConnection.createSession(true, Session.AUTO_ACKNOWLEDGE));
    }

    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        throw new JMSException(LrcXAConnection.class.getName() + " can only respond to createXASession()");
    }

    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool serverSessionPool, int maxMessages) throws JMSException {
        return nonXaConnection.createConnectionConsumer(destination, messageSelector, serverSessionPool, maxMessages);
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool serverSessionPool, int maxMessages) throws JMSException {
        return nonXaConnection.createDurableConnectionConsumer(topic, subscriptionName, messageSelector, serverSessionPool, maxMessages);
    }

    public String getClientID() throws JMSException {
        return nonXaConnection.getClientID();
    }

    public void setClientID(String clientID) throws JMSException {
        nonXaConnection.setClientID(clientID);
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        return nonXaConnection.getMetaData();
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        return nonXaConnection.getExceptionListener();
    }

    public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
        nonXaConnection.setExceptionListener(exceptionListener);
    }

    public void start() throws JMSException {
        nonXaConnection.start();
    }

    public void stop() throws JMSException {
        nonXaConnection.stop();
    }

    public void close() throws JMSException {
        nonXaConnection.close();
    }

    public String toString() {
        return "a JMS LrcXAConnection on " + nonXaConnection;
    }
}
