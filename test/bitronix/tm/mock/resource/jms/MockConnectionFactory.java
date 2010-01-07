package bitronix.tm.mock.resource.jms;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import javax.jms.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockConnectionFactory implements ConnectionFactory {

    public Connection createConnection() throws JMSException {
    	Answer<Session> sessionAnswer = new Answer<Session>() {
			public Session answer(InvocationOnMock invocation) throws Throwable {
				Session session = mock(Session.class);
				MessageProducer producer = mock(MessageProducer.class);
				when(session.createProducer((Destination) anyObject())).thenReturn(producer);
				return session;
			}
    	};

    	Connection connection = mock(Connection.class);
    	when(connection.createSession(anyBoolean(), anyInt())).thenAnswer(sessionAnswer);
    	return connection;
    }

    public Connection createConnection(String jndiName, String jndiName1) throws JMSException {
        return createConnection();
    }
}