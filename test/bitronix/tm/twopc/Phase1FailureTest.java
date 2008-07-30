package bitronix.tm.twopc;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.AbstractMockJdbcTest;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.MockXAConnection;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.RollbackException;
import javax.transaction.xa.XAException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class Phase1FailureTest extends TestCase {
    private PoolingDataSource poolingDataSource1;
    private PoolingDataSource poolingDataSource2;
    private PoolingDataSource poolingDataSource3;
    private BitronixTransactionManager tm;

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
     *  2 XAResourcePrepareEvent, 1 XAResourceRollbackEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, UNKNOWN, ROLLING_BACK, UNKNOWN
     * @throws Exception if any error happens.
     */
    public void testPrepareFailureRollbackFailure() throws Exception {
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

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (SystemException ex) {
            assertEquals("transaction partially prepared and only partially rolled back. Some resources might be left in doubt !", ex.getMessage());

            assertEquals("collected 2 exception(s):" + System.getProperty("line.separator") +
                    " [pds2 - javax.transaction.xa.XAException(XAER_RMERR) - resource 2 prepare failed]" + System.getProperty("line.separator") +
                    " [pds1 - bitronix.tm.internal.BitronixXAException(XA_HEURHAZ) - resource reported XAER_INVAL when asked to rollback transaction branch]", ex.getCause().getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find a UNKNOWN status in the journal log
        // and 2 prepare tries (1 successful for resource 1, 1 failed for resource 2)
        // and 2 rollback tries (1 failed for resource 1, 1 successful for resource 2)
        int journalUnknownEventCount = 0;
        int prepareEventCount = 0;
        int rollbackEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceRollbackEvent)
                rollbackEventCount++;

            if (event instanceof XAResourcePrepareEvent)
                prepareEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_UNKNOWN)
                    journalUnknownEventCount++;
            }
        }
        assertEquals("TM should have journaled 2 UNKNOWN status", 1, journalUnknownEventCount);
        assertEquals("TM haven't properly tried to prepare", 2, prepareEventCount);
        assertEquals("TM haven't properly tried to rollback", 2, rollbackEventCount);
    }

    /**
     * Test scenario:
     *
     * XAResources: 3
     * TX timeout: 10s
     * TX resolution: rollback
     *
     * @throws Exception if any error happens.
     */
    public void testPrepareFailure() throws Exception {
        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        Connection connection3 = poolingDataSource2.getConnection();
        connection3.createStatement();

        MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setPrepareException(createXAException("resource 2 prepare failed", XAException.XAER_RMERR));

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (RollbackException ex) {
            assertTrue(ex.getMessage().matches("transaction failed to prepare: a Bitronix Transaction with GTRID (.*?) status=ROLLEDBACK, 3 resource\\(s\\) enlisted (.*?)"));

            assertTrue(ex.getCause().getMessage().matches("transaction failed during prepare of a Bitronix Transaction with GTRID (.*?), status=PREPARING, 3 resource\\(s\\) enlisted (.*?): resource\\(s\\) \\[pds2\\] threw unexpected exception"));

            assertEquals("collected 1 exception(s):" + System.getProperty("line.separator") +
                    " [pds2 - javax.transaction.xa.XAException(XAER_RMERR) - resource 2 prepare failed]", ex.getCause().getCause().getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find a ROLLEDBACK status in the journal log
        // and 3 prepare tries (1 successful for resources 1 and 3, 1 failed for resource 2)
        // and 3 rollback tries (1 successful for each resource)
        int journalRollbackEventCount = 0;
        int prepareEventCount = 0;
        int rollbackEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceRollbackEvent)
                rollbackEventCount++;

            if (event instanceof XAResourcePrepareEvent)
                prepareEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_ROLLEDBACK)
                    journalRollbackEventCount++;
            }
        }
        assertEquals("TM should have journaled 1 ROLLEDBACK status", 1, journalRollbackEventCount);
        assertEquals("TM haven't properly tried to prepare", 3, prepareEventCount);
        assertEquals("TM haven't properly tried to rollback", 3, rollbackEventCount);
    }

    protected void setUp() throws Exception {
        EventRecorder.clear();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journal");
        field.setAccessible(true);
        field.set(TransactionManagerServices.class, new MockJournal());

        poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setMinPoolSize(5);
        poolingDataSource1.setMaxPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setMinPoolSize(5);
        poolingDataSource2.setMaxPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        poolingDataSource3 = new PoolingDataSource();
        poolingDataSource3.setClassName(MockXADataSource.class.getName());
        poolingDataSource3.setUniqueName("pds3");
        poolingDataSource3.setMinPoolSize(5);
        poolingDataSource3.setMaxPoolSize(5);
        poolingDataSource3.setAutomaticEnlistingEnabled(true);
        poolingDataSource3.init();

        tm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        poolingDataSource1.close();
        poolingDataSource2.close();
        poolingDataSource3.close();
        tm.shutdown();
    }

    private XAException createXAException(String msg, int errorCode) {
        XAException prepareException = new XAException(msg);
        prepareException.errorCode = errorCode;
        return prepareException;
    }

}
