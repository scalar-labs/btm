package bitronix.tm.resource.jdbc4.lrc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Properties;

import bitronix.tm.resource.jdbc.lrc.LrcConnectionHandle;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

public class LrcJdbc4ConnectionHandle extends LrcConnectionHandle {

	public LrcJdbc4ConnectionHandle(LrcXAResource xaResource, Connection delegate) {
		super(xaResource, delegate);
	}

    /* java.sql.Wrapper implementation */

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (Connection.class.equals(iface)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
        if (Connection.class.equals(iface)) {
            return (T) getDelegate();
        }
        throw new SQLException(getClass().getName() + " is not a wrapper for interface " + iface.getName());
    }

	/* Delegated JDBC4 methods */

	public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
		return getDelegate().createArrayOf(arg0, arg1);
	}

	public Blob createBlob() throws SQLException {
		return getDelegate().createBlob();
	}

	public Clob createClob() throws SQLException {
		return getDelegate().createClob();
	}

	public NClob createNClob() throws SQLException {
		return getDelegate().createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return getDelegate().createSQLXML();
	}

	public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
		return getDelegate().createStruct(arg0, arg1);
	}

	public Properties getClientInfo() throws SQLException {
		return getDelegate().getClientInfo();
	}

	public String getClientInfo(String arg0) throws SQLException {
		return getDelegate().getClientInfo(arg0);
	}

	public boolean isValid(int arg0) throws SQLException {
		return getDelegate().isValid(arg0);
	}

	public void setClientInfo(Properties arg0) throws SQLClientInfoException {
		getConnection().setClientInfo(arg0);
	}

	public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
		getConnection().setClientInfo(arg0, arg1);
	}
}
