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

    private static Queue<Connection> proxyConnectionPool;
    private static Queue<Statement> proxyStatementPool;
    private static Queue<CallableStatement> proxyCallableStatementPool;
    private static Queue<PreparedStatement> proxyPreparedStatementPool;

    private static Class<?> wrapperClass;

    static {
        proxyConnectionPool = new LinkedBlockingQueue<Connection>(500);
        proxyStatementPool = new LinkedBlockingQueue<Statement>(500);
        proxyCallableStatementPool = new LinkedBlockingQueue<CallableStatement>(500);
        proxyPreparedStatementPool = new LinkedBlockingQueue<PreparedStatement>(500);

        try {
            wrapperClass = ClassLoaderUtils.loadClass("java.sql.wrapper");
        }
        catch (ClassNotFoundException cnfe) {
            // continue
        }        
    }

    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        Connection proxyConnection = proxyConnectionPool.poll();
        if (proxyConnection != null) {
            ((ConnectionJavaProxy) Proxy.getInvocationHandler(proxyConnection)).initialize(jdbcPooledConnection, connection);
            return proxyConnection;
        }
        
        ConnectionJavaProxy jdbcConnectionProxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);

        Class<?>[] interfaces;
        if (wrapperClass != null) {
            interfaces = new Class[] { Connection.class, PooledConnectionProxy.class, wrapperClass };
        } else {
            interfaces = new Class[] { Connection.class, PooledConnectionProxy.class };
        }

        return (Connection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, jdbcConnectionProxy);
    }

    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        Statement proxyStatement = proxyStatementPool.poll();
        if (proxyStatement != null) {
            ((StatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement);
            return proxyStatement;
        }

        StatementJavaProxy jdbcStatementProxy = new StatementJavaProxy(jdbcPooledConnection, statement);

        Class<?>[] interfaces;
        if (wrapperClass != null) {
            interfaces = new Class[] { Statement.class, wrapperClass };
        } else {
            interfaces = new Class[] { Statement.class };
        }

        return (Statement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, jdbcStatementProxy);
    }

    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        CallableStatement proxyStatement = proxyCallableStatementPool.poll();
        if (proxyStatement != null) {
            ((CallableStatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement);
            return proxyStatement;
        }

        CallableStatementJavaProxy jdbcStatementProxy = new CallableStatementJavaProxy(jdbcPooledConnection, statement);

        Class<?>[] interfaces;
        if (wrapperClass != null) {
            interfaces = new Class[] { CallableStatement.class, wrapperClass };
        } else {
            interfaces = new Class[] { CallableStatement.class };
        }

        return (CallableStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, jdbcStatementProxy);
    }

    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        PreparedStatement proxyStatement = proxyPreparedStatementPool.poll();
        if (proxyStatement != null) {
            ((PreparedStatementJavaProxy) Proxy.getInvocationHandler(proxyStatement)).initialize(jdbcPooledConnection, statement, cacheKey);
            return proxyStatement;
        }

        PreparedStatementJavaProxy jdbcStatementProxy = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);

        Class<?>[] interfaces;
        if (wrapperClass != null) {
            interfaces = new Class[] { PreparedStatement.class, wrapperClass };
        } else {
            interfaces = new Class[] { PreparedStatement.class };
        }

        return (PreparedStatement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, jdbcStatementProxy);
    }

    public XAConnection getProxyXaConnection(Connection connection) {
        LrcXAConnectionJavaProxy jdbcLrcXaConnectionProxy = new LrcXAConnectionJavaProxy(connection);

        return (XAConnection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { XAConnection.class }, jdbcLrcXaConnectionProxy);
    }

    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        LrcConnectionJavaProxy lrcConnectionJavaProxy = new LrcConnectionJavaProxy(xaResource, connection);

        Class<?>[] interfaces;
        if (wrapperClass != null) {
            interfaces = new Class[] { Connection.class, PooledConnectionProxy.class, wrapperClass };
        } else {
            interfaces = new Class[] { Connection.class, PooledConnectionProxy.class };
        }

        return (Connection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), interfaces, lrcConnectionJavaProxy);
    }

    public void returnProxyConnection(Connection connection) {
        proxyConnectionPool.offer(connection);
    }

    public void returnProxyStatement(Statement statement) {
        proxyStatementPool.offer(statement);
    }

    public void returnProxyCallableStatement(CallableStatement statement) {
        proxyCallableStatementPool.offer(statement);
    }

    public void returnProxyPreparedStatement(PreparedStatement statement) {
        proxyPreparedStatementPool.offer(statement);
    }
}
