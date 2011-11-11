/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.resource.jms;

import bitronix.tm.internal.BitronixRuntimeException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * JMS destination wrapper optimized for use with hashed collections where it is the key and a
 * {@link javax.jms.MessageProducer} or a {@link javax.jms.MessageConsumer} is the value.
 *
 * @author lorban
 */
public class MessageProducerConsumerKey {

    private final Destination destination;
    private final String messageSelector;
    private final Boolean noLocal;

    public MessageProducerConsumerKey(Destination destination) {
        this.destination = destination;
        this.messageSelector = null;
        this.noLocal = null;
    }

    public MessageProducerConsumerKey(Destination destination, String messageSelector) {
        this.destination = destination;
        this.messageSelector = messageSelector;
        this.noLocal = null;
    }

    public MessageProducerConsumerKey(Destination destination, String messageSelector, boolean noLocal) {
        this.destination = destination;
        this.messageSelector = messageSelector;
        this.noLocal = noLocal;
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
