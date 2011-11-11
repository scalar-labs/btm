/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.resource.jdbc.lrc;

import bitronix.tm.resource.jdbc.BaseProxyHandlerClass;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Connection handle implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author lorban, brettw
 */
public class LrcConnectionHandle extends BaseProxyHandlerClass { // implements Connection

    private final Connection delegate;
    private final LrcXAResource xaResource;
    private volatile boolean closed = false;

    public LrcConnectionHandle(LrcXAResource xaResource, Connection delegate) {
        this.delegate = delegate;
        this.xaResource = xaResource;
    }

    public Connection getConnection() {
        return delegate;
    }

    private Connection getDelegate() throws SQLException {
        if (delegate == null)
            throw new SQLException("connection is closed");
        return delegate;
    }

    /* wrapped Connection methods that have special XA semantics */

    public void close() throws SQLException {
        closed = true;
    }

    public boolean isClosed() throws SQLException {
        return closed;
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
