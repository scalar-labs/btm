package bitronix.tm.resource.jdbc.lrc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionEvent;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * XAConnection implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class LrcXAConnection implements XAConnection {

    private final static Logger log = LoggerFactory.getLogger(LrcXAConnection.class);

    private Connection connection;
    private LrcXAResource xaResource;
    private List connectionEventListeners = new ArrayList();

    public LrcXAConnection(Connection connection) {
        this.connection = connection;
        this.xaResource = new LrcXAResource(connection);
    }

    public XAResource getXAResource() throws SQLException {
        return xaResource;
    }

    public void close() throws SQLException {
        connection.close();
        fireCloseEvent();
    }

    public Connection getConnection() throws SQLException {
        return new LrcConnectionHandle(xaResource, connection);
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
    }

    private void fireCloseEvent() {
        if (log.isDebugEnabled()) log.debug("notifying " + connectionEventListeners.size() + " connectionEventListeners(s) about closing of " + this);
        for (int i = 0; i < connectionEventListeners.size(); i++) {
            ConnectionEventListener connectionEventListener = (ConnectionEventListener) connectionEventListeners.get(i);
            connectionEventListener.connectionClosed(new ConnectionEvent(this));
        }
    }

    public String toString() {
        return "a JDBC LrcXAConnection on " + connection;
    }
}
