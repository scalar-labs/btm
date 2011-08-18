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
package bitronix.tm.resource.jdbc4;

import java.sql.SQLException;
import java.sql.Statement;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.JdbcStatementHandle;

/**
 * Statement {@link Statement} wrapper.
 * <p/>
 * This class is a proxy handler for a Statement.  It does not
 * implement the Statement interface or extend a class directly,
 * but you methods implemented here will override those of the
 * underlying delegate.  Simply implement a method with the same
 * signature, and the local method will be called rather than the delegate.
 * <p/>
 *
 * @author brettw
 */
public class Jdbc4StatementHandle extends JdbcStatementHandle {

    public Jdbc4StatementHandle(Statement statement, JdbcPooledConnection pooledConnection) {
    	super(statement, pooledConnection);
    }

    /* java.sql.Wrapper implementation */

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
	    if (Statement.class.equals(iface)) {
	        return true;
	    }
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
        if (Statement.class.equals(iface)) {
            return (T) getDelegate();
	    }
	    throw new SQLException(getClass().getName() + " is not a wrapper for interface " + iface.getName());
	}

	public boolean isClosed() throws SQLException {
		return getDelegate().isClosed();
	}

	public boolean isPoolable() throws SQLException {
		return getDelegate().isPoolable();
	}

	public void setPoolable(boolean arg0) throws SQLException {
		getDelegate().setPoolable(arg0);
	}


}
