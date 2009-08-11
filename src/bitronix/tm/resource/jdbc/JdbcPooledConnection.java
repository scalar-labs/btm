package bitronix.tm.resource.jdbc;

import bitronix.tm.utils.*;
import bitronix.tm.internal.*;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Implementation of a JDBC pooled connection wrapping vendor's {@link XAConnection} implementation.
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class JdbcPooledConnection extends AbstractXAResourceHolder implements StateChangeListener, JdbcPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JdbcPooledConnection.class);

    private XAConnection xaConnection;
    private Connection connection;
    private XAResource xaResource;
    private PoolingDataSource poolingDataSource;
    private LruMap statementsCache;
    private List uncachedStatements = new ArrayList();

    /* management */
    private String jmxName;
    private Date acquisitionDate;
    private Date lastReleaseDate;


    public JdbcPooledConnection(PoolingDataSource poolingDataSource, XAConnection xaConnection) throws SQLException {
        this.poolingDataSource = poolingDataSource;
        this.xaConnection = xaConnection;
        this.xaResource = xaConnection.getXAResource();
        this.statementsCache = new LruMap(poolingDataSource.getPreparedStatementCacheSize());
        statementsCache.addEvictionListener(new LruEvictionListener() {
            public void onEviction(Object value) {
                PreparedStatement stmt = (PreparedStatement) value;
                try {
                    if (log.isDebugEnabled()) log.debug("closing evicted statement " + stmt);
                    stmt.close();
                } catch (SQLException ex) {
                    log.warn("error closing evicted statement", ex);
                }
            }
        });
        connection = xaConnection.getConnection();
        addStateChangeEventListener(this);

        if (poolingDataSource.getClassName().equals(LrcXADataSource.class.getName())) {
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing twoPcOrderingPosition to " + Scheduler.ALWAYS_LAST_POSITION);
            poolingDataSource.setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing deferConnectionRelease to true");
            poolingDataSource.setDeferConnectionRelease(true);
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName() + " - changing useTmJoin to true");
            poolingDataSource.setUseTmJoin(true);
        }

        this.jmxName = "bitronix.tm:type=JdbcPooledConnection,UniqueName=" + ManagementRegistrar.makeValidName(poolingDataSource.getUniqueName()) + ",Id=" + poolingDataSource.incCreatedResourcesCounter();
        ManagementRegistrar.register(jmxName, this);
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
        setState(STATE_CLOSED);

        // cleanup of pooled resources
        Iterator it = statementsCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            PreparedStatement stmt = (PreparedStatement) entry.getValue();
            stmt.close();
        }

        ManagementRegistrar.unregister(jmxName);

        connection.close();
        xaConnection.close();
    }

    public RecoveryXAResourceHolder createRecoveryXAResourceHolder() {
        return new RecoveryXAResourceHolder(this);
    }

    private void testConnection(Connection connection) throws SQLException {
        String query = poolingDataSource.getTestQuery();
        if (query == null) {
            if (log.isDebugEnabled()) log.debug("no query to test connection of " + this + ", skipping test");
            return;
        }

        if (log.isDebugEnabled()) log.debug("testing with query '" + query + "' connection of " + this);
        PreparedStatement stmt = connection.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        rs.close();
        stmt.close();
        if (log.isDebugEnabled()) log.debug("successfully tested connection of " + this);
    }

    protected void release() throws SQLException {
        if (log.isDebugEnabled()) log.debug("releasing to pool " + this);

        //TODO: even if delisting fails, requeuing should be done or we'll have a connection leak here

        // delisting
        try {
            TransactionContextHelper.delistFromCurrentTransaction(this, poolingDataSource);
        } catch (BitronixRollbackSystemException ex) {
            throw (SQLException) new SQLException("unilateral rollback of " + this).initCause(ex);
        } catch (SystemException ex) {
            throw (SQLException) new SQLException("error delisting " + this).initCause(ex);
        }

        // requeuing
        try {
            TransactionContextHelper.requeue(this, poolingDataSource);
        } catch (BitronixSystemException ex) {
            throw (SQLException) new SQLException("error requeueing " + this).initCause(ex);
        }

        if (log.isDebugEnabled()) log.debug("released to pool " + this);
    }

    public XAResource getXAResource() {
        return xaResource;
    }

    public PoolingDataSource getPoolingDataSource() {
        return poolingDataSource;
    }

    public List getXAResourceHolders() {
        List xaResourceHolders = new ArrayList();
        xaResourceHolders.add(this);
        return xaResourceHolders;
    }

    public Object getConnectionHandle() throws Exception {
        if (log.isDebugEnabled()) log.debug("getting connection handle from " + this);
        int oldState = getState();
        setState(STATE_ACCESSIBLE);
        if (oldState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("connection " + xaConnection + " was in state IN_POOL, testing it");
            testConnection(connection);
            applyIsolationLevel();
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
            lastReleaseDate = new Date();
        }
        if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
            acquisitionDate = new Date();
        }
        if (oldState == STATE_NOT_ACCESSIBLE && newState == STATE_ACCESSIBLE) {
            TransactionContextHelper.recycle(this);
        }
    }

    public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
        if (futureState == STATE_IN_POOL) {
            // close all uncached statements
            if (log.isDebugEnabled()) log.debug("closing " + uncachedStatements.size() + " uncached statement(s)");
            for (int i = 0; i < uncachedStatements.size(); i++) {
                Statement statement = (Statement) uncachedStatements.get(i);
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
     * @param sql the key that has been used to cache the statement.
     * @return the cached statement corresponding to the key or null if no statement is cached under that key.
     */
    protected PreparedStatement getCachedStatement(String sql) {
        PreparedStatement stmt = (PreparedStatement) statementsCache.get(sql);
        if (log.isDebugEnabled()) log.debug("statement cache lookup of <" + sql + "> in " + this + ": " + stmt);
        return stmt;
    }

    /**
     * Put a PreparedStatement in the cache.
     * @param sql the key that is used to cache the statement.
     * @param stmt the statement to cache.
     * @return the cached statement.
     */
    protected PreparedStatement putCachedStatement(String sql, PreparedStatement stmt) {
        if (log.isDebugEnabled()) log.debug("caching statement <" + sql + "> in " + this);
        return (PreparedStatement) statementsCache.put(sql, stmt);
    }

    /**
     * Register uncached statement so that it can be closed when the connection is put back in the pool.
     * @param stmt the statement to register.
     * @return the registered statement.
     */
    protected Statement registerUncachedStatement(Statement stmt) {
        uncachedStatements.add(stmt);
        return stmt;
    }

    public String toString() {
        return "a JdbcPooledConnection from datasource " + poolingDataSource.getUniqueName() + " in state " + Decoder.decodeXAStatefulHolderState(getState()) + " wrapping " + xaConnection;
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

    public String getTransactionGtridCurrentlyHoldingThis() {
        return getXAResourceHolderState().getXid().getGlobalTransactionIdUid().toString();
    }

}
