package bitronix.tm.mock.resource.jms;

import javax.jms.MessageConsumer;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Message;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockMessageConsumer implements MessageConsumer {
    public String getMessageSelector() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MessageListener getMessageListener() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Message receive() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Message receive(long l) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Message receiveNoWait() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
