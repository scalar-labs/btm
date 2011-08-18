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
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;

import bitronix.tm.resource.jdbc.JdbcCallableStatementHandle;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;

/**
 * CallableStatement {@link Statement} wrapper.
 * <p/>
 * This class is a proxy handler for a CallableStatement. It does not implement
 * the CallableStatement interface or extend a class directly, but you methods
 * implemented here will override those of the underlying delegate. Simply
 * implement a method with the same signature, and the local method will be
 * called rather than the delegate.
 * <p/>
 * 
 * @author brettw
 */
public class Jdbc4CallableStatementHandle extends JdbcCallableStatementHandle {

	public Jdbc4CallableStatementHandle(CallableStatement delegate, JdbcPooledConnection pooledConnection) {
		super(delegate, pooledConnection);
	}

	/* java.sql.Wrapper implementation */

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		if (CallableStatement.class.equals(iface)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
		if (CallableStatement.class.equals(iface)) {
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

	public Reader getCharacterStream(int arg0) throws SQLException {
		return getDelegate().getCharacterStream(arg0);
	}

	public Reader getCharacterStream(String arg0) throws SQLException {
		return getDelegate().getCharacterStream(arg0);
	}

	public Reader getNCharacterStream(int arg0) throws SQLException {
		return getDelegate().getNCharacterStream(arg0);
	}

	public Reader getNCharacterStream(String arg0) throws SQLException {
		return getDelegate().getNCharacterStream(arg0);
	}

	public NClob getNClob(int arg0) throws SQLException {
		return getDelegate().getNClob(arg0);
	}

	public NClob getNClob(String arg0) throws SQLException {
		return getDelegate().getNClob(arg0);
	}

	public String getNString(int arg0) throws SQLException {
		return getDelegate().getNString(arg0);
	}

	public String getNString(String arg0) throws SQLException {
		return getDelegate().getNString(arg0);
	}

	public RowId getRowId(int arg0) throws SQLException {
		return getDelegate().getRowId(arg0);
	}

	public RowId getRowId(String arg0) throws SQLException {
		return getDelegate().getRowId(arg0);
	}

	public SQLXML getSQLXML(int arg0) throws SQLException {
		return getDelegate().getSQLXML(arg0);
	}

	public SQLXML getSQLXML(String arg0) throws SQLException {
		return getDelegate().getSQLXML(arg0);
	}

	public void setAsciiStream(String parameterIndex, InputStream x, long length) throws SQLException {
		getDelegate().setAsciiStream(parameterIndex, x, length);
	}

	public void setAsciiStream(String arg0, InputStream arg1) throws SQLException {
		getDelegate().setAsciiStream(arg0, arg1);
	}

	public void setBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
		getDelegate().setBinaryStream(arg0, arg1, arg2);
	}

	public void setBinaryStream(String arg0, InputStream arg1) throws SQLException {
		getDelegate().setBinaryStream(arg0, arg1);
	}

	public void setBlob(String arg0, Blob arg1) throws SQLException {
		getDelegate().setBlob(arg0, arg1);
	}

	public void setBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
		getDelegate().setBlob(arg0, arg1, arg2);
	}

	public void setBlob(String arg0, InputStream arg1) throws SQLException {
		getDelegate().setBlob(arg0, arg1);
	}

	public void setCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setCharacterStream(arg0, arg1, arg2);
	}

	public void setCharacterStream(String arg0, Reader arg1) throws SQLException {
		getDelegate().setCharacterStream(arg0, arg1);
	}

	public void setClob(String arg0, Clob arg1) throws SQLException {
		getDelegate().setClob(arg0, arg1);
	}

	public void setClob(String arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setClob(arg0, arg1, arg2);
	}

	public void setClob(String arg0, Reader arg1) throws SQLException {
		getDelegate().setClob(arg0, arg1);
	}

	public void setNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setNCharacterStream(arg0, arg1, arg2);
	}

	public void setNCharacterStream(String arg0, Reader arg1) throws SQLException {
		getDelegate().setNCharacterStream(arg0, arg1);
	}

	public void setNClob(String arg0, NClob arg1) throws SQLException {
		getDelegate().setNClob(arg0, arg1);
	}

	public void setNClob(String arg0, Reader arg1, long arg2) throws SQLException {
		getDelegate().setNClob(arg0, arg1, arg2);
	}

	public void setNClob(String arg0, Reader arg1) throws SQLException {
		getDelegate().setNClob(arg0, arg1);
	}

	public void setNString(String arg0, String arg1) throws SQLException {
		getDelegate().setNString(arg0, arg1);
	}

	public void setRowId(String arg0, RowId arg1) throws SQLException {
		getDelegate().setRowId(arg0, arg1);
	}

	public void setSQLXML(String arg0, SQLXML arg1) throws SQLException {
		getDelegate().setSQLXML(arg0, arg1);
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
