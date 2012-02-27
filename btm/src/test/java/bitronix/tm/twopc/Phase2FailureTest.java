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
package bitronix.tm.twopc;

import java.lang.reflect.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.XAException;

import bitronix.tm.journal.Journal;
import junit.framework.TestCase;
import bitronix.tm.*;
import bitronix.tm.mock.AbstractMockJdbcTest;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.*;
import bitronix.tm.mock.resource.jdbc.*;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lorban
 */
public class Phase2FailureTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(Phase2FailureTest.class);

    private PoolingDataSource poolingDataSource1;
    private PoolingDataSource poolingDataSource2;
    private BitronixTransactionManager tm;

    /**
     * Test scenario:
     *
     * XAResources: 2
     * TX timeout: 10s
     * TX resolution: heuristic mixed
     *
     * XAResource 1 resolution: successful
     * XAResource 2 resolution: commit throws exception XAException.XAER_RMERR
     *
     * Expected outcome:
     *   TM fails on resource 2 commit but does not report that via an exception
     *   as the recoverer will clean that up
     * Expected TM events:
     *  2 XAResourcePrepareEvent, 2 XAResourceCommitEvent
     * Expected journal events:
     *   ACTIVE, PREPARING, PREPARED, COMMITTING, COMMITTED
     * @throws Exception if any error happens.
     */
    public void testExpectNoHeuristic() throws Exception {
        tm.begin();
        tm.setTransactionTimeout(10); // TX must not timeout

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XAER_RMERR", XAException.XAER_RMERR));

        tm.commit();

        log.info(EventRecorder.dumpToString());

        int journalUnknownEventCount = 0;
        int journalCommittingEventCount = 0;
        int journalCommittedEventCount = 0;
        int commitEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceCommitEvent)
                commitEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_UNKNOWN)
                    journalUnknownEventCount++;
            }

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTING)
                    journalCommittingEventCount++;
            }

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTED)
                    journalCommittedEventCount++;
            }
        }
        assertEquals("TM should have logged a COMMITTING status", 1, journalCommittingEventCount);
        assertEquals("TM should have logged a COMMITTED status", 1, journalCommittedEventCount);
        assertEquals("TM should not have logged ant UNKNOWN status", 0, journalUnknownEventCount);
        assertEquals("TM haven't properly tried to commit", 2, commitEventCount);
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
        tm.begin();
        tm.setTransactionTimeout(1); // TX timeout should have no effect here

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XA_HEURCOM", XAException.XA_HEURCOM));

        tm.commit();

        log.info(EventRecorder.dumpToString());

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
        tm.begin();
        tm.setTransactionTimeout(1); // TX timeout should have no effect here

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection2.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XA_HEURRB", XAException.XA_HEURRB));

        try {
            tm.commit();
            fail("TM should have thrown HeuristicMixedException");
        } catch (HeuristicMixedException ex) {
            assertEquals("transaction failed during commit of a Bitronix Transaction with GTRID [", ex.getMessage().substring(0, 71));
            int idx = ex.getMessage().indexOf(']');
            assertEquals("], status=UNKNOWN, 2 resource(s) enlisted (started ", ex.getMessage().substring(idx, idx + 51));
            assertTrue("got message <" + ex.getMessage() + ">", ex.getMessage().endsWith("resource(s) [pds2] improperly unilaterally rolled back"));
        }

        log.info(EventRecorder.dumpToString());

        int journalUnknownEventCount = 0;
        int commitEventCount = 0;
        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);

            if (event instanceof XAResourceCommitEvent)
                commitEventCount++;

            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_UNKNOWN)
                    journalUnknownEventCount++;
            }
        }
        assertEquals("TM should have logged a UNKNOWN status", 1, journalUnknownEventCount);
        assertEquals("TM haven't properly tried to commit", 2, commitEventCount);
    }

    protected void setUp() throws Exception {
        Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
        }

        EventRecorder.clear();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journalRef");
        field.setAccessible(true);
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());


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

        tm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        poolingDataSource1.close();
        poolingDataSource2.close();
        tm.shutdown();
    }

    private XAException createXAException(String msg, int errorCode) {
        XAException prepareException = new XAException(msg);
        prepareException.errorCode = errorCode;
        return prepareException;
    }

}
