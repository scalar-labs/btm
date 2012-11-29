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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import javax.sql.XAConnection;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

/**
 *
 */
public interface JdbcProxyFactory {

    /* Classes should use JdbcProxyFactory.INSTANCE to access the factory */
    JdbcProxyFactory INSTANCE = Initializer.initialize();

    /* Methods used to create the proxies around various JDBC classes */

    Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection);

    Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement);

    CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement);

    PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey);

    XAConnection getProxyXaConnection(Connection connection);

    Connection getProxyConnection(LrcXAResource xaResource, Connection connection);

    /* Methods used to return proxies for pooling */

    void returnProxyConnection(Connection connection);

    void returnProxyStatement(Statement statement);

    void returnProxyCallableStatement(CallableStatement statement);

    void returnProxyPreparedStatement(PreparedStatement statement);

    /**
     * Initializer class used to initialize the proxy factory. 
     */
    class Initializer {
        private static JdbcProxyFactory initialize() {
            try {
                String jdbcProxyFactoryClass = TransactionManagerServices.getConfiguration().getJdbcProxyFactoryClass();
                Class<?> proxyFactoryClass = ClassLoaderUtils.loadClass(jdbcProxyFactoryClass);
                return (JdbcProxyFactory) proxyFactoryClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
