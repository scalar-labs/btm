/**
 * 
 */
package bitronix.tm.resource.jdbc.proxy;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

import javax.sql.XAConnection;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

/**
 * @author Brett Wooldridge
 */
public class JdbcCglibProxyFactory implements JdbcProxyFactory {

    private static Class<Connection> proxyConnectionClass;
    private static Class<PreparedStatement> proxyPreparedStatementClass;

    static {
        createProxyConnectionClass();
        createProxyPreparedStatementClass();
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyConnection(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.Connection)
     */
    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        FastDispatcher<Connection> fastDispatcher = new FastDispatcher<Connection>(connection);
        ConnectionJavaProxy methodInterceptor = new ConnectionJavaProxy(jdbcPooledConnection, connection);
        Interceptor<ConnectionJavaProxy> interceptor = new Interceptor<ConnectionJavaProxy>(methodInterceptor);

        try {
            Connection connectionCglibProxy = proxyConnectionClass.newInstance();
            ((Factory) connectionCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return connectionCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.Statement)
     */
    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyCallableStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.CallableStatement)
     */
    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection,
            CallableStatement statement) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyPreparedStatement(bitronix.tm.resource.jdbc.JdbcPooledConnection, java.sql.PreparedStatement, bitronix.tm.resource.jdbc.LruStatementCache.CacheKey)
     */
    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        FastDispatcher<PreparedStatement> fastDispatcher = new FastDispatcher<PreparedStatement>(statement);
        PreparedStatementJavaProxy methodInterceptor = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);
        Interceptor<PreparedStatementJavaProxy> interceptor = new Interceptor<PreparedStatementJavaProxy>(methodInterceptor);

        try {
            PreparedStatement statementCglibProxy = proxyPreparedStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyXaConnection(java.sql.Connection)
     */
    public XAConnection getProxyXaConnection(Connection connection) {
        return new JdbcJavaProxyFactory().getProxyXaConnection(connection);
    }

    /* (non-Javadoc)
     * @see bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory#getProxyConnection(bitronix.tm.resource.jdbc.lrc.LrcXAResource, java.sql.Connection)
     */
    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        return new JdbcJavaProxyFactory().getProxyConnection(xaResource, connection);
    }

    @SuppressWarnings("unchecked")
    private static void createProxyConnectionClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { Connection.class, PooledConnectionProxy.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new ConnectionJavaProxy()));
        proxyConnectionClass = enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private static void createProxyPreparedStatementClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { PreparedStatement.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new PreparedStatementJavaProxy()));
        proxyPreparedStatementClass = enhancer.createClass();
    }

    // ---------------------------------------------------------------
    //  CGLIB Classes
    // ---------------------------------------------------------------
    
    static class FastDispatcher<T> implements Dispatcher {
        private T delegate;

        public FastDispatcher(T delegate) {
            this.delegate = delegate;
        }

        public Object loadObject() throws Exception {
            return delegate;
        }
    }

    static class Interceptor<T extends JavaProxyBase<?>> implements MethodInterceptor {
        private T interceptor;

        public Interceptor(T interceptor) {
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
                return 1;
            }

            return 0;
        }
    }
}
