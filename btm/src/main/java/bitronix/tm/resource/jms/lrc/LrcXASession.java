/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.resource.jms.lrc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * XASession implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 */
public class LrcXASession implements XASession {

    private final static Logger log = LoggerFactory.getLogger(LrcXASession.class);

    private final Session nonXaSession;
    private final XAResource xaResource;

    public LrcXASession(Session session) {
        this.nonXaSession = session;
        this.xaResource = new LrcXAResource(session);
        if (log.isDebugEnabled()) { log.debug("creating new LrcXASession with " + xaResource); }
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
