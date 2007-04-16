package bitronix.tm.mock.events;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class ConnectionDequeuedEvent extends Event {

    private JdbcPooledConnection jdbcPooledConnection;

    public ConnectionDequeuedEvent(Object source, JdbcPooledConnection jdbcPooledConnection) {
        super(source, null);
        this.jdbcPooledConnection = jdbcPooledConnection;
    }

    public ConnectionDequeuedEvent(Object source, Exception ex) {
        super(source, ex);
    }

    public JdbcPooledConnection getPooledConnectionImpl() {
        return jdbcPooledConnection;
    }

    public String toString() {
        return "ConnectionDequeuedEvent at " + getTimestamp() + " on " + jdbcPooledConnection + (getException()!=null ? " and " + getException().toString() : "");
    }
}
