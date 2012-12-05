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
import java.security.CodeSource;
import java.security.ProtectionDomain;
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
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

/**
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

    public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection) {
        try {
            return proxyConnectionConstructor.newInstance(jdbcPooledConnection, connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement) {
        try {
            return proxyStatementConstructor.newInstance(jdbcPooledConnection, statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement) {
        try {
            return proxyCallableStatementConstructor.newInstance(jdbcPooledConnection, statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey) {
        try {
            return proxyPreparedStatementConstructor.newInstance(jdbcPooledConnection, statement, cacheKey);
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
    //  Generate Javassist Proxy Classes
    // ---------------------------------------------------------------

    private void createProxyConnectionClass() {
        try {
            CtClass superClass = classPool.getCtClass(ConnectionJavaProxy.class.getName());
            CtClass connectionClassCt = classPool.makeClass("bitronix.tm.resource.jdbc.proxy.ConnectionJavassistProxy", superClass);

            Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
            interfaces.add(PooledConnectionProxy.class);

            Class<Connection> proxyClass = generateProxyClass(Connection.class, superClass, connectionClassCt, interfaces);
            proxyConnectionConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, Connection.class} );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProxyStatementClass() {
        try {
            CtClass superClass = classPool.getCtClass(StatementJavaProxy.class.getName());
            CtClass statementClassCt = classPool.makeClass("bitronix.tm.resource.jdbc.proxy.StatementJavassistProxy", superClass);

            Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

            Class<Statement> proxyClass = generateProxyClass(Statement.class, superClass, statementClassCt, interfaces);
            proxyStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, Statement.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProxyCallableStatementClass() {
        try {
            CtClass superClass = classPool.getCtClass(CallableStatementJavaProxy.class.getName());
            CtClass statementClassCt = classPool.makeClass("bitronix.tm.resource.jdbc.proxy.CallableStatementJavassistProxy", superClass);

            Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

            Class<CallableStatement> proxyClass = generateProxyClass(CallableStatement.class, superClass, statementClassCt, interfaces);
            proxyCallableStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, CallableStatement.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createProxyPreparedStatementClass() {
        try {
            CtClass superClass = classPool.getCtClass(PreparedStatementJavaProxy.class.getName());
            CtClass statementClassCt = classPool.makeClass("bitronix.tm.resource.jdbc.proxy.PreparedStatementJavassistProxy", superClass);

            Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

            Class<PreparedStatement> proxyClass = generateProxyClass(PreparedStatement.class, superClass, statementClassCt, interfaces);
            proxyPreparedStatementConstructor = proxyClass.getConstructor(new Class<?>[] {JdbcPooledConnection.class, PreparedStatement.class, CacheKey.class});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> generateProxyClass(Class<T> primaryInterface, CtClass superClass, CtClass targetCt, Set<Class<?>> interfaces)
        throws NotFoundException, CannotCompileException, NoSuchMethodException, SecurityException {

        // Generate constructors that simply call super(..)
        for (CtConstructor constructor : superClass.getDeclaredConstructors()) {
            CtConstructor ctConstructor = CtNewConstructor.make(constructor.getParameterTypes(), constructor.getExceptionTypes(), targetCt);
            targetCt.addConstructor(ctConstructor);
        }

        // Make a set of method signatures we inherit implementation for, so we don't generate delegates for these
        Set<String> superSigs = new HashSet<String>();
        for (CtMethod method : superClass.getMethods()) {
            superSigs.add(method.getName() + method.getSignature());
        }

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
                CtClass returnType = method.getReturnType();
                if (returnType != CtClass.voidType) {
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
