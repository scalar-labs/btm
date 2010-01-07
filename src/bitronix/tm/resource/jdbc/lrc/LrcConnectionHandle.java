package bitronix.tm.resource.jdbc.lrc;

import java.sql.*;

import bitronix.tm.resource.jdbc.BaseProxyHandlerClass;

/**
 * Connection handle implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban, brettw
 */
public class LrcConnectionHandle extends BaseProxyHandlerClass { // implements Connection

    private Connection delegate;
    private LrcXAResource xaResource;

    public LrcConnectionHandle(LrcXAResource xaResource, Connection delegate) {
        this.delegate = delegate;
        this.xaResource = xaResource;
    }

    private Connection getDelegate() throws SQLException {
        if (delegate == null)
            throw new SQLException("connection is closed");
        return delegate;
    }

    /* wrapped Connection methods that have special XA semantics */

    public void close() throws SQLException {
        delegate = null;
    }

    public boolean isClosed() throws SQLException {
        return delegate == null;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX && autoCommit)
            throw new SQLException("XA transaction started, cannot enable autocommit mode");
        getDelegate().setAutoCommit(autoCommit);
    }

    public void commit() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call commit directly on connection");
        getDelegate().commit();
    }

    public void rollback() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        getDelegate().rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        getDelegate().rollback(savepoint);
    }

    public String toString() {
        return "a JDBC LrcConnectionHandle on " + xaResource;
    }

	public Object getProxiedDelegate() throws Exception {
		return getDelegate();
	}
}
