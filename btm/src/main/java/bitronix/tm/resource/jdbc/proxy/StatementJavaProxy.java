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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

/**
 * @author Brett Wooldridge
 */
public class StatementJavaProxy extends JavaProxyBase<Statement> {

    private final static Map<String, Method> selfMethodMap = createMethodMap(StatementJavaProxy.class);

    private JdbcPooledConnection jdbcPooledConnection;

    public StatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        initialize(jdbcPooledConnection, statement);
    }

    public StatementJavaProxy() {
        // Default constructor
    }

    void initialize(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = statement;
    }

    /* Overridden methods of java.sql.Statement */

    public void close() throws SQLException {
        if (delegate == null) {
            return;
        }

        jdbcPooledConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
    }

    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(delegate.getClass())) {
            return (T) delegate;
        }
        if (isWrapperFor(iface)) {
            return unwrap(delegate, iface);
        }
        throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
    }

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
