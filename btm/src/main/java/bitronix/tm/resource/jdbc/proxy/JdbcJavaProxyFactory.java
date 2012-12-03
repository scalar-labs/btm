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

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.XAConnection;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 * @author Brett Wooldridge
 */
public class JdbcJavaProxyFactory implements JdbcProxyFactory {

    private static Map<Class<?>, Set<Class<?>>> interfaceCache;

    static {
        interfaceCache = new ConcurrentHashMap<Class<?>, Set<Class<?>>>();
    }

    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        ConnectionJavaProxy jdbcConnectionProxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);

        return (Connection) createNewProxy(connection, jdbcConnectionProxy, PooledConnectionProxy.class);
    }

    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        StatementJavaProxy jdbcStatementProxy = new StatementJavaProxy(jdbcPooledConnection, statement);

        return (Statement) createNewProxy(statement, jdbcStatementProxy);
    }

    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        CallableStatementJavaProxy jdbcStatementProxy = new CallableStatementJavaProxy(jdbcPooledConnection, statement);

        return (CallableStatement) createNewProxy(statement, jdbcStatementProxy);
    }

    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        PreparedStatementJavaProxy jdbcStatementProxy = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);

        return (PreparedStatement) createNewProxy(statement, jdbcStatementProxy);
    }

    public XAConnection getProxyXaConnection(Connection connection) {
        LrcXAConnectionJavaProxy jdbcLrcXaConnectionProxy = new LrcXAConnectionJavaProxy(connection);

        return (XAConnection) createNewProxy(connection, jdbcLrcXaConnectionProxy, XAConnection.class);
    }

    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        LrcConnectionJavaProxy lrcConnectionJavaProxy = new LrcConnectionJavaProxy(xaResource, connection);

        return (Connection) createNewProxy(connection, lrcConnectionJavaProxy);
    }

    private Object createNewProxy(Object obj, JavaProxyBase<?> proxy, Class<?>... additionalInterfaces) {

        Set<Class<?>> interfaces = interfaceCache.get(obj.getClass());
        if (interfaces == null) {
            interfaces = ClassLoaderUtils.getAllInterfaces(obj.getClass());
            interfaceCache.put(obj.getClass(), interfaces);
        }

        if (additionalInterfaces != null) {
            interfaces = new HashSet<Class<?>>(interfaces);
            interfaces.addAll(Arrays.asList(additionalInterfaces));
        }

        return Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces.toArray(new Class<?>[0]), proxy);
    }
}
