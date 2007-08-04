package bitronix.tm.resource.jdbc;

import bitronix.tm.internal.Decoder;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.ManagementRegistrar;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import bitronix.tm.BitronixXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Implementation of a JDBC {@link PooledConnection} wrapping vendor's {@link XAConnection} implementation.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class JdbcPooledConnection extends AbstractXAResourceHolder implements PooledConnection, StateChangeListener, JdbcPooledConnectionMBean {

    private final static Logger log = LoggerFactory.getLogger(JdbcPooledConnection.class);

    private List connectionEventListeners = new ArrayList();
    private XAConnection xaConnection;
    private XAResource xaResource;
    private PoolingDataSource poolingDataSource;
    private boolean emulateXa = false;

    protected Connection connection;

    /* management */
    private String jmxName;
    private Date acquisitionDate;


    public JdbcPooledConnection(PoolingDataSource poolingDataSource, XAConnection xaConnection) throws SQLException {
        this.poolingDataSource = poolingDataSource;
        this.xaConnection = xaConnection;
        this.xaResource = xaConnection.getXAResource();
        addStateChangeEventListener(this);

        if (poolingDataSource.getClassName().equals(LrcXADataSource.class.getName())) {
            if (log.isDebugEnabled()) log.debug("emulating XA for resource " + poolingDataSource.getUniqueName());
            emulateXa = true;
        }

        this.jmxName = "bitronix.tm:type=JdbcPooledConnection,UniqueName=" + poolingDataSource.getUniqueName() + ",Id=" + poolingDataSource.incCreatedResourcesCounter();
        ManagementRegistrar.register(jmxName, this);
    }

    public void close() throws SQLException {
        setState(STATE_CLOSED);
        xaConnection.close();
    }

    public RecoveryXAResourceHolder createRecoveryXAResourceHolder() {
        return new RecoveryXAResourceHolder(this);
    }

    public Connection getConnection() throws SQLException {
        if (log.isDebugEnabled()) log.debug("getting connection handle from " + this);
        setState(STATE_ACCESSIBLE);
        connection = xaConnection.getConnection();
        testConnection(connection);
        if (log.isDebugEnabled()) log.debug("got connection handle from " + this);
        return new JdbcConnectionHandle(this, connection);
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
        if (!connection.getAutoCommit())
            connection.rollback();
        if (log.isDebugEnabled()) log.debug("successfully tested connection of " + this);
    }

    protected void release() throws SQLException {
        if (log.isDebugEnabled()) log.debug("releasing to pool " + this);

        // delisting
        try {
            TransactionContextHelper.delistFromCurrentTransaction(this, poolingDataSource);
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

    public boolean isEmulatingXA() {
        return emulateXa;
    }

    /**
     * If this method returns false, then local transaction calls like Connection.commit() can be made.
     * @return true if start() has been successfully called but not end() yet <i>and</i> the transaction is not suspended.
     * @throws java.sql.SQLException
     */
    public boolean isParticipatingInActiveGlobalTransaction() throws SQLException {
        XAResourceHolderState xaResourceHolderState = getXAResourceHolderState();
        if (xaResourceHolderState == null)
            return false;
        return (xaResourceHolderState.isStarted()) && (!xaResourceHolderState.isSuspended()) && (!xaResourceHolderState.isEnded());
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.add(listener);
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        connectionEventListeners.remove(listener);
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
        return getConnection();
    }

    public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
        if (newState == STATE_IN_POOL) {
            if (log.isDebugEnabled()) log.debug("requeued JDBC connection of " + poolingDataSource);
            fireCloseEvent();
        }
        if (oldState == STATE_IN_POOL && newState == STATE_ACCESSIBLE) {
            acquisitionDate = new Date();
        }
        if (newState == STATE_CLOSED) {
            ManagementRegistrar.unregister(jmxName);
        }
    }

    public String toString() {
        return "a JdbcPooledConnection from datasource " + poolingDataSource.getUniqueName() + " in state " + Decoder.decodeXAStatefulHolderState(getState()) + " wrapping " + xaConnection;
    }

    private void fireCloseEvent() {
        if (log.isDebugEnabled()) log.debug("notifying " + connectionEventListeners.size() + " connectionEventListener(s) about closing of " + this);
        ConnectionEvent event = new ConnectionEvent(this);
        for (int i = 0; i < connectionEventListeners.size(); i++) {
            ConnectionEventListener connectionEventListener = (ConnectionEventListener) connectionEventListeners.get(i);
            connectionEventListener.connectionClosed(event);
        }
    }

    /* management */

    public String getStateDescription() {
        return Decoder.decodeXAStatefulHolderState(state);
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public String getTransactionGtridCurrentlyHoldingThis() {
        return ((BitronixXid) getXAResourceHolderState().getXid()).getGlobalTransactionIdUid().toString();
    }

}