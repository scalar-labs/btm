package bitronix.tm.resource.jdbc;

import java.sql.*;

public class JdbcUncachedPreparedStatementHandle extends BaseProxyHandlerClass {

    // The 'parent' connection. Used to remove this statement delegate
    // from the un-closed statements list when close() is called.
    private JdbcPooledConnection parentConnection;

    private PreparedStatement delegate;

    public JdbcUncachedPreparedStatementHandle(PreparedStatement delegate, JdbcPooledConnection pooledConnection) {
        this.delegate = delegate;
        this.parentConnection = pooledConnection;
    }

    public Object getProxiedDelegate() throws Exception {
        return delegate;
    }

    /**
     * Overridden methods of PreparedStatement.
     */

    public void close() throws SQLException {
        parentConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }
}
