package bitronix.tm.mock.resource.jdbc;

import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.XAConnectionCloseEvent;
import bitronix.tm.mock.resource.MockXAResource;

import javax.sql.ConnectionEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class MockXAConnection implements XAConnection {

    private MockXAResource mockXAResource;


    public MockXAConnection(MockXADataSource xads) {
        mockXAResource = new MockXAResource(xads);
    }

    private EventRecorder getEventRecorder() {
       return EventRecorder.getEventRecorder(this);
    }

    public void close() throws SQLException {
        getEventRecorder().addEvent(new XAConnectionCloseEvent(this));
    }

    public Connection getConnection() throws SQLException {
        return new MockConnection();
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    public XAResource getXAResource() throws SQLException {
        return mockXAResource;
    }
}
