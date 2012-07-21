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

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.AbstractMockJdbcTest;
import bitronix.tm.mock.events.Event;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;
import bitronix.tm.mock.events.XAResourceCommitEvent;
import bitronix.tm.mock.events.XAResourceForgetEvent;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;
import oracle.jdbc.xa.OracleXAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author lorban
 */
public class OnePcFailureTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(OnePcFailureTest.class);

    private PoolingDataSource poolingDataSource1;
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
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        connection1.createStatement();

        final MockXAResource mockXAResource2 = (MockXAResource) xaConnection2.getXAResource();
        mockXAResource2.setCommitException(createXAException("resource 2 commit failed with XAER_RMERR", XAException.XAER_RMERR));

        try {
            tm.commit();
            fail("expected RollbackException");
        } catch (RollbackException ex) {
            assertTrue(ex.getMessage().matches("transaction failed during 1PC commit of a Bitronix Transaction with GTRID \\[.*\\], status=ROLLEDBACK, 1 resource\\(s\\) enlisted \\(.*\\)"));
        }

        log.info(EventRecorder.dumpToString());

        int journalUnknownEventCount = 0;
        int journalCommittingEventCount = 0;
        int journalRolledbackEventCount = 0;
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
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_ROLLEDBACK)
                    journalRolledbackEventCount++;
            }
        }
        assertEquals("TM should have logged a COMMITTING status", 1, journalCommittingEventCount);
        assertEquals("TM should have logged a ROLLEDBACK status", 1, journalRolledbackEventCount);
        assertEquals("TM should not have logged ant UNKNOWN status", 0, journalUnknownEventCount);
        assertEquals("TM haven't properly tried to commit", 1, commitEventCount);
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

        tm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        poolingDataSource1.close();
        tm.shutdown();
    }

    private XAException createXAException(String msg, int errorCode) {
        XAException prepareException = new OracleXAException(msg, 9876);
        prepareException.errorCode = errorCode;
        return prepareException;
    }

}
