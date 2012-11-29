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

package bitronix.tm.resource.jdbc.proxy;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Map;

import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

/**
 * @author Brett Wooldridge
 */
public class LrcConnectionJavaProxy extends JavaProxyBase<Connection> {

    private static Map<String, Method> selfMethodMap = createMethodMap(LrcConnectionJavaProxy.class);

    private LrcXAResource xaResource;

    LrcConnectionJavaProxy(LrcXAResource xaResource, Connection connection) {
        this.delegate = connection;
        this.xaResource = xaResource;
    }

    public String toString() {
        return "a JDBC LrcConnectionJavaProxy on " + delegate;
    }

    /* wrapped Connection methods that have special XA semantics */

    public void close() throws SQLException {
        if (delegate != null) {
            delegate.close();
        }

        delegate = null;
    }

    public boolean isClosed() throws SQLException {
        return delegate == null;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX && autoCommit)
            throw new SQLException("XA transaction started, cannot enable autocommit mode");
        delegate.setAutoCommit(autoCommit);
    }

    public void commit() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call commit directly on connection");
        delegate.commit();
    }

    public void rollback() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        delegate.rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        delegate.rollback(savepoint);
    }

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
