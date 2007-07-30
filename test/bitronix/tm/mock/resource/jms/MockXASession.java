package bitronix.tm.mock.resource.jms;

import bitronix.tm.mock.resource.MockXAResource;

import javax.jms.*;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockXASession implements XASession {

    private XAResource xaResource = new MockXAResource(null);

    public Session getSession() throws JMSException {
        return this;
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public BytesMessage createBytesMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MapMessage createMapMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Message createMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public StreamMessage createStreamMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TextMessage createTextMessage() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TextMessage createTextMessage(String jndiName) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getTransacted() throws JMSException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getAcknowledgeMode() throws JMSException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void commit() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rollback() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void recover() throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public MessageListener getMessageListener() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public MessageProducer createProducer(Destination destination) throws JMSException {
        return new MockMessageProducer();
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return new MockMessageConsumer();
    }

    public MessageConsumer createConsumer(Destination destination, String jndiName) throws JMSException {
        return new MockMessageConsumer();
    }

    public MessageConsumer createConsumer(Destination destination, String jndiName, boolean b) throws JMSException {
        return new MockMessageConsumer();
    }

    public Queue createQueue(String jndiName) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Topic createTopic(String jndiName) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String jndiName) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String jndiName, String jndiName1, boolean b) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public QueueBrowser createBrowser(Queue queue, String jndiName) throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void unsubscribe(String jndiName) throws JMSException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
