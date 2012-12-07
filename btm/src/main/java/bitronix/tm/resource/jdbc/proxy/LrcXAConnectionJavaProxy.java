/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2012, Bitronix Software.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

/**
 * @author Brett Wooldridge
 */
public class LrcXAConnectionJavaProxy extends JavaProxyBase<Connection> {

    private final static Logger log = LoggerFactory.getLogger(LrcXAConnectionJavaProxy.class);

    private static Map<String, Method> selfMethodMap = createMethodMap(LrcXAConnectionJavaProxy.class);

    private LrcXAResource xaResource;
    private final List<ConnectionEventListener> connectionEventListeners = new CopyOnWriteArrayList<ConnectionEventListener>();

    public LrcXAConnectionJavaProxy(Connection connection) {
        this.xaResource = new LrcXAResource(connection);
        this.delegate = new JdbcJavaProxyFactory().getProxyConnection(xaResource, connection);
    }

    public String toString() {
        return "a JDBC LrcXAConnection on " + delegate;
    }

    public XAResource getXAResource() throws SQLException {
        return xaResource;
    }

    public void close() throws SQLException {
        delegate.close();
        fireCloseEvent();
    }

    public Connection getConnection() throws SQLException {
        return delegate;
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
    }

    private void fireCloseEvent() {
        if (log.isDebugEnabled()) { log.debug("notifying " + connectionEventListeners.size() + " connectionEventListeners(s) about closing of " + this); }
        for (int i = 0; i < connectionEventListeners.size(); i++) {
            ConnectionEventListener connectionEventListener = connectionEventListeners.get(i);
            connectionEventListener.connectionClosed(new ConnectionEvent((PooledConnection) delegate));
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LrcXAConnectionJavaProxy))
            return false;

        LrcXAConnectionJavaProxy other = (LrcXAConnectionJavaProxy) obj;
        return this.delegate.equals(other.delegate);
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
