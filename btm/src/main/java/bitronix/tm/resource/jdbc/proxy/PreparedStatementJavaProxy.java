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
import java.sql.PreparedStatement;
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

    PreparedStatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        initialize(jdbcPooledConnection, statement, cacheKey);
    }

    void initialize(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        this.jdbcPooledConnection = jdbcPooledConnection;
        this.delegate = statement;
        this.cacheKey = cacheKey;
        this.pretendClosed = false;
    }

    public String toString() {
        return "a JdbcPreparedStatementHandle wrapping [" + delegate + "]";
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

            initialize(null, null, null);
            JdbcProxyFactory.INSTANCE.returnProxyPreparedStatement(getProxy());
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

    /* Overridden methods of JavaProxyBase */

    @Override
    protected Map<String, Method> getMethodMap() {
        return selfMethodMap;
    }
}
