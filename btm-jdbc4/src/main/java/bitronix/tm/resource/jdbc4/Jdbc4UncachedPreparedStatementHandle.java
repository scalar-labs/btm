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

import java.io.InputStream;
import java.io.Reader;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.JdbcUncachedPreparedStatementHandle;

public class Jdbc4UncachedPreparedStatementHandle extends JdbcUncachedPreparedStatementHandle {

    public Jdbc4UncachedPreparedStatementHandle(PreparedStatement statement, JdbcPooledConnection pooledConnection) {
    	super(statement, pooledConnection);
    }

    /* java.sql.Wrapper implementation */

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
	    if (PreparedStatement.class.equals(iface)) {
	        return true;
	    }
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
        if (PreparedStatement.class.equals(iface)) {
            return (T) getDelegate();
	    }
	    throw new SQLException(getClass().getName() + " is not a wrapper for interface " + iface.getName());
	}

	/* Delegated JDBC4 methods */

	public boolean isClosed() throws SQLException {
		return getDelegate().isClosed();
	}

	public boolean isPoolable() throws SQLException {
		return getDelegate().isPoolable();
	}

	public void setPoolable(boolean poolable) throws SQLException {
		getDelegate().setPoolable(poolable);
	}

	public void setAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
		getDelegate().setAsciiStream(arg0, arg1, arg2);
	}

	public void setAsciiStream(int arg0, InputStream arg1) throws SQLException {
		getDelegate().setAsciiStream(arg0, arg1);
	}

	public void setBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
		getDelegate().setBinaryStream(arg0, arg1, arg2);
	}

	public void setBinaryStream(int arg0, InputStream arg1) throws SQLException {
		getDelegate().setBinaryStream(arg0, arg1);
	}

	public void setBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
		getDelegate().setBlob(arg0, arg1, arg2);
	}

	public void setBlob(int arg0, InputStream arg1) throws SQLException {
		getDelegate().setBlob(arg0, arg1);
	}

	public void setCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setCharacterStream(arg0, arg1, arg2);
	}

	public void setCharacterStream(int arg0, Reader arg1) throws SQLException {
		getDelegate().setCharacterStream(arg0, arg1);
	}

	public void setClob(int arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setClob(arg0, arg1, arg2);
	}

	public void setClob(int arg0, Reader arg1) throws SQLException {
		getDelegate().setClob(arg0, arg1);
	}

	public void setNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setNCharacterStream(arg0, arg1, arg2);
	}

	public void setNCharacterStream(int arg0, Reader arg1) throws SQLException {
		getDelegate().setNCharacterStream(arg0, arg1);
	}

	public void setNClob(int arg0, NClob arg1) throws SQLException {
		getDelegate().setNClob(arg0, arg1);
	}

	public void setNClob(int arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setNClob(arg0, arg1, arg2);
	}

	public void setNClob(int arg0, Reader arg1) throws SQLException {
		getDelegate().setNClob(arg0, arg1);
	}

	public void setNString(int arg0, String arg1) throws SQLException {
		getDelegate().setNString(arg0, arg1);
	}

	public void setRowId(int arg0, RowId arg1) throws SQLException {
		getDelegate().setRowId(arg0, arg1);
	}

	public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
		getDelegate().setSQLXML(arg0, arg1);
	}
}