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
import java.lang.reflect.InvocationHandler;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import javax.sql.XAConnection;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 * @author Brett Wooldridge
 */
public class JdbcCglibProxyFactory implements JdbcProxyFactory {

    private Class<Connection> proxyConnectionClass;
    private Class<Statement> proxyStatementClass;
    private Class<CallableStatement> proxyCallableStatementClass;
    private Class<PreparedStatement> proxyPreparedStatementClass;

    // For LRC we just use the standard Java Proxies
    private JdbcJavaProxyFactory lrcProxyFactory;

    JdbcCglibProxyFactory() {
        proxyConnectionClass = createProxyConnectionClass();
        proxyStatementClass = createProxyStatementClass();
        proxyCallableStatementClass = createProxyCallableStatementClass();
        proxyPreparedStatementClass = createProxyPreparedStatementClass();
        lrcProxyFactory = new JdbcJavaProxyFactory();
    }

    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        ConnectionJavaProxy methodInterceptor = new ConnectionJavaProxy(jdbcPooledConnection, connection);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(connection);

        try {
            Connection connectionCglibProxy = proxyConnectionClass.newInstance();
            ((Factory) connectionCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return connectionCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        StatementJavaProxy methodInterceptor = new StatementJavaProxy(jdbcPooledConnection, statement);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            Statement statementCglibProxy = proxyStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        CallableStatementJavaProxy methodInterceptor = new CallableStatementJavaProxy(jdbcPooledConnection, statement);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            CallableStatement statementCglibProxy = proxyCallableStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        PreparedStatementJavaProxy methodInterceptor = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);
        Interceptor interceptor = new Interceptor(methodInterceptor);
        FastDispatcher fastDispatcher = new FastDispatcher(statement);

        try {
            PreparedStatement statementCglibProxy = proxyPreparedStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public XAConnection getProxyXaConnection(Connection connection) {
        return lrcProxyFactory.getProxyXaConnection(connection);
    }

    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        return lrcProxyFactory.getProxyConnection(xaResource, connection);
    }

    // ---------------------------------------------------------------
    //  Generate CGLIB Proxy Classes
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Class<Connection> createProxyConnectionClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
        interfaces.add(PooledConnectionProxy.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new ConnectionJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<PreparedStatement> createProxyPreparedStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new PreparedStatementJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<Statement> createProxyStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new StatementJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private Class<CallableStatement> createProxyCallableStatementClass() {
        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new CallableStatementJavaProxy()));
        return enhancer.createClass();
    }


    // ---------------------------------------------------------------
    //  CGLIB Classes
    // ---------------------------------------------------------------
    
    static class FastDispatcher implements LazyLoader {
        private Object delegate;

        public FastDispatcher(Object delegate) {
            this.delegate = delegate;
        }

        public Object loadObject() throws Exception {
            return delegate;
        }
    }

    static class Interceptor implements MethodInterceptor {
        private InvocationHandler interceptor;

        public Interceptor(InvocationHandler interceptor) {
            this.interceptor = interceptor;
        }
        
        public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy fastProxy) throws Throwable {
            return interceptor.invoke(interceptor, method, args);
        }
    }

    static class InterceptorFilter implements CallbackFilter {
        private Map<String, Method> methodMap;

        public InterceptorFilter(JavaProxyBase<?> proxyClass) {
            methodMap = proxyClass.getMethodMap();
        }

        public int accept(Method method) {
            if (methodMap.containsKey(JavaProxyBase.getMethodKey(method))) {
                // Use the Interceptor
                return 1;
            }

            // Use the FastDispatcher
            return 0;
        }
    }
}
