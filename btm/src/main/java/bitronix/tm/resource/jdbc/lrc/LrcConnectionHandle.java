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
package bitronix.tm.resource.jdbc.lrc;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

/**
 * Connection handle implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author lorban, brettw
 */
public class LrcConnectionHandle implements Connection {

    private final Connection delegate;
    private final LrcXAResource xaResource;

    public LrcConnectionHandle(LrcXAResource xaResource, Connection delegate) {
        this.delegate = delegate;
        this.xaResource = xaResource;
    }

    public Connection getConnection() {
        return delegate;
    }

    protected Connection getDelegate() throws SQLException {
        if (delegate == null)
            throw new SQLException("connection is closed");
        return delegate;
    }

    /* wrapped Connection methods that have special XA semantics */

    public void close() throws SQLException {
        delegate = null;
    }

    public boolean isClosed() throws SQLException {
        return delegate == null;
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX && autoCommit)
            throw new SQLException("XA transaction started, cannot enable autocommit mode");
        getDelegate().setAutoCommit(autoCommit);
    }

    public void commit() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call commit directly on connection");
        getDelegate().commit();
    }

    public void rollback() throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        getDelegate().rollback();
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        if (xaResource.getState() != LrcXAResource.NO_TX)
            throw new SQLException("XA transaction started, cannot call rollback directly on connection");
        getDelegate().rollback(savepoint);
    }

    public String toString() {
        return "a JDBC LrcConnectionHandle on " + xaResource;
    }

    /* Delegated methods */

	public Statement createStatement() throws SQLException {
		return delegate.createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return delegate.prepareStatement(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return delegate.prepareCall(sql);
	}

	public String nativeSQL(String sql) throws SQLException {
		return delegate.nativeSQL(sql);
	}

	public boolean getAutoCommit() throws SQLException {
		return delegate.getAutoCommit();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return delegate.getMetaData();
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		delegate.setReadOnly(readOnly);
	}

	public boolean isReadOnly() throws SQLException {
		return delegate.isReadOnly();
	}

	public void setCatalog(String catalog) throws SQLException {
		delegate.setCatalog(catalog);
	}

	public String getCatalog() throws SQLException {
		return delegate.getCatalog();
	}

	public void setTransactionIsolation(int level) throws SQLException {
		delegate.setTransactionIsolation(level);
	}

	public int getTransactionIsolation() throws SQLException {
		return delegate.getTransactionIsolation();
	}

	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return delegate.createStatement(resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return delegate.getTypeMap();
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		delegate.setTypeMap(map);
	}

	public void setHoldability(int holdability) throws SQLException {
		delegate.setHoldability(holdability);
	}

	public int getHoldability() throws SQLException {
		return delegate.getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException {
		return delegate.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		return delegate.setSavepoint(name);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		delegate.releaseSavepoint(savepoint);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.prepareStatement(sql, autoGeneratedKeys);
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return delegate.prepareStatement(sql, columnIndexes);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return delegate.prepareStatement(sql, columnNames);
	}
}
