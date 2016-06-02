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
package bitronix.tm.resource.jdbc;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import bitronix.tm.utils.ClassLoaderUtils;

public class JdbcUncachedPreparedStatementHandle extends BaseProxyHandlerClass {

    // The 'parent' connection. Used to remove this statement delegate
    // from the un-closed statements list when close() is called.
    private final JdbcPooledConnection parentConnection;

    private final PreparedStatement delegate;

    public JdbcUncachedPreparedStatementHandle(PreparedStatement delegate, JdbcPooledConnection pooledConnection) {
        this.delegate = delegate;
        this.parentConnection = pooledConnection;
    }

    public Object getProxiedDelegate() throws Exception {
        return delegate;
    }

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isAssignableFrom(delegate.getClass());
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(delegate.getClass())) {
            return (T) delegate;
	    }
	    throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
	}

    /* Overridden methods of java.sql.PreparedStatement */

    public void close() throws SQLException {
        parentConnection.unregisterUncachedStatement(delegate);
        delegate.close();
    }
    

    public ResultSet executeQuery(String sql) throws SQLException {
        return (ResultSet) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{ResultSet.class}, new JdbcResultSetHandle(delegate.executeQuery(sql), this));
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        return (ResultSet) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{ResultSet.class}, new JdbcResultSetHandle(delegate.getGeneratedKeys(), this));
    }

    public ResultSet getResultSet() throws SQLException {
        return (ResultSet) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{ResultSet.class}, new JdbcResultSetHandle(delegate.getResultSet(), this));
    }

    public ResultSet executeQuery() throws SQLException {
        return (ResultSet) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{ResultSet.class}, new JdbcResultSetHandle(delegate.executeQuery(), this));
    }
    
    public boolean equals(Object object) {
        Object handler = Proxy.getInvocationHandler(object);
        return super.equals(handler);
    }

}
