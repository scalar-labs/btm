package bitronix.tm.twopc;

import java.lang.reflect.*;
import java.sql.Connection;
import java.util.List;

import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.XAException;

import junit.framework.TestCase;
import bitronix.tm.*;
import bitronix.tm.mock.AbstractMockJdbcTest;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.*;
import bitronix.tm.mock.resource.jdbc.*;
import bitronix.tm.resource.jdbc.*;

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
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection1 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle2.getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource1 = (MockXAResource) xaConnection1.getXAResource();
        mockXAResource1.setRollbackException(createXAException("resource 1 rollback failed", XAException.XAER_INVAL));

        MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setPrepareException(createXAException("resource 2 prepare failed", XAException.XAER_RMERR));

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (RollbackException ex) {
            assertTrue(ex.getMessage().matches("transaction failed to prepare: a Bitronix Transaction with GTRID \\[.*\\], status=ROLLEDBACK, 2 resource\\(s\\) enlisted .*"));
            assertEquals("collected 1 exception(s):" + System.getProperty("line.separator") +
                    " [pds2 - javax.transaction.xa.XAException(XAER_RMERR) - resource 2 prepare failed]", ex.getCause().getCause().getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find in the journal log:
        //  2 prepare tries (1 successful for resource 1, 1 failed for resource 2)
        //  2 rollback tries (1 failed for resource 1, 1 successful for resource 2)
        // the rollabck error on resource 1 should not be reported to the code as it is the job
        // of the recovery engine to clean it up and eventually report the heuristic
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
        assertEquals("TM should have journaled 0 UNKNOWN status", 0, journalUnknownEventCount);
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
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        Connection connection3 = poolingDataSource2.getConnection();
        connection3.createStatement();

        MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
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

    public void testUnilateralRollbackOnCommitDelist() throws Exception {
        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setEndException(createXAException("resource 2 end failed", XAException.XAER_NOTA));

        try {
            tm.commit();
            fail("TM should have thrown an exception");
        } catch (RollbackException ex) {
            assertEquals("unilateral resource rollback caused transaction rollback", ex.getMessage());

            assertEquals("resource(s) [pds2] unilaterally rolled back", ex.getCause().getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find a ROLLEDBACK status in the journal log
        // and 0 prepare try
        // and 2 rollback tries (1 for each resource)
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
        assertEquals("TM has tried to prepare", 0, prepareEventCount);
        assertEquals("TM haven't properly tried to rollback", 2, rollbackEventCount);
    }

    public void testUnilateralRollbackOnRollbackDelist() throws Exception {
        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setEndException(createXAException("resource 2 end failed", XAException.XAER_NOTA));

        tm.rollback();

        System.out.println(EventRecorder.dumpToString());

        // we should find a ROLLEDBACK status in the journal log
        // and 0 prepare try
        // and 2 rollback tries (1 for each resource)
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
        assertEquals("TM has tried to prepare", 0, prepareEventCount);
        assertEquals("TM haven't properly tried to rollback", 2, rollbackEventCount);
    }

    protected void setUp() throws Exception {
        EventRecorder.clear();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journal");
        field.setAccessible(true);
        field.set(TransactionManagerServices.class, new MockJournal());

        poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setMinPoolSize(5);
        poolingDataSource1.setMaxPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setMinPoolSize(5);
        poolingDataSource2.setMaxPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        poolingDataSource3 = new PoolingDataSource();
        poolingDataSource3.setClassName(MockitoXADataSource.class.getName());
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
