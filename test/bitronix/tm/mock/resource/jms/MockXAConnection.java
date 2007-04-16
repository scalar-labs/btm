package bitronix.tm.mock.resource.jms;

import javax.jms.*;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockXAConnection implements XAConnection {

    public XASession createXASession() throws JMSException {
        return new MockXASession();
    }

    public Session createSession(boolean b, int i) throws JMSException {
        return new MockXASession();
    }

    public String getClientID() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setClientID(String jndiName) throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void start() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void stop() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ConnectionConsumer createConnectionConsumer(Destination destination, String jndiName, ServerSessionPool serverSessionPool, int i) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String jndiName, String jndiName1, ServerSessionPool serverSessionPool, int i) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
