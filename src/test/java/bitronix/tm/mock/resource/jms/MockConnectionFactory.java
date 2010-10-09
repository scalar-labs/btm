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

/**
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