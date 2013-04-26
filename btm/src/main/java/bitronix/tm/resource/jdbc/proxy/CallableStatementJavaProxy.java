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

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class CallableStatementJavaProxy extends JavaProxyBase<CallableStatement> {

    private final static Map<String, Method> selfMethodMap = createMethodMap(CallableStatementJavaProxy.class);

    private JdbcPooledConnection jdbcPooledConnection;
    
    public CallableStatementJavaProxy() {
        // Default constructor
    }

    public CallableStatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        initialize(jdbcPooledConnection, statement);
    }

    void initialize(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
    	this.proxy = this;
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = statement;
    }

    /* Overridden methods of java.sql.CallableStatement */

    public void close() throws SQLException {
        if (delegate == null) {
            return;
        }

        jdbcPooledConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }

    public ResultSet executeQuery() throws SQLException {
    	return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.executeQuery());
    }

    public ResultSet executeQuery(String sql) throws SQLException {
    	return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.executeQuery(sql));
    }

    public ResultSet getGeneratedKeys() throws SQLException {
    	return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.getGeneratedKeys());
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

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
