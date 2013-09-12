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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import bitronix.tm.utils.ClassLoaderUtils;

/**
 * ResultSet {@link ResultSet} wrapper.
 * <p/>
 * This class is a proxy handler for a ResultSet.  It does not
 * implement the ResultSet interface or extend a class directly,
 * but you methods implemented here will override those of the
 * underlying delegate.  Simply implement a method with the same
 * signature, and the local method will be called rather than the delegate.
 * <p/>
 *
 * @author brettw
 */
public class JdbcResultSetHandle extends BaseProxyHandlerClass { // implements ResultSet

    private final ResultSet delegate;
    private final BaseProxyHandlerClass statementHandle;

    public JdbcResultSetHandle(ResultSet delegate, BaseProxyHandlerClass statementHandle) {
        this.delegate = delegate;
        this.statementHandle = statementHandle;
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

    /* Internal methods */

    public Object getProxiedDelegate() throws Exception {
        return delegate;
    }

    /* Overridden methods of java.sql.ResultSet */

    public void close() throws SQLException {
        delegate.close();
    }


    public Statement getStatement() throws SQLException {
        return (Statement) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[]{Statement.class}, statementHandle);
    }

}
