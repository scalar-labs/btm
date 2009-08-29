package bitronix.tm.resource.jms.lrc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * XASession implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class LrcXASession implements XASession {

    private final static Logger log = LoggerFactory.getLogger(LrcXASession.class);

    private final Session nonXaSession;
    private final XAResource xaResource;

    public LrcXASession(Session session) {
        this.nonXaSession = session;
        this.xaResource = new LrcXAResource(session);
        if (log.isDebugEnabled()) log.debug("creating new LrcXASession with " + xaResource);
    }


    public Session getSession() throws JMSException {
        return nonXaSession;
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public BytesMessage createBytesMessage() throws JMSException {
        return nonXaSession.createBytesMessage();
    }

    public MapMessage createMapMessage() throws JMSException {
        return nonXaSession.createMapMessage();
    }

    public Message createMessage() throws JMSException {
        return nonXaSession.createMessage();
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        return nonXaSession.createObjectMessage();
    }

    public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
        return nonXaSession.createObjectMessage(serializable);
    }

    public StreamMessage createStreamMessage() throws JMSException {
        return nonXaSession.createStreamMessage();
    }

    public TextMessage createTextMessage() throws JMSException {
        return nonXaSession.createTextMessage();
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        return nonXaSession.createTextMessage(text);
    }

    public boolean getTransacted() throws JMSException {
        return nonXaSession.getTransacted();
    }

    public int getAcknowledgeMode() throws JMSException {
        return nonXaSession.getAcknowledgeMode();
    }

    public void commit() throws JMSException {
        nonXaSession.commit();
    }

    public void rollback() throws JMSException {
        nonXaSession.rollback();
    }

    public void close() throws JMSException {
        nonXaSession.close();
    }

    public void recover() throws JMSException {
        nonXaSession.recover();
    }

    public MessageListener getMessageListener() throws JMSException {
        return nonXaSession.getMessageListener();
    }

    public void setMessageListener(MessageListener messageListener) throws JMSException {
        nonXaSession.setMessageListener(messageListener);
    }

    public void run() {
        nonXaSession.run();
    }

    public MessageProducer createProducer(Destination destination) throws JMSException {
        return nonXaSession.createProducer(destination);
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return nonXaSession.createConsumer(destination);
    }

    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        return nonXaSession.createConsumer(destination, messageSelector);
    }

    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        return nonXaSession.createConsumer(destination, messageSelector, noLocal);
    }

    public Queue createQueue(String queueName) throws JMSException {
        return nonXaSession.createQueue(queueName);
    }

    public Topic createTopic(String topicName) throws JMSException {
        return nonXaSession.createTopic(topicName);
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        return nonXaSession.createDurableSubscriber(topic, name);
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        return nonXaSession.createDurableSubscriber(topic, name, messageSelector, noLocal);
    }

    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return nonXaSession.createBrowser(queue);
    }

    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        return nonXaSession.createBrowser(queue, messageSelector);
    }

    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return nonXaSession.createTemporaryQueue();
    }

    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return nonXaSession.createTemporaryTopic();
    }

    public void unsubscribe(String name) throws JMSException {
        nonXaSession.unsubscribe(name);
    }

    public String toString() {
        return "a JMS LrcXASession on " + nonXaSession;
    }
}
