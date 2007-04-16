package bitronix.tm.mock.events;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class ConnectionQueuedEvent extends Event {

    private JdbcPooledConnection jdbcPooledConnection;

    public ConnectionQueuedEvent(Object source, JdbcPooledConnection jdbcPooledConnection) {
        super(source, null);
        this.jdbcPooledConnection = jdbcPooledConnection;
    }

    public ConnectionQueuedEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public JdbcPooledConnection getPooledConnectionImpl() {
        return jdbcPooledConnection;
    }

    public String toString() {
        return "ConnectionQueuedEvent at " + getTimestamp() + " on " + jdbcPooledConnection + (getException()!=null ? " and " + getException().toString() : "");
    }
}
