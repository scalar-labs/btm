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
package bitronix.tm.mock.resource.jms;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.jms.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import bitronix.tm.mock.resource.MockXAResource;

/**
 *
 * @author lorban
 */
public class MockXAConnectionFactory implements XAConnectionFactory {

    public XAConnection createXAConnection() throws JMSException {

    	Answer xaSessionAnswer = new Answer<XASession>() {
    		public XASession answer(InvocationOnMock invocation)throws Throwable {
    			XASession mockXASession = mock(XASession.class);
    			MessageProducer messageProducer = mock(MessageProducer.class);
    	    	when(mockXASession.createProducer((Destination) anyObject())).thenReturn(messageProducer);
    	    	MessageConsumer messageConsumer = mock(MessageConsumer.class);
    	    	when(mockXASession.createConsumer((Destination) anyObject())).thenReturn(messageConsumer);
    	    	when(mockXASession.createConsumer((Destination) anyObject(), anyString())).thenReturn(messageConsumer);
    	    	when(mockXASession.createConsumer((Destination) anyObject(), anyString(), anyBoolean())).thenReturn(messageConsumer);
    	    	Queue queue = mock(Queue.class);
    	    	when(mockXASession.createQueue(anyString())).thenReturn(queue);
    	    	Topic topic = mock(Topic.class);
    	    	when(mockXASession.createTopic(anyString())).thenReturn(topic);
    	    	MockXAResource mockXAResource = new MockXAResource(null);
    			when(mockXASession.getXAResource()).thenReturn(mockXAResource);    			
    	    	Answer<Session> sessionAnswer = new Answer<Session>() {
    				public Session answer(InvocationOnMock invocation) throws Throwable {
    					Session session = mock(Session.class);
    					MessageProducer producer = mock(MessageProducer.class);
    					when(session.createProducer((Destination) anyObject())).thenReturn(producer);
    					return session;
    				}
    	    	};
    			when(mockXASession.getSession()).thenAnswer(sessionAnswer);
    			
    			return mockXASession;
    		}
    	};

    	XAConnection mockXAConnection = mock(XAConnection.class);
    	when(mockXAConnection.createXASession()).thenAnswer(xaSessionAnswer);
    	when(mockXAConnection.createSession(anyBoolean(), anyInt())).thenAnswer(xaSessionAnswer);
        return mockXAConnection;
    }

    public XAConnection createXAConnection(String jndiName, String jndiName1) throws JMSException {
        return createXAConnection();
    }
}
