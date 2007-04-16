package bitronix.tm.mock;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.events.ConnectionDequeuedEvent;
import bitronix.tm.mock.events.ConnectionQueuedEvent;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.*;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.common.*;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 *
 */
public abstract class AbstractMockJdbcTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(AbstractMockJdbcTest.class);

    protected PoolingDataSource poolingDataSource1;
    protected PoolingDataSource poolingDataSource2;
    protected static final int POOL_SIZE = 5;
    protected static final String DATASOURCE1_NAME = "pds1";
    protected static final String DATASOURCE2_NAME = "pds2";

    protected void setUp() throws Exception {
        DataSourceBean dsb1 = new DataSourceBean();
        dsb1.setClassName(MockXADataSource.class.getName());
        dsb1.setUniqueName(DATASOURCE1_NAME);
        dsb1.setPoolSize(POOL_SIZE);
        dsb1.setAllowLocalTransactions(true);
        poolingDataSource1 = (PoolingDataSource) dsb1.createResource();

        DataSourceBean dsb2 = new DataSourceBean();
        dsb2.setClassName(MockXADataSource.class.getName());
        dsb2.setUniqueName(DATASOURCE2_NAME);
        dsb2.setPoolSize(POOL_SIZE);
        dsb2.setAllowLocalTransactions(true);
        poolingDataSource2 = (PoolingDataSource) dsb2.createResource();

        // change disk journal into mock journal
        Field journalField = TransactionManagerServices.class.getDeclaredField("journal");
        journalField.setAccessible(true);
        journalField.set(TransactionManagerServices.class, new MockJournal());

        // change connection pools into mock pools
        XAPool p1 = getPool(poolingDataSource1);
        registerPoolEventListener(p1);
        XAPool p2 = getPool(poolingDataSource2);
        registerPoolEventListener(p2);

        // change transactionRetryInterval to 1 second
        Field transactionRetryIntervalField = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        transactionRetryIntervalField.setAccessible(true);
        transactionRetryIntervalField.set(TransactionManagerServices.getConfiguration(), new Integer(1));

        // start TM
        TransactionManagerServices.getTransactionManager();

        // clear event recorder list
        EventRecorder.clear();
    }

    protected XAPool getPool(PoolingDataSource poolingDataSource) throws NoSuchFieldException, IllegalAccessException {
        Field poolField = PoolingDataSource.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        return (XAPool) poolField.get(poolingDataSource);
    }

    private void registerPoolEventListener(XAPool pool) throws Exception {
        ArrayList connections = new ArrayList();

        while (pool.inPoolSize() > 0) {
            JdbcConnectionHandle connectionHandle = (JdbcConnectionHandle) pool.getConnectionHandle();
            JdbcPooledConnection jdbcPooledConnection = connectionHandle.getPooledConnection();
            connections.add(connectionHandle);
            jdbcPooledConnection.addStateChangeEventListener(new StateChangeListener() {
                public void stateChanged(XAStatefulHolder source, int oldState, int newState) {
                    if (newState == AbstractXAResourceHolder.STATE_IN_POOL)
                        EventRecorder.getEventRecorder(this).addEvent(new ConnectionQueuedEvent(this, (JdbcPooledConnection) source));
                    if (newState == AbstractXAResourceHolder.STATE_ACCESSIBLE)
                        EventRecorder.getEventRecorder(this).addEvent(new ConnectionDequeuedEvent(this, (JdbcPooledConnection) source));
                }
            });
        }

        for (int i = 0; i < connections.size(); i++) {
            JdbcConnectionHandle connectionHandle = (JdbcConnectionHandle) connections.get(i);
            connectionHandle.close();
        }
    }

    protected void tearDown() throws Exception {
        try {
            if (log.isDebugEnabled()) log.debug("*** tearDown rollback");
            TransactionManagerServices.getTransactionManager().rollback();
        } catch (Exception ex) {
            // ignore
        }
        poolingDataSource1.close();
        poolingDataSource2.close();
    }

    public static Object getWrappedXAConnectionOf(JdbcPooledConnection pc1) throws NoSuchFieldException, IllegalAccessException {
        Field f = pc1.getClass().getDeclaredField("xaConnection");
        f.setAccessible(true);
        return f.get(pc1);
    }

}
