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

import bitronix.tm.internal.BitronixRollbackSystemException;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Implementation of a JDBC pooled connection wrapping vendor's {@link XAConnection} implementation.
 *
 * @author lorban, brettw
 */
public class JdbcPooledConnection extends AbstractXAResourceHolder implements StateChangeListener, JdbcPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JdbcPooledConnection.class);

    private final static int DETECTION_TIMEOUT = 5; // seconds

    private final XAConnection xaConnection;
    private final Connection connection;
    private final XAResource xaResource;
    private final PoolingDataSource poolingDataSource;
    private final LruStatementCache statementsCache;
    private final List<Statement> uncachedStatements;
    private volatile int usageCount;

    /* management */
    private final String jmxName;
    private volatile Date acquisitionDate;
    private volatile Date lastReleaseDate;

    private volatile int jdbcVersionDetected;
    private volatile Method isValidMethod;


    public JdbcPooledConnection(PoolingDataSource poolingDataSource, XAConnection xaConnection) throws SQLException {
        this.poolingDataSource = poolingDataSource;
        this.xaConnection = xaConnection;
        this.xaResource = xaConnection.getXAResource();
        this.statementsCache = new LruStatementCache(poolingDataSource.getPreparedStatementCacheSize());
        this.uncachedStatements = Collections.synchronizedList(new ArrayList<Statement>());
        this.lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
        statementsCache.addEvictionListener(new LruEvictionListener() {
            public void onEviction(Object value) {
                PreparedStatement stmt = (PreparedStatement) value;
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    log.warn("error closing evicted statement", ex);
                }
            }
        });

        connection = xaConnection.getConnection();
        detectJdbcVersion(connection);
        addStateChangeEventListener(this);

        if (poolingDataSource.getClassName().equals(LrcXADataSource.class.getName())) {
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing twoPcOrderingPosition to ALWAYS_LAST_POSITION");
            poolingDataSource.setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing deferConnectionRelease to true");
            poolingDataSource.setDeferConnectionRelease(true);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing useTmJoin to true");
            poolingDataSource.setUseTmJoin(true);
        }

        this.jmxName = "bitronix.tm:type=JDBC,UniqueName=" + ManagementRegistrar.makeValidName(poolingDataSource.getUniqueName()) + ",Id=" + poolingDataSource.incCreatedResourcesCounter();
        ManagementRegistrar.register(jmxName, this);

        poolingDataSource.fireOnAcquire(connection);
    }

    private synchronized void detectJdbcVersion(Connection connection) {
        if (jdbcVersionDetected > 0)
            return;

        try {
            isValidMethod = connection.getClass().getMethod("isValid", Integer.TYPE);
            isValidMethod.invoke(connection, DETECTION_TIMEOUT); // test invoke
            jdbcVersionDetected = 4;
            if (!poolingDataSource.isEnableJdbc4ConnectionTest()) {
                if (log.isDebugEnabled()) log.debug("dataSource is JDBC4 or newer and supports isValid(), but enableJdbc4ConnectionTest is not set or is false");
            }
        } catch (Exception ex) {
            jdbcVersionDetected = 3;
        } catch (AbstractMethodError er) {
            // this happens if the driver implements JDBC 3 but runs on JDK 1.6+ (which embeds the JDBC 4 interfaces)
            jdbcVersionDetected = 3;
        }
        if (log.isDebugEnabled()) log.debug("detected JDBC connection class '" + connection.getClass() + "' is version " + jdbcVersionDetected + " type");
    }

    private void applyIsolationLevel() throws SQLException {
        String isolationLevel = getPoolingDataSource().getIsolationLevel();
        if (isolationLevel != null) {
            int level = translateIsolationLevel(isolationLevel);
            if (level < 0) {
                log.warn("invalid transaction isolation level '" + isolationLevel + "' configured, keeping the default isolation level.");
            }
            else {
                if (log.isDebugEnabled()) log.debug("setting connection's isolation level to " + isolationLevel);
                connection.setTransactionIsolation(level);
            }
        }
    }

    private static int translateIsolationLevel(String isolationLevelGuarantee) {
        if ("READ_COMMITTED".equals(isolationLevelGuarantee)) return Connection.TRANSACTION_READ_COMMITTED;
        if ("READ_UNCOMMITTED".equals(isolationLevelGuarantee)) return Connection.TRANSACTION_READ_UNCOMMITTED;
        if ("REPEATABLE_READ".equals(isolationLevelGuarantee)) return Connection.TRANSACTION_REPEATABLE_READ;
        if ("SERIALIZABLE".equals(isolationLevelGuarantee)) return Connection.TRANSACTION_SERIALIZABLE;
        return -1;
    }

    public void close() throws SQLException {
        // this should never happen, should we throw an exception or log at warn/error?
        if (usageCount > 0) {
            if (log.isDebugEnabled()) log.debug("close connection with usage count > 0, " + this);
        }

        setState(STATE_CLOSED);

        // cleanup of pooled resources
        statementsCache.clear();

        ManagementRegistrar.unregister(jmxName);

        connection.close();
        xaConnection.close();

        poolingDataSource.fireOnDestroy(connection);
    }

    public RecoveryXAResourceHolder createRecoveryXAResourceHolder() {
        return new RecoveryXAResourceHolder(this);
    }

    private void testConnection(Connection connection) throws SQLException {
        if (poolingDataSource.isEnableJdbc4ConnectionTest() && jdbcVersionDetected >= 4) {
            Boolean isValid = null;
            try {
                if (log.isDebugEnabled()) log.debug("testing with JDBC4 isValid() method, connection of " + this);
                isValid = (Boolean) isValidMethod.invoke(connection, poolingDataSource.getAcquisitionTimeout());
            } catch (Exception e) {
                log.warn("dysfunctional JDBC4 Connection.isValid() method, or negative acquisition timeout, in call to test connection of " + this + ".  Falling back to test query.");
                jdbcVersionDetected = 3;
            }
            // if isValid is null, and exception was caught above and we fall through to the query test
            if (isValid != null) {
                if (isValid) {
                    if (log.isDebugEnabled()) log.debug("isValid successfully tested connection of " + this);
                    return;
                }
                throw new SQLException("connection is no longer valid");
            }
        }

        String query = poolingDataSource.getTestQuery();
        if (query == null) {
            if (log.isDebugEnabled()) log.debug("no query to test connection of " + this + ", skipping test");
            return;
        }

        // Throws a SQLException if the connection is dead
        if (log.isDebugEnabled()) log.debug("testing with query '" + query + "' connection of " + this);
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        rs.close();
        stmt.close();
        if (log.isDebugEnabled()) log.debug("testQuery successfully tested connection of " + this);
    }

    protected void release() throws SQLException {
        if (log.isDebugEnabled()) log.debug("releasing to pool " + this);
        --usageCount;

        // delisting
        try {
            TransactionContextHelper.delistFromCurrentTransaction(this);
        }
        catch (BitronixRollbackSystemException ex) {
            throw (SQLException) new SQLException("unilateral rollback of " + this).initCause(ex);
        }
        catch (SystemException ex) {
            throw (SQLException) new SQLException("error delisting " + this).initCause(ex);
        }
        finally {
            // Only requeue a connection if it is no longer in use.  In the case of non-shared connections,
            // usageCount will always be 0 here, so the default behavior is unchanged.
            if (usageCount == 0) {
                try {
                    TransactionContextHelper.requeue(this, poolingDataSource);
                } catch (BitronixSystemException ex) {
                    // Requeue failed, restore the usageCount to previous value (see testcase
                    // NewJdbcStrangeUsageMockTest.testClosingSuspendedConnectionsInDifferentContext).
                    // This can happen when a close is attempted while the connection is participating
                    // in a global transaction.
                    usageCount++;

                    // this may hide the exception thrown by delistFromCurrentTransaction() but
                    // an error requeuing must absolutely be reported as an exception.
                    // Too bad if this happens... See DualSessionWrapper.close() as well.
                    throw (SQLException) new SQLException("error requeuing " + this).initCause(ex);
                }

                if (log.isDebugEnabled()) log.debug("released to pool " + this);
            }
            else {
                if (log.isDebugEnabled()) log.debug("not releasing " + this + " to pool yet, connection is still shared");
            }
        } // finally
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public ResourceBean getResourceBean() {
        return getPoolingDataSource();
    }

    public PoolingDataSource getPoolingDataSource() {
        return poolingDataSource;
    }

    public List<XAResourceHolder> getXAResourceHolders() {
        return Arrays.asList((XAResourceHolder) this);
    }

    public Object getConnectionHandle() throws Exception {
        if (log.isDebugEnabled()) log.debug("getting connection handle from " + this);
        int oldState = getState();

        // Increment the usage count
        usageCount++;

        // Only transition to STATE_ACCESSIBLE on the first usage.  If we're not sharing 
        // connections (default behavior) usageCount is always 1 here, so this transition
        // will always occur (current behavior unchanged).  If we _are_ sharing connections,
        // and this is _not_ the first usage, it is valid for the state to already be 
        // STATE_ACCESSIBLE.  Calling setState() with STATE_ACCESSIBLE when the state is
        // already STATE_ACCESSIBLE fails the sanity check in AbstractXAStatefulHolder.
        // Even if the connection is shared (usageCount > 1), if the state was STATE_NOT_ACCESSIBLE
        // we transition back to STATE_ACCESSIBLE.
        if (usageCount == 1 || oldState == STATE_NOT_ACCESSIBLE) {
            setState(STATE_ACCESSIBLE);
        }

        if (oldState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("connection " + xaConnection + " was in state IN_POOL, testing it");
            testConnection(connection);
            applyIsolationLevel();
            applyCursorHoldabilty();
            if (TransactionContextHelper.currentTransaction() == null) {
                // it is safe to set the auto-commit flag outside of a global transaction
                applyLocalAutoCommit();
            }
        }
        else {
            if (log.isDebugEnabled()) log.debug("connection " + xaConnection + " was in state " + Decoder.decodeXAStatefulHolderState(oldState) + ", no need to test it");
        }

        if (log.isDebugEnabled()) log.debug("got connection handle from " + this);
        return new JdbcConnectionHandle(this, connection);
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("requeued JDBC connection of " + poolingDataSource);
            lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
        }
        if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
            acquisitionDate = new Date(MonotonicClock.currentTimeMillis());
        }
        if (oldState == STATE_NOT_ACCESSIBLE && newState == STATE_ACCESSIBLE) {
            TransactionContextHelper.recycle(this);
        }
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
        if (futureState == STATE_IN_POOL) {
            if (usageCount > 0) {
                log.warn("usage count too high (" + usageCount + ") on connection returned to pool " + source);
            }
        }

        if (futureState == STATE_IN_POOL || futureState == STATE_NOT_ACCESSIBLE) {
            // close all uncached statements
            if (log.isDebugEnabled()) log.debug("closing " + uncachedStatements.size() + " dangling uncached statement(s)");
            for (Statement statement : uncachedStatements) {
                try {
                    statement.close();
                } catch (SQLException ex) {
                    if (log.isDebugEnabled()) log.debug("error trying to close uncached statement " + statement, ex);
                }
            }
            uncachedStatements.clear();

            // clear SQL warnings
            try {
                if (log.isDebugEnabled()) log.debug("clearing warnings of " + connection);
                connection.clearWarnings();
            } catch (SQLException ex) {
                if (log.isDebugEnabled()) log.debug("error cleaning warnings of " + connection, ex);
            }
        }
    }

    /**
     * Get a PreparedStatement from cache.
     * @param stmt the key that has been used to cache the statement.
     * @return the cached statement corresponding to the key or null if no statement is cached under that key.
     */
    protected JdbcPreparedStatementHandle getCachedStatement(JdbcPreparedStatementHandle stmt) {
        return statementsCache.get(stmt);
    }

    /**
     * Put a PreparedStatement in the cache.
     * @param stmt the statement to cache.
     * @return the cached statement.
     */
    protected JdbcPreparedStatementHandle putCachedStatement(JdbcPreparedStatementHandle stmt) {
        return statementsCache.put(stmt);
    }

    /**
     * Register uncached statement so that it can be closed when the connection is put back in the pool.
     *
     * @param stmt the statement to register.
     * @return the registered statement.
     */
    protected Statement registerUncachedStatement(Statement stmt) {
        uncachedStatements.add(stmt);
        return stmt;
    }

    protected void unregisterUncachedStatement(Statement stmt) {
        uncachedStatements.remove(stmt);
    }

    public String toString() {
        return "a JdbcPooledConnection from datasource " + poolingDataSource.getUniqueName() + " in state " + Decoder.decodeXAStatefulHolderState(getState()) + " with usage count " + usageCount + " wrapping " + xaConnection;
    }

    private void applyCursorHoldabilty() throws SQLException {
        String cursorHoldability = getPoolingDataSource().getCursorHoldability();
        if (cursorHoldability != null) {
            int holdability = translateCursorHoldability(cursorHoldability);
            if (holdability < 0) {
                log.warn("invalid cursor holdability '" + cursorHoldability + "' configured, keeping the default cursor holdability.");
            }
            else {
                if (log.isDebugEnabled()) log.debug("setting connection's cursor holdability to " + cursorHoldability);
                connection.setHoldability(holdability);
            }
        }
    }

    private static int translateCursorHoldability(String cursorHoldability) {
        if ("CLOSE_CURSORS_AT_COMMIT".equals(cursorHoldability)) return ResultSet.CLOSE_CURSORS_AT_COMMIT;
        if ("HOLD_CURSORS_OVER_COMMIT".equals(cursorHoldability)) return ResultSet.HOLD_CURSORS_OVER_COMMIT;
        return -1;
    }


    private void applyLocalAutoCommit() throws SQLException {
        String localAutoCommit = getPoolingDataSource().getLocalAutoCommit();
        if (localAutoCommit != null) {
            if (localAutoCommit.equalsIgnoreCase("true")) {
                if (log.isDebugEnabled()) log.debug("setting connection's auto commit to true");
                connection.setAutoCommit(true);
            }
            else if (localAutoCommit.equalsIgnoreCase("false")) {
                if (log.isDebugEnabled()) log.debug("setting connection's auto commit to false");
                connection.setAutoCommit(false);
            }
            else {
                log.warn("invalid auto commit '" + localAutoCommit + "' configured, keeping default auto commit");
            }
        }
    }

    /* management */

    public String getStateDescription() {
        return Decoder.decodeXAStatefulHolderState(getState());
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public Date getLastReleaseDate() {
        return lastReleaseDate;
    }

    public Collection getTransactionGtridsCurrentlyHoldingThis() {
        return getXAResourceHolderStateGtrids();
    }

}
