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

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XAConnection;
import javax.jms.XASession;

/**
 * XAConnection implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 */
public class LrcXAConnection implements XAConnection {

    private final Connection nonXaConnection;

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
