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

import javax.transaction.HeuristicMixedException;
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
public class Phase2FailureTest extends TestCase {

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 10s
     * TX resolution: commit
     *
     * XAResource 1 resolution: successful
     * XAResource 2 resolution: commit throws exception XAException.XAER_RMERR, fixed after 2s
     *
     * Expected outcome:
     *   TM fails on resource 2 commit and retries twice (once per second) then commit should succeed.
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 4 XAResourceCommitEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, PREPARED, COMMITTING, COMMITTED
     * @throws Exception if any error happens.
     */
    public void test() throws Exception {
        PoolingDataSource poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection cconnection1 = poolingDataSource1.getConnection();
        cconnection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XAER_RMERR", XAException.XAER_RMERR));


        // after 2 secs we 'fix' the commit problem
        new Timer(true).schedule(new TimerTask() {
            public void run() {
                mockXAResource2.setCommitException(null);
            }
        }, 2000, 2000);

        tm.commit();

        System.out.println(EventRecorder.dumpToString());

        // we should find a COMMITTED status in the journal log
        // and 4 commit tries (1 for resource 1, 2 failed for resource 2 and 1 successful for resource 2)
        int journalCommittedEventCount = 0;
        int journalCommittingEventCount = 0;
        int commitEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceCommitEvent)
                commitEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTED)
                    journalCommittedEventCount++;
            }

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTING)
                    journalCommittingEventCount++;
            }
        }
        assertEquals("TM should have logged a COMMITTING status", 1, journalCommittingEventCount);
        assertEquals("TM should have logged a COMMITTED status", 1, journalCommittedEventCount);
        assertEquals("TM haven't properly tried to commit", 4, commitEventCount);

        poolingDataSource1.close();
        poolingDataSource2.close();
    }

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 1s
     * TX resolution: commit
     *
     * XAResource 1 resolution: successful
     * XAResource 2 resolution: commit throws exception XAException.XA_HEURCOM
     *
     * Expected outcome:
     *   TM fails on resource 2 commit because of heuristic commit. Since the decision is compatible
     *   with the TX outcome, the XID is forgotten on the resource and the TX should succeed.
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 2 XAResourceCommitEvent, 1 XAResourceForgetEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, PREPARED, COMMITTING, COMMITTED
     * @throws Exception if any error happens.
     */
    public void testHeuristicCommit() throws Exception {
        PoolingDataSource poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();
        tm.setTransactionTimeout(1); // TX timeout should have no effect here

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XA_HEURCOM", XAException.XA_HEURCOM));

        tm.commit();

        System.out.println(EventRecorder.dumpToString());

        // we should find a COMMITTED status in the journal log
        // 2 commit tries and 1 forget
        int journalCommittedEventCount = 0;
        int commitEventCount = 0;
        int forgetEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceCommitEvent)
                commitEventCount++;

            if (event instanceof XAResourceForgetEvent)
                forgetEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTED)
                    journalCommittedEventCount++;
            }
        }
        assertEquals("TM should have logged a COMMITTED status", 1, journalCommittedEventCount);
        assertEquals("TM haven't properly tried to commit", 2, commitEventCount);
        assertEquals("TM haven't properly tried to forget", 1, forgetEventCount);

        poolingDataSource1.close();
        poolingDataSource2.close();
    }

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 1s
     * TX resolution: commit
     *
     * XAResource 1 resolution: successful
     * XAResource 2 resolution: commit throws exception XAException.XA_HEURRB
     *
     * Expected outcome:
     *   TM fails on resource 2 commit because of heuristic rollback. Since the decision is not compatible
     *   with the TX outcome, the TX should fail.
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 2 XAResourceCommitEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, PREPARED, COMMITTING, COMMITTED
     * @throws Exception if any error happens.
     */
    public void testHeuristicMixed() throws Exception {
        PoolingDataSource poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();
        tm.setTransactionTimeout(1); // TX timeout should have no effect here

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        Connection connection2 = poolingDataSource2.getConnection();
        MockXAConnection mockXAConnection2 = (MockXAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(((JdbcConnectionHandle) connection2).getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) mockXAConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XA_HEURRB", XAException.XA_HEURRB));

        try {
            tm.commit();
            fail("TM should have thrown HeuristicMixedException");
        } catch (HeuristicMixedException ex) {
            assertEquals("1 resource(s) out of 2 improperly heuristically rolled back", ex.getMessage());
        }

        System.out.println(EventRecorder.dumpToString());

        // we should find a COMMITTED status in the journal log
        // and 4 rollback tries (1 for resource 2, 2 failed for resource 1 and 1 successful for resource 1)
        int journalCommittedEventCount = 0;
        int commitEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceCommitEvent)
                commitEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTED)
                    journalCommittedEventCount++;
            }
        }
        assertEquals("TM should have logged a COMMITTED status", 1, journalCommittedEventCount);
        assertEquals("TM haven't properly tried to commit", 2, commitEventCount);

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
