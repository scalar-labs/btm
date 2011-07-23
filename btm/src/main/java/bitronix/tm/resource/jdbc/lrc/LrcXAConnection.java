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

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import javax.sql.*;
import javax.transaction.xa.XAResource;

import bitronix.tm.utils.ClassLoaderUtils;
import org.slf4j.*;

import bitronix.tm.resource.jdbc.BaseProxyHandlerClass;

/**
 * XAConnection implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author lorban, brettw
 */
public class LrcXAConnection extends BaseProxyHandlerClass { // implements XAConnection

    private final static Logger log = LoggerFactory.getLogger(LrcXAConnection.class);

    private final Connection connection;
    private final LrcXAResource xaResource;
    private final List connectionEventListeners = new ArrayList();

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
    	LrcConnectionHandle lrcConnectionHandle = new LrcConnectionHandle(xaResource, connection);
        return (Connection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { Connection.class }, lrcConnectionHandle);
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
            XAConnection conn = (XAConnection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { XAConnection.class }, this);
            connectionEventListener.connectionClosed(new ConnectionEvent(conn));
        }
    }

    public boolean equals(Object obj) {
    	if (!(obj instanceof LrcXAConnection))
    		return false;

    	LrcXAConnection other = (LrcXAConnection) obj;
    	return this.connection.equals(other.connection);
    }

    public int hashCode() {
    	return this.connection.hashCode();
    }

    public String toString() {
        return "a JDBC LrcXAConnection on " + connection;
    }

	public Object getProxiedDelegate() throws Exception {
		return connection;
	}
}
