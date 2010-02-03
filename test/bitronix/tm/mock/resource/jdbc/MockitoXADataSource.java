package bitronix.tm.mock.resource.jdbc;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

import javax.sql.*;
import javax.transaction.xa.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockXAResource;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class MockitoXADataSource implements XADataSource {

    private List xaConnections = new ArrayList();
    private String userName;
    private String password;
    private String database;
    private List inDoubtXids = new ArrayList();
    private SQLException getXAConnectionException;
    private static SQLException staticGetXAConnectionException;

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
        doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockXAConnection);
				eventRecorder.addEvent(new XAConnectionCloseEvent(mockXAConnection));
				return null;
			}
		}).doThrow(new SQLException("XAConnection is already closed")).when(mockXAConnection).close();

        when(mockXAConnection.getXAResource()).thenReturn(xaResource);
        Connection mockConnection = createMockConnection();
        when(mockXAConnection.getConnection()).thenReturn(mockConnection);

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

    public static Connection createMockConnection() throws SQLException {
        // Setup mock connection
        final Connection mockConnection = mock(Connection.class);
        // Handle Connection.isValid()
        when(mockConnection.isValid(anyInt())).thenReturn(true);
        // Handle Connection.createStatement()
        Statement statement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(statement);
        when(mockConnection.createStatement(anyInt(), anyInt())).thenReturn(statement);
        when(mockConnection.createStatement(anyInt(), anyInt(), anyInt())).thenReturn(statement);
        // Handle Connection.prepareStatement()
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), (int[]) anyObject())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), (String[]) anyObject())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mockPreparedStatement);
        // Handle Connection.prepareCall()
        CallableStatement mockCallableStatement = mock(CallableStatement.class);
        when(mockConnection.prepareCall(anyString())).thenReturn(mockCallableStatement);
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt())).thenReturn(mockCallableStatement);
        when(mockConnection.prepareCall(anyString(), anyInt(), anyInt(), anyInt())).thenReturn(mockCallableStatement);
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
}
