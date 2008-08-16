package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.TransactionContextHelper;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * {@link MessageProducer} wrapper that adds XA enlistment semantics.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class MessageProducerWrapper implements MessageProducer {

    private MessageProducer messageProducer;
    protected DualSessionWrapper session;
    private PoolingConnectionFactory poolingConnectionFactory;

    public MessageProducerWrapper(MessageProducer messageProducer, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory) {
        this.messageProducer = messageProducer;
        this.session = session;
        this.poolingConnectionFactory = poolingConnectionFactory;
    }

    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

    /**
     * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws JMSException
     */
    protected void enlistResource() throws JMSException {
        if (poolingConnectionFactory.getAutomaticEnlistingEnabled()) {
            try {
                TransactionContextHelper.enlistInCurrentTransaction(session, poolingConnectionFactory);
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    public String toString() {
        return "a MessageProducerWrapper of " + session;
    }

    /* MessageProducer with special XA semantics implementation */

    public void send(Message message) throws JMSException {
        enlistResource();
        getMessageProducer().send(message);
    }

    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        enlistResource();
        getMessageProducer().send(message, deliveryMode, priority, timeToLive);
    }

    public void send(Destination destination, Message message) throws JMSException {
        enlistResource();
        getMessageProducer().send(destination, message);
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        enlistResource();
        getMessageProducer().send(destination, message, deliveryMode, priority, timeToLive);
    }

    public void close() throws JMSException {
        // do nothing as the close is handled by the session handle
    }

    /* dumb wrapping of MessageProducer methods */

    public void setDisableMessageID(boolean value) throws JMSException {
        getMessageProducer().setDisableMessageID(value);
    }

    public boolean getDisableMessageID() throws JMSException {
        return getMessageProducer().getDisableMessageID();
    }

    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        getMessageProducer().setDisableMessageTimestamp(value);
    }

    public boolean getDisableMessageTimestamp() throws JMSException {
        return getMessageProducer().getDisableMessageTimestamp();
    }

    public void setDeliveryMode(int deliveryMode) throws JMSException {
        getMessageProducer().setDeliveryMode(deliveryMode);
    }

    public int getDeliveryMode() throws JMSException {
        return getMessageProducer().getDeliveryMode();
    }

    public void setPriority(int defaultPriority) throws JMSException {
        getMessageProducer().setPriority(defaultPriority);
    }

    public int getPriority() throws JMSException {
        return getMessageProducer().getPriority();
    }

    public void setTimeToLive(long timeToLive) throws JMSException {
        getMessageProducer().setTimeToLive(timeToLive);
    }

    public long getTimeToLive() throws JMSException {
        return getMessageProducer().getTimeToLive();
    }

    public Destination getDestination() throws JMSException {
        return getMessageProducer().getDestination();
    }

}
