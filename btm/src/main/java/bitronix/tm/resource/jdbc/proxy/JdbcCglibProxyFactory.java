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
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
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
    private static Class<Statement> proxyStatementClass;
    private static Class<CallableStatement> proxyCallableStatementClass;
    private static Class<PreparedStatement> proxyPreparedStatementClass;

    static {
        proxyConnectionClass = createProxyConnectionClass();
        proxyStatementClass = createProxyStatementClass();
        proxyCallableStatementClass = createProxyCallableStatementClass();
        proxyPreparedStatementClass = createProxyPreparedStatementClass();
    }

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

    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        FastDispatcher<Statement> fastDispatcher = new FastDispatcher<Statement>(statement);
        StatementJavaProxy methodInterceptor = new StatementJavaProxy(jdbcPooledConnection, statement);
        Interceptor<StatementJavaProxy> interceptor = new Interceptor<StatementJavaProxy>(methodInterceptor);

        try {
            Statement statementCglibProxy = proxyStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        FastDispatcher<CallableStatement> fastDispatcher = new FastDispatcher<CallableStatement>(statement);
        CallableStatementJavaProxy methodInterceptor = new CallableStatementJavaProxy(jdbcPooledConnection, statement);
        Interceptor<CallableStatementJavaProxy> interceptor = new Interceptor<CallableStatementJavaProxy>(methodInterceptor);

        try {
            CallableStatement statementCglibProxy = proxyCallableStatementClass.newInstance();
            ((Factory) statementCglibProxy).setCallbacks(new Callback[] { fastDispatcher, interceptor });
            return statementCglibProxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public XAConnection getProxyXaConnection(Connection connection) {
        return new JdbcJavaProxyFactory().getProxyXaConnection(connection);
    }

    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        return new JdbcJavaProxyFactory().getProxyConnection(xaResource, connection);
    }

    // ---------------------------------------------------------------
    //  Generate CGLIB Proxy Classes
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Class<Connection> createProxyConnectionClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { Connection.class, PooledConnectionProxy.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new ConnectionJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<PreparedStatement> createProxyPreparedStatementClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { PreparedStatement.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new PreparedStatementJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<Statement> createProxyStatementClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { Statement.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new StatementJavaProxy()));
        return enhancer.createClass();
    }

    @SuppressWarnings("unchecked")
    private static Class<CallableStatement> createProxyCallableStatementClass() {
        Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[] { CallableStatement.class } );
        enhancer.setCallbackTypes(new Class[] {FastDispatcher.class, Interceptor.class} );
        enhancer.setCallbackFilter(new InterceptorFilter(new CallableStatementJavaProxy()));
        return enhancer.createClass();
    }


    // ---------------------------------------------------------------
    //  CGLIB Classes
    // ---------------------------------------------------------------
    
    static class FastDispatcher<T> implements LazyLoader {
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
