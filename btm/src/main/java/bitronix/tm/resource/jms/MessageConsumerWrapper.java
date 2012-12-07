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
package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.TransactionContextHelper;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * {@link MessageConsumer} wrapper that adds XA enlistment semantics.
 *
 * @author lorban
 */
public class MessageConsumerWrapper implements MessageConsumer {

    private final MessageConsumer messageConsumer;
    protected final DualSessionWrapper session;
    private final PoolingConnectionFactory poolingConnectionFactory;

    public MessageConsumerWrapper(MessageConsumer messageConsumer, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory) {
        this.messageConsumer = messageConsumer;
        this.session = session;
        this.poolingConnectionFactory = poolingConnectionFactory;
    }

    public MessageConsumer getMessageConsumer() {
        return messageConsumer;
    }

    /**
     * Enlist this session into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws javax.jms.JMSException
     */
    protected void enlistResource() throws JMSException {
        if (poolingConnectionFactory.getAutomaticEnlistingEnabled()) {
            session.getSession(); // make sure the session is created before enlisting it
            try {
                TransactionContextHelper.enlistInCurrentTransaction(session);
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    public String toString() {
        return "a MessageConsumerWrapper of " + session;
    }

    /* MessageProducer with special XA semantics implementation */

    public Message receive() throws JMSException {
        enlistResource();
        return getMessageConsumer().receive();
    }

    public Message receive(long timeout) throws JMSException {
        enlistResource();
        return getMessageConsumer().receive(timeout);
    }

    public Message receiveNoWait() throws JMSException {
        enlistResource();
        return getMessageConsumer().receiveNoWait();
    }

    public void close() throws JMSException {
        // do nothing as the close is handled by the session handle
    }

    /* dumb wrapping of MessageProducer methods */

    public String getMessageSelector() throws JMSException {
        return getMessageConsumer().getMessageSelector();
    }

    public MessageListener getMessageListener() throws JMSException {
        return getMessageConsumer().getMessageListener();
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        getMessageConsumer().setMessageListener(listener);
    }

}
