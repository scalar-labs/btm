package bitronix.tm.mock.resource.jdbc;

import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.ConnectionCloseEvent;
import bitronix.tm.mock.events.LocalCommitEvent;
import bitronix.tm.mock.events.LocalRollbackEvent;

import java.sql.*;
import java.util.Map;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockConnection implements Connection {

    private boolean closed = false;

    private EventRecorder getEventRecorder() {
        return EventRecorder.getEventRecorder(this);
    }

    public void close() throws SQLException {
        getEventRecorder().addEvent(new ConnectionCloseEvent(this));
        closed = true;
    }

    public boolean isClosed() throws SQLException {
        return closed;
    }

    public int getHoldability() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getTransactionIsolation() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearWarnings() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void commit() throws SQLException {
        getEventRecorder().addEvent(new LocalCommitEvent(this, new Exception()));
    }

    public void rollback() throws SQLException {
        getEventRecorder().addEvent(new LocalRollbackEvent(this, new Exception()));
    }

    public boolean getAutoCommit() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isReadOnly() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setHoldability(int holdability) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTransactionIsolation(int level) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getCatalog() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCatalog(String catalog) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Savepoint setSavepoint() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Statement createStatement() throws SQLException {
        return new MockStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MockStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MockStatement();
    }

    public Map getTypeMap() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTypeMap(Map map) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String nativeSQL(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new MockPreparedStatement();
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new MockPreparedStatement();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new MockPreparedStatement();
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new MockPreparedStatement();
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[]) throws SQLException {
        return new MockPreparedStatement();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[]) throws SQLException {
        return new MockPreparedStatement();
    }

    public String toString() {
        return "a MockXAConnection";
    }
}
