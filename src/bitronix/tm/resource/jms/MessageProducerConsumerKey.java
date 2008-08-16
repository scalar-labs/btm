package bitronix.tm.resource.jms;

import bitronix.tm.internal.BitronixRuntimeException;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.jms.JMSException;

/**
 * JMS destination wrapper optimized for use with hashed collections where it is the key and a
 * {@link javax.jms.MessageProducer} or a {@link javax.jms.MessageConsumer} is the value.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class MessageProducerConsumerKey {

    private Destination destination;
    private String messageSelector;
    private Boolean noLocal;

    public MessageProducerConsumerKey(Destination destination) {
        this.destination = destination;
    }

    public MessageProducerConsumerKey(Destination destination, String messageSelector) {
        this.destination = destination;
        this.messageSelector = messageSelector;
    }

    public MessageProducerConsumerKey(Destination destination, String messageSelector, boolean noLocal) {
        this.destination = destination;
        this.messageSelector = messageSelector;
        this.noLocal = Boolean.valueOf(noLocal);
    }

    public boolean equals(Object obj) {
        if (obj instanceof MessageProducerConsumerKey) {
            MessageProducerConsumerKey otherKey = (MessageProducerConsumerKey) obj;

            if (!areEquals(getDestinationName(), otherKey.getDestinationName()))
                return false;
            if (!areEquals(messageSelector, otherKey.messageSelector))
                return false;
            if (!areEquals(noLocal, otherKey.noLocal))
                return false;

            return true;
        }
        return false;
    }

    private static boolean areEquals(Object o1, Object o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null && o2 == null)
            return false;
        if (o1 == null)
            return false;
        return o1.equals(o2);
    }

    private String getDestinationName() {
        if (destination == null) {
            return null;
        }
        else if (destination instanceof Queue) {
            try {
                return ((Queue) destination).getQueueName();
            } catch (JMSException ex) {
                throw new BitronixRuntimeException("error getting queue name of " + destination, ex);
            }
        }
        else if (destination instanceof Topic) {
            try {
                return ((Topic) destination).getTopicName();
            } catch (JMSException ex) {
                throw new BitronixRuntimeException("error getting topic name of " + destination, ex);
            }
        }
        else throw new IllegalArgumentException("unsupported destination: " + destination);
    }

    public int hashCode() {
        return hash(getDestinationName()) + hash(messageSelector) + hash(noLocal);
    }

    private static int hash(Object o) {
        if (o == null)
            return 0;
        return o.hashCode();
    }

    public String toString() {
        return "a MessageProducerConsumerKey on " + destination +
                (messageSelector == null ? "" : (" with selector '" + messageSelector) + "'") +
                (noLocal == null ? "" : (" and noLocal=" + noLocal));
    }
}
