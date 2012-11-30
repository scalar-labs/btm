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
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

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

    private static Queue<Statement> proxyStatementPool;
    private static Queue<CallableStatement> proxyCallableStatementPool;
    private static Queue<PreparedStatement> proxyPreparedStatementPool;

    static {
        proxyStatementPool = new LinkedBlockingQueue<Statement>(500);
        proxyCallableStatementPool = new LinkedBlockingQueue<CallableStatement>(500);
        proxyPreparedStatementPool = new LinkedBlockingQueue<PreparedStatement>(500);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyConnection(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.Connection)
     */
    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        ConnectionJavaProxy jdbcConnectionProxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);

        return (Connection) createNewProxy(connection, jdbcConnectionProxy, PooledConnectionProxy.class);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.Statement)
     */
    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        Statement proxyStatement = proxyStatementPool.poll();
        if (proxyStatement != null) {
            ((StatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement);
            return proxyStatement;
        }

        StatementJavaProxy jdbcStatementProxy = new StatementJavaProxy(jdbcPooledConnection, statement);

        return (Statement) createNewProxy(statement, jdbcStatementProxy);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyCallableStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.CallableStatement)
     */
    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        CallableStatement proxyStatement = proxyCallableStatementPool.poll();
        if (proxyStatement != null) {
            ((CallableStatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement);
            return proxyStatement;
        }

        CallableStatementJavaProxy jdbcStatementProxy = new CallableStatementJavaProxy(jdbcPooledConnection, statement);

        return (CallableStatement) createNewProxy(statement, jdbcStatementProxy);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyPreparedStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.PreparedStatement, bitronix.tm.resource.jdbc.LruStatementCache.CacheKey)
     */
    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        PreparedStatement proxyStatement = proxyPreparedStatementPool.poll();
        if (proxyStatement != null) {
            ((PreparedStatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement, cacheKey);
            return proxyStatement;
        }

        PreparedStatementJavaProxy jdbcStatementProxy = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);

        return (PreparedStatement) createNewProxy(statement, jdbcStatementProxy);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyXaConnection(java.sql.Connection)
     */
    public XAConnection getProxyXaConnection(Connection connection) {
        LrcXAConnectionJavaProxy jdbcLrcXaConnectionProxy = new LrcXAConnectionJavaProxy(connection);

        return (XAConnection) createNewProxy(connection, jdbcLrcXaConnectionProxy, XAConnection.class);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyConnection(bitronix.tm.resource.jdbc.lrc.LrcXAResource, java.sql.Connection)
     */
    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        LrcConnectionJavaProxy lrcConnectionJavaProxy = new LrcConnectionJavaProxy(xaResource, connection);

        return (Connection) createNewProxy(connection, lrcConnectionJavaProxy);
    }

    private Object createNewProxy(Object obj, JavaProxyBase<?> proxy, Class<?>... additionalInterfaces) {
        Class<?>[] interfaces = obj.getClass().getInterfaces();
        if (additionalInterfaces != null) {
            Class<?>[] augmented = new Class<?>[interfaces.length + additionalInterfaces.length];
            System.arraycopy(interfaces, 0, augmented, 0, interfaces.length);
            System.arraycopy(additionalInterfaces, 0, augmented, interfaces.length, additionalInterfaces.length);
            interfaces = augmented;
        }
        return Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, proxy);
    }
}
