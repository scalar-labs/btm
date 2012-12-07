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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Set;

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

	private ProxyFactory<Connection> proxyConnectionFactory;
	private ProxyFactory<XAConnection> proxyXAConnectionFactory;
	private ProxyFactory<Statement> proxyStatementFactory;
	private ProxyFactory<CallableStatement> proxyCallableStatementFactory;
	private ProxyFactory<PreparedStatement> proxyPreparedStatementFactory;

	JdbcJavaProxyFactory() {
		proxyConnectionFactory = createProxyConnectionFactory();
		proxyXAConnectionFactory = createProxyXAConnectionFactory();
		proxyStatementFactory = createProxyStatementFactory();
		proxyCallableStatementFactory = createProxyCallableStatementFactory();
		proxyPreparedStatementFactory = createProxyPreparedStatementFactory();
	}

	public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
		try {
			ConnectionJavaProxy jdbcConnectionProxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);
			return proxyConnectionFactory.getConstructor().newInstance(jdbcConnectionProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
		try {
			StatementJavaProxy jdbcStatementProxy = new StatementJavaProxy(jdbcPooledConnection, statement);
			return proxyStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
		try {
			CallableStatementJavaProxy jdbcStatementProxy = new CallableStatementJavaProxy(jdbcPooledConnection, statement);
			return proxyCallableStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
		try {
			PreparedStatementJavaProxy jdbcStatementProxy = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);
			return proxyPreparedStatementFactory.getConstructor().newInstance(jdbcStatementProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public XAConnection getProxyXaConnection(Connection connection) {
		try {
			LrcXAConnectionJavaProxy jdbcLrcXaConnectionProxy = new LrcXAConnectionJavaProxy(connection);
			return proxyXAConnectionFactory.getConstructor().newInstance(jdbcLrcXaConnectionProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
		try {
			LrcConnectionJavaProxy lrcConnectionJavaProxy = new LrcConnectionJavaProxy(xaResource, connection);
			return proxyConnectionFactory.getConstructor().newInstance(lrcConnectionJavaProxy);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* High-efficiency proxy factories (basically cached constructors) */

	private ProxyFactory<Connection> createProxyConnectionFactory() {

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
		interfaces.add(PooledConnectionProxy.class);

		return new ProxyFactory<Connection>(interfaces.toArray(new Class<?>[0]));
	}

	private ProxyFactory<Statement> createProxyStatementFactory() {

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

		return new ProxyFactory<Statement>(interfaces.toArray(new Class<?>[0]));
	}

	private ProxyFactory<PreparedStatement> createProxyPreparedStatementFactory() {

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

		return new ProxyFactory<PreparedStatement>(interfaces.toArray(new Class<?>[0]));
	}

	private ProxyFactory<CallableStatement> createProxyCallableStatementFactory() {

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

		return new ProxyFactory<CallableStatement>(interfaces.toArray(new Class<?>[0]));
	}

	private ProxyFactory<XAConnection> createProxyXAConnectionFactory() {

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
		interfaces.add(XAConnection.class);

		return new ProxyFactory<XAConnection>(interfaces.toArray(new Class<?>[0]));
	}

	public static class ProxyFactory<T> {
		private final Class<?>[] interfaces;
		private Reference<Constructor<T>> ctorRef;

		public ProxyFactory(Class<?>[] interfaces) {
			this.interfaces = interfaces;
		}

		public T newInstance(InvocationHandler handler) {
			if (handler == null)
				throw new NullPointerException();

			try {
				return getConstructor().newInstance(new Object[] { handler });
			} catch (Exception e) {
				throw new InternalError(e.toString());
			}
		}

		@SuppressWarnings("unchecked")
		private synchronized Constructor<T> getConstructor() {
			Constructor<T> ctor = ctorRef == null ? null : ctorRef.get();

			if (ctor == null) {
				try {
					ctor = (Constructor<T>) Proxy.getProxyClass(getClass().getClassLoader(), interfaces)
							.getConstructor(new Class[] { InvocationHandler.class });
				} catch (NoSuchMethodException e) {
					throw new InternalError(e.toString());
				}

				ctorRef = new SoftReference<Constructor<T>>(ctor);
			}

			return ctor;
		}
	}
}
