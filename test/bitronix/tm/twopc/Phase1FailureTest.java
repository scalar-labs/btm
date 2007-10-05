package bitronix.tm.twopc;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRollbackException;
import bitronix.tm.mock.AbstractMockJdbcTest;
import bitronix.tm.mock.events.Event;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;
import bitronix.tm.mock.events.XAResourceRollbackEvent;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.MockXAConnection;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class Phase1FailureTest extends TestCase {

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 10s
     * TX resolution: rollback
     *
     * XAResource 1 resolution: rollback throws exception XAException.XAER_INVAL, exception fixed after 2s
     * XAResource 2 resolution: prepare throws exception XAException.XAER_RMERR
     *
     * Expected outcome:
     *   TM fails on resource 2 prepare and throws RollbackException. On call to rollback, resource 2 fails to rollback
     *   and is retried twice (once per second) then rollback should succeed.
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 4 XAResourceRollbackEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, ROLLING_BACK, ROLLEDBACK
     * @throws Exception if any error happens.
     */
    public void test() throws Exception {
        PoolingDataSource poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setMinPoolSize(5);
        poolingDataSource1.setMaxPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setMinPoolSize(5);
        poolingDataSource2.setMaxPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection connection1 = poolingDataSource1.getConnection();
        MockXAConnection mockXAConnection1 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection1).getPooledConnection());
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource1 = (MockXAResource) mockXAConnection1.getXAResource();
        mockXAResource1.setRollbackException(createXAException("resource 1 rollback failed", XAException.XAER_INVAL));

        MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setPrepareException(createXAException("resource 2 prepare failed", XAException.XAER_RMERR));

            // after 2 secs we 'fix' the rollback problem
        new Timer(true).schedule(new TimerTask() {
            public void run() {
                mockXAResource1.setRollbackException(null);
            }
        }, 2000, 2000);

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (BitronixRollbackException ex) {
            assertEquals("transaction failed during prepare, error=XAER_RMERR", ex.getMessage());
            assertEquals("resource 2 prepare failed", ex.getCause().getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find a ROLLEDBACK status in the journal log
        // and 4 rollback tries (1 successful for resource 2, 2 failed for resource 1 and 1 successful for resource 1)
        int journalRollbackEventCount = 0;
        int rollbackEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceRollbackEvent)
                rollbackEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_ROLLEDBACK)
                    journalRollbackEventCount++;
            }
        }
        assertEquals("TM should have logged a ROLLEDBACK status", 1, journalRollbackEventCount);
        assertEquals("TM haven't properly tried to rollback", 4, rollbackEventCount);

        poolingDataSource1.close();
        poolingDataSource2.close();
    }

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 1s
     * TX resolution: in-doubt at rollback
     *
     * XAResource 1 resolution: rollback throws exception XAException.XAER_INVAL
     * XAResource 2 resolution: prepare throws exception XAException.XAER_RMERR
     *
     * Expected outcome:
     *   TM fails on resource 2 prepare and throws RollbackException. On call to rollback, resource 2 fails to rollback
     *   and is retried until TX times out.
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 2+ XAResourceRollbackEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, ROLLING_BACK
     * @throws Exception if any error happens.
     */
    public void testTimeout() throws Exception {
        PoolingDataSource poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setMinPoolSize(5);
        poolingDataSource1.setMaxPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setMinPoolSize(5);
        poolingDataSource2.setMaxPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.setTransactionTimeout(1); // TX must timeout
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        MockXAConnection mockXAConnection1 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection1).getPooledConnection());
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource1 = (MockXAResource) mockXAConnection1.getXAResource();
        mockXAResource1.setRollbackException(createXAException("resource 1 rollback failed", XAException.XAER_INVAL));

        MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setPrepareException(createXAException("resource 2 prepare failed", XAException.XAER_RMERR));

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (RollbackException ex) {
            assertEquals("transaction failed during prepare, error=XAER_RMERR", ex.getMessage());
            ex.printStackTrace(System.out);
            assertEquals("resource 2 prepare failed", ex.getCause().getMessage());
        } finally {
            System.out.println(EventRecorder.dumpToString());
        }

        // we should find a ROLLINGBACK but not ROLLEDBACK status in the journal log
        // and 4 rollback tries (1 successful for resource 2, 2 failed for resource 1 and 1 successful for resource 1)
        int journalRollbackEventCount = 0;
        int rollbackEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceRollbackEvent)
                rollbackEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_ROLLEDBACK)
                    journalRollbackEventCount++;
            }
        }
        assertEquals("TM should not have logged a ROLLEDBACK status", 0, journalRollbackEventCount);
        assertTrue("TM haven't properly tried to rollback", rollbackEventCount > 1);

        poolingDataSource1.close();
        poolingDataSource2.close();
    }

    protected void setUp() throws Exception {
        EventRecorder.clear();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journal");
        field.setAccessible(true);
        field.set(TransactionManagerServices.class, new MockJournal());

        // change transactionRetryInterval to 1 second
        field = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        field.setAccessible(true);
        field.set(TransactionManagerServices.getConfiguration(), new Integer(1));
    }

    private XAException createXAException(String msg, int errorCode) {
        XAException prepareException = new XAException(msg);
        prepareException.errorCode = errorCode;
        return prepareException;
    }

}
