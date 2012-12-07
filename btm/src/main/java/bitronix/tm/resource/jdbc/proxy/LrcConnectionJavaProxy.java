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

    public LrcConnectionJavaProxy(LrcXAResource xaResource, Connection connection) {
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
