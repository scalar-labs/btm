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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;

/**
 * @author Brett Wooldridge
 */
public class PreparedStatementJavaProxy extends JavaProxyBase<PreparedStatement> {

    private final static Map<String, Method> selfMethodMap = createMethodMap(PreparedStatementJavaProxy.class);

    private JdbcPooledConnection jdbcPooledConnection;
    private CacheKey cacheKey;
    private boolean pretendClosed;

    public PreparedStatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        initialize(jdbcPooledConnection, statement, cacheKey);
    }

    public PreparedStatementJavaProxy() {
        // Default constructor
    }

    void initialize(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
    	this.proxy = (PreparedStatement) this;
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = statement;
        this.cacheKey = cacheKey;
        this.pretendClosed = false;
    }

    public String toString() {
        return "a PreparedStatementJavaProxy wrapping [" + delegate + "]";
    }

    /* Overridden methods of java.sql.PreparedStatement */

    public void close() throws SQLException {
        if (pretendClosed || delegate == null) {
            return;
        }

        pretendClosed = true;

        if (cacheKey == null) {
            jdbcPooledConnection.unregisterUncachedStatement(delegate);
            delegate.close();
        }
        else {
	        // Clear the parameters so the next use of this cached statement
	        // doesn't pick up unexpected values.
	        delegate.clearParameters();
	
	        // Return to cache so the usage count can be updated
	        jdbcPooledConnection.putCachedStatement(cacheKey, delegate);
        }
    }

    public boolean isClosed() throws SQLException {
        return pretendClosed;
    }

    public ResultSet getResultSet() throws SQLException {
    	return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.getResultSet());
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

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
