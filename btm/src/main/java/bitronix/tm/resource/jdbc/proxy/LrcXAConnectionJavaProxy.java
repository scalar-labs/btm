/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.resource.jdbc.proxy;

import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Brett Wooldridge
 */
public class LrcXAConnectionJavaProxy extends JavaProxyBase<Connection> {

    private final static Logger log = LoggerFactory.getLogger(LrcXAConnectionJavaProxy.class);

    private final static Map<String, Method> selfMethodMap = createMethodMap(LrcXAConnectionJavaProxy.class);

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
