package bitronix.tm.mock.resource.jdbc;

import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.XAConnectionCloseEvent;
import bitronix.tm.mock.resource.jdbc.MockConnection;
import bitronix.tm.mock.resource.MockXAResource;

import javax.sql.XAConnection;
import javax.sql.ConnectionEventListener;
import javax.transaction.xa.XAResource;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class MockXAConnection implements XAConnection {

    private MockXAResource mockXAResource = new MockXAResource();
    private MockConnection mockConnection = new MockConnection();

    private EventRecorder getEventRecorder() {
       return EventRecorder.getEventRecorder(this);
   }

    public void close() throws SQLException {
        getEventRecorder().addEvent(new XAConnectionCloseEvent(this));
    }

    public Connection getConnection() throws SQLException {
        return mockConnection;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    public XAResource getXAResource() throws SQLException {
        return mockXAResource;
    }
}
