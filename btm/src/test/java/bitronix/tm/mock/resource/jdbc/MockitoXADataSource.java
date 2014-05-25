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
package bitronix.tm.mock.resource.jdbc;

import bitronix.tm.mock.events.ConnectionCloseEvent;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.LocalCommitEvent;
import bitronix.tm.mock.events.LocalRollbackEvent;
import bitronix.tm.mock.events.XAConnectionCloseEvent;
import bitronix.tm.mock.resource.MockXAResource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Ludovic Orban
 */
public class MockitoXADataSource implements XADataSource {

    private List xaConnections = new ArrayList();
    private String userName;
    private String password;
    private String database;
    private Object uselessThing;
    private List inDoubtXids = new ArrayList();
    private SQLException getXAConnectionException;
    private static SQLException staticGetXAConnectionException;
    private static SQLException staticCloseXAConnectionException;

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public XAConnection getXAConnection() throws SQLException {
        if (staticGetXAConnectionException != null)
            throw staticGetXAConnectionException;
        if (getXAConnectionException != null)
            throw getXAConnectionException;

        // Create an XAResource
        XAResource xaResource = new MockXAResource(this);


        // Setup mock XAConnection
        final XAConnection mockXAConnection = mock(XAConnection.class);
        // Handle XAConnection.close(), first time we answer, after that we throw
        doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockXAConnection);
				eventRecorder.addEvent(new XAConnectionCloseEvent(mockXAConnection));
				return null;
			}
		}).doThrow(new SQLException("XAConnection is already closed")).when(mockXAConnection).close();

        when(mockXAConnection.getXAResource()).thenReturn(xaResource);
//        Connection mockConnection = createMockConnection();
//        when(mockXAConnection.getConnection()).thenReturn(mockConnection);
        doAnswer(new Answer<Connection>() {
			public Connection answer(InvocationOnMock invocation) throws Throwable {
				return createMockConnection();
			}
		}).when(mockXAConnection).getConnection();

        if (staticCloseXAConnectionException != null)
            doThrow(staticCloseXAConnectionException).when(mockXAConnection).close();

        xaConnections.add(mockXAConnection);
        return mockXAConnection;
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return getXAConnection();
    }

    public void setXaConnections(List xaConnections) {
        this.xaConnections = xaConnections;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void addInDoubtXid(Xid xid) {
        inDoubtXids.add(xid);
    }

    public boolean removeInDoubtXid(Xid xid) {
        for (int i = 0; i < inDoubtXids.size(); i++) {
            Xid xid1 = (Xid) inDoubtXids.get(i);
            if (Arrays.equals(xid1.getGlobalTransactionId(), xid.getGlobalTransactionId()) && Arrays.equals(xid1.getBranchQualifier(), xid.getBranchQualifier()) ) {
                inDoubtXids.remove(xid1);
                return true;
            }
        }
        return false;
    }

    public Xid[] getInDoubtXids() {
        return (Xid[]) inDoubtXids.toArray(new Xid[inDoubtXids.size()]);
    }

    public void setGetXAConnectionException(SQLException ex) {
        this.getXAConnectionException = ex;
    }

    public static void setStaticGetXAConnectionException(SQLException ex) {
        staticGetXAConnectionException = ex;
    }

    public static void setStaticCloseXAConnectionException(SQLException ex) {
        staticCloseXAConnectionException = ex;
    }

    public static Connection createMockConnection() throws SQLException {
        // Setup mock connection
        final Connection mockConnection = mock(Connection.class);

        // Autocommit is always true by default
        when(mockConnection.getAutoCommit()).thenReturn(true);

        // Handle Connection.createStatement()
        when(mockConnection.createStatement()).thenAnswer(mockStatement());
        when(mockConnection.createStatement(anyInt(), anyInt())).thenAnswer(mockStatement());
        when(mockConnection.createStatement(anyInt(), anyInt(), anyInt())).thenAnswer(mockStatement());

        // Handle Connection.prepareStatement()
        when(mockConnection.prepareStatement(anyString())).thenAnswer(mockPreparedStatement());
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenAnswer(mockPreparedStatement());
        when(mockConnection.prepareStatement(anyString(), (int[]) anyObject())).thenAnswer(mockPreparedStatement());
        when(mockConnection.prepareStatement(anyString(), (String[]) anyObject())).thenAnswer(mockPreparedStatement());
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenAnswer(mockPreparedStatement());
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenAnswer(mockPreparedStatement());

        // Handle Connection.prepareCall()
        when(mockConnection.prepareCall(anyString())).thenAnswer(mockCallableStatement());
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt())).thenAnswer(mockCallableStatement());
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt(), anyInt())).thenAnswer(mockCallableStatement());

        // Handle Connection.close()
        doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
				eventRecorder.addEvent(new ConnectionCloseEvent(mockConnection));
				return null;
			}
        }).doThrow(new SQLException("Connection is already closed")).when(mockConnection).close();

        // Handle Connection.commit()
        doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
				eventRecorder.addEvent(new LocalCommitEvent(mockConnection, new Exception()));
				return null;
			}
        }).doThrow(new SQLException("Transaction already commited")).when(mockConnection).commit();

        // Handle Connection.rollback()
        doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
				eventRecorder.addEvent(new LocalRollbackEvent(mockConnection, new Exception()));
				return null;
			}
        }).doThrow(new SQLException("Transaction already rolledback")).when(mockConnection).rollback();

        return mockConnection;
    }

    public Object getUselessThing() {
        return uselessThing;
    }

    public void setUselessThing(Object uselessThing) {
        this.uselessThing = uselessThing;
    }

    private static Answer<Statement> mockStatement() {
        return new Answer<Statement>() {
            @Override
            public Statement answer(InvocationOnMock invocation) throws Throwable {
                return mock(Statement.class);
            }
        };
    }

    private static Answer<PreparedStatement> mockPreparedStatement() {
        return new Answer<PreparedStatement>() {
            @Override
            public PreparedStatement answer(InvocationOnMock invocation) throws Throwable {
                return mock(PreparedStatement.class);
            }
        };
    }

    private static Answer<CallableStatement> mockCallableStatement() {
        return new Answer<CallableStatement>() {
            @Override
            public CallableStatement answer(InvocationOnMock invocation) throws Throwable {
                return mock(CallableStatement.class);
            }
        };
    }
}
