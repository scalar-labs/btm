package bitronix.tm.resource.jdbc;

import java.sql.*;

/**
 * CallableStatement {@link Statement} wrapper.
 * <p/>
 * This class is a proxy handler for a CallableStatement.  It does not
 * implement the CallableStatement interface or extend a class directly,
 * but you methods implemented here will override those of the
 * underlying delegate.  Simply implement a method with the same
 * signature, and the local method will be called rather than the delegate.
 * <p/>
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author brettw
 */
public class JdbcCallableStatementHandle extends BaseProxyHandlerClass { // implements CallableStatement

    // The 'parent' connection. Used to remove this statement delegate
    // from the un-closed statements list when close() is called.
    private JdbcPooledConnection parentConnection;

    private CallableStatement delegate;

    public JdbcCallableStatementHandle(CallableStatement delegate, JdbcPooledConnection pooledConnection) {
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
