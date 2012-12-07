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

import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import javax.sql.XAConnection;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 * This class generates JDBC proxy classes using Javassist bytecode generated
 * implementations.  This is the most efficient proxy factory.
 *
 * @author Brett Wooldridge
 */
public class JdbcJavassistProxyFactory implements JdbcProxyFactory {

    private ClassMap classMap;
    private ClassPool classPool;

    private Constructor<Connection> proxyConnectionConstructor;
    private Constructor<Statement> proxyStatementConstructor;
    private Constructor<CallableStatement> proxyCallableStatementConstructor;
    private Constructor<PreparedStatement> proxyPreparedStatementConstructor;

    // For LRC we just use the standard Java Proxies
    private JdbcJavaProxyFactory lrcProxyFactory;

    JdbcJavassistProxyFactory() {
        classMap = new ClassMap();
        ClassPool defaultPool = ClassPool.getDefault();
        classPool = new ClassPool(defaultPool);
        classPool.insertClassPath(new ClassClassPath(this.getClass()));
        classPool.childFirstLookup = true;

        createProxyConnectionClass();
        createProxyStatementClass();
        createProxyCallableStatementClass();
        createProxyPreparedStatementClass();

        lrcProxyFactory = new JdbcJavaProxyFactory();

        // Clear the map, we don't need it anymore
        classMap.clear();
        classPool = null;
    }

    /** {@inheritDoc} */
    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        try {
            return proxyConnectionConstructor.newInstance(jdbcPooledConnection, connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        try {
            return proxyStatementConstructor.newInstance(jdbcPooledConnection, statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        try {
            return proxyCallableStatementConstructor.newInstance(jdbcPooledConnection, statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        try {
            return proxyPreparedStatementConstructor.newInstance(jdbcPooledConnection, statement, cacheKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public XAConnection getProxyXaConnection(Connection connection) {
        return lrcProxyFactory.getProxyXaConnection(connection);
    }

    /** {@inheritDoc} */
    public Connection getProxyConnection(LrcXAResource xaResource, Connection connection) {
        return lrcProxyFactory.getProxyConnection(xaResource, connection);
    }

    // ---------------------------------------------------------------
    //  Generate Javassist Proxy Classes
    // ---------------------------------------------------------------

    /**
     * Create a proxy class: class ConnectionJavassistProxy extends ConnectionJavaProxy implements java.sql.Connection 
     */
    private void createProxyConnectionClass() {
        try {
            Class<Connection> proxyClass = generateProxyClass(Connection.class, ConnectionJavaProxy.class);
            proxyConnectionConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, Connection.class} );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a proxy class: class StatementJavassistProxy extends StatementJavaProxy implements java.sql.Statement 
     */
    private void createProxyStatementClass() {
        try {
            Class<Statement> proxyClass = generateProxyClass(Statement.class, StatementJavaProxy.class);
            proxyStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, Statement.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a proxy class: class CallableStatementJavassistProxy extends CallableStatementJavaProxy implements java.sql.CallableStatement 
     */
    private void createProxyCallableStatementClass() {
        try {
            Class<CallableStatement> proxyClass = generateProxyClass(CallableStatement.class, CallableStatementJavaProxy.class);
            proxyCallableStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, CallableStatement.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a proxy class: class PreparedStatementJavassistProxy extends PreparedStatementJavaProxy implements java.sql.PreparedStatement 
     */
    private void createProxyPreparedStatementClass() {
        try {
            Class<PreparedStatement> proxyClass = generateProxyClass(PreparedStatement.class, PreparedStatementJavaProxy.class);
            proxyPreparedStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, PreparedStatement.class, CacheKey.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> generateProxyClass(Class<T> primaryInterface, Class<?> superClass)
        throws NotFoundException, CannotCompileException, NoSuchMethodException, SecurityException {

        // Make a new class that extends one of the JavaProxy classes (ie. superClass); use the name to XxxJavassistProxy instead of XxxJavaProxy
        String superClassName = superClass.getName();
        CtClass superClassCt = classPool.getCtClass(superClassName);
        CtClass targetCt = classPool.makeClass(superClassName.replace("JavaProxy", "JavassistProxy"), superClassCt);

        // Generate constructors that simply call super(..)
        for (CtConstructor constructor : superClassCt.getConstructors()) {
            CtConstructor ctConstructor = CtNewConstructor.make(constructor.getParameterTypes(), constructor.getExceptionTypes(), targetCt);
            targetCt.addConstructor(ctConstructor);
        }

        // Make a set of method signatures we inherit implementation for, so we don't generate delegates for these
        Set<String> superSigs = new HashSet<String>();
        for (CtMethod method : superClassCt.getMethods()) {
            superSigs.add(method.getName() + method.getSignature());
        }

        Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(primaryInterface);
        for (Class<?> intf : interfaces) {
            CtClass intfCt = classPool.getCtClass(intf.getName());
            targetCt.addInterface(intfCt);
            for (CtMethod intfMethod : intfCt.getDeclaredMethods()) {
                if (superSigs.contains(intfMethod.getName() + intfMethod.getSignature())) {
                    // don't generate delegates for methods we override
                    continue;
                }

                // Generate a method that simply invokes the same method on the delegate
                CtMethod method = CtNewMethod.copy(intfMethod, targetCt, classMap);
                StringBuilder call = new StringBuilder("{");
                if ( method.getReturnType() != CtClass.voidType) {
                    call.append("return ");
                }
                call.append("((").append(primaryInterface.getName()).append(')'); // cast to primary interface
                call.append("delegate).");
                call.append(method.getName()).append("($$);");
                call.append('}');
                method.setBody(call.toString());
                targetCt.addMethod(method);
            }
        }

        return targetCt.toClass(ClassLoaderUtils.getClassLoader(), null);
    }
}
