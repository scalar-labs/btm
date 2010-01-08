package bitronix.tm.mock;

import java.lang.reflect.Field;
import java.util.*;

import junit.framework.TestCase;

import org.slf4j.*;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jdbc.*;

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
        Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
        }

        // DataSource1 has shared accessible connections
        poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource1.setUniqueName(DATASOURCE1_NAME);
        poolingDataSource1.setMinPoolSize(POOL_SIZE);
        poolingDataSource1.setMaxPoolSize(POOL_SIZE);
        poolingDataSource1.setAllowLocalTransactions(true);
        poolingDataSource1.init();

        // DataSource2 does not have shared accessible connections
        poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource2.setUniqueName(DATASOURCE2_NAME);
        poolingDataSource2.setMinPoolSize(POOL_SIZE);
        poolingDataSource2.setMaxPoolSize(POOL_SIZE);
        poolingDataSource2.setAllowLocalTransactions(true);
        poolingDataSource2.init();

        // change disk journal into mock journal
        Field journalField = TransactionManagerServices.class.getDeclaredField("journal");
        journalField.setAccessible(true);
        journalField.set(TransactionManagerServices.class, new MockJournal());

        // change connection pools into mock pools
        XAPool p1 = getPool(this.poolingDataSource1);
        registerPoolEventListener(p1);
        XAPool p2 = getPool(this.poolingDataSource2);
        registerPoolEventListener(p2);

        TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(2);

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

                public void stateChanging(XAStatefulHolder source, int currentState, int futureState) {
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

        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public static Object getWrappedXAConnectionOf(Object pc1) throws NoSuchFieldException, IllegalAccessException {
        Field f = pc1.getClass().getDeclaredField("xaConnection");
        f.setAccessible(true);
        return f.get(pc1);
    }
}
