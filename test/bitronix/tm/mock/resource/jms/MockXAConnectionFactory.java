package bitronix.tm.mock.resource.jms;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.jms.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import bitronix.tm.mock.resource.MockXAResource;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
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
