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

    /* java.sql.Wrapper implementation */

	public boolean isWrapperFor(Class iface) throws SQLException {
	    if (PreparedStatement.class.equals(iface)) {
	        return true;
	    }
		return false;
	}

	public Object unwrap(Class iface) throws SQLException {
        if (PreparedStatement.class.equals(iface)) {
            return delegate;
	    }
	    throw new SQLException(getClass().getName() + " is not a wrapper for interface " + iface.getName());
	}

    /* Overridden methods of java.sql.PreparedStatement */

    public void close() throws SQLException {
        parentConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }
}
