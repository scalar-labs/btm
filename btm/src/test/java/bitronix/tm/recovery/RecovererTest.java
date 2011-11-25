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
package bitronix.tm.recovery;

import java.io.File;
import java.lang.reflect.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.Status;
import javax.transaction.xa.*;

import junit.framework.TestCase;

import org.slf4j.*;

import bitronix.tm.*;
import bitronix.tm.internal.TransactionStatusChangeListener;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.*;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.jdbc.*;
import bitronix.tm.utils.*;

/**
 *
 * @author lorban
 */
public class RecovererTest extends TestCase {
    private final static Logger log = LoggerFactory.getLogger(RecovererTest.class);

    private MockXAResource xaResource;
    private PoolingDataSource pds;
    private Journal journal;


    protected void setUp() throws Exception {
        Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
        }

        pds = new PoolingDataSource();
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setUniqueName("mock-xads");
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(1);
        pds.init();

        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();

        Connection connection1 = pds.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        xaResource = (MockXAResource) handle.getPooledConnection().getXAResource();
        handle.close();

        // test the clustered recovery as its logic is more complex and covers the non-clustered logic
        TransactionManagerServices.getConfiguration().setCurrentNodeOnlyRecovery(true);

        // recoverer needs the journal to be open to be run manually
        journal = TransactionManagerServices.getJournal();
        journal.open();
    }


    protected void tearDown() throws Exception {
        if (TransactionManagerServices.isTransactionManagerRunning())
            TransactionManagerServices.getTransactionManager().shutdown();

        journal.close();
        pds.close();
        TransactionManagerServices.getJournal().close();
        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();
        EventRecorder.clear();
    }

    /**
     * Create 3 XIDs on the resource that are not in the journal -> recoverer presumes they have aborted and rolls
     * them back.
     * @throws Exception
     */
    public void testRecoverPresumedAbort() throws Exception {
        byte[] gtrid = UidGenerator.generateUid().getArray();

        xaResource.addInDoubtXid(new MockXid(0, gtrid, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(1, gtrid, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(2, gtrid, BitronixXid.FORMAT_ID));

        TransactionManagerServices.getRecoverer().run();

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(3, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    /**
     * Create 3 XIDs on the resource that are not in the journal -> recoverer presumes they have aborted and rolls
     * them back.
     * @throws Exception
     */
    public void testIncrementalRecoverPresumedAbort() throws Exception {
        byte[] gtrid = UidGenerator.generateUid().getArray();

        xaResource.addInDoubtXid(new MockXid(0, gtrid, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(1, gtrid, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(2, gtrid, BitronixXid.FORMAT_ID));

        IncrementalRecoverer.recover(pds);

        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    /**
     * Create 3 XIDs on the resource that are in the journal -> recoverer commits them.
     * @throws Exception
     */
    public void testRecoverCommitting() throws Exception {
        Xid xid0 = new MockXid(0, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);
        Xid xid1 = new MockXid(1, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid1);
        Xid xid2 = new MockXid(2, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid2);

        Set names = new HashSet();
        names.add(pds.getUniqueName());
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid1.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid2.getGlobalTransactionId()), names);
        TransactionManagerServices.getRecoverer().run();

        assertEquals(3, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    /**
     * Create 3 XIDs on the resource that are in the journal -> recoverer commits them.
     * @throws Exception
     */
    public void testIncrementalRecoverCommitting() throws Exception {
        Xid xid0 = new MockXid(0, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);
        Xid xid1 = new MockXid(1, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid1);
        Xid xid2 = new MockXid(2, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid2);

        Set names = new HashSet();
        names.add(pds.getUniqueName());
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid1.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid2.getGlobalTransactionId()), names);

        IncrementalRecoverer.recover(pds);

        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    public void testSkipInFlightRollback() throws Exception {
        BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();

        Uid uid0 = UidGenerator.generateUid();
        Xid xid0 = new MockXid(0, uid0.getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);

        assertNull(btm.getCurrentTransaction());
        Thread.sleep(30); // let the clock run a bit so that in-flight TX is a bit older than the journaled one
        btm.begin();

        Xid xid1 = new MockXid(1, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid1);

        TransactionManagerServices.getRecoverer().run();

        btm.rollback();

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(1, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(1, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);

        btm.shutdown();
        TransactionManagerServices.getJournal().open();
        TransactionManagerServices.getRecoverer().run();

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(1, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    public void testSkipInFlightCommit() throws Exception {
        BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();

        Uid uid0 = UidGenerator.generateUid();
        Xid xid0 = new MockXid(0, uid0.getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);
        Set names = new HashSet();
        names.add(pds.getUniqueName());
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);

        assertNull(btm.getCurrentTransaction());
        Thread.sleep(30); // let the clock run a bit so that in-flight TX is a bit older than the journaled one
        btm.begin();

        Xid xid1 = new MockXid(1, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid1);

        names = new HashSet();
        names.add(pds.getUniqueName());
        journal.log(Status.STATUS_COMMITTING, new Uid(xid1.getGlobalTransactionId()), names);

        TransactionManagerServices.getRecoverer().run();

        btm.rollback();

        assertEquals(1, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);

        btm.shutdown();
        TransactionManagerServices.getJournal().open();
        TransactionManagerServices.getRecoverer().run();

        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    public void testRecoverMissingResource() throws Exception {
        final Xid xid0 = new MockXid(0, UidGenerator.generateUid().getArray(), BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);

        Set names = new HashSet();
        names.add("no-such-registered-resource");
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        assertEquals(1, TransactionManagerServices.getJournal().collectDanglingRecords().size());

        // the TM must run the recoverer in this scenario
        TransactionManagerServices.getTransactionManager();

        assertEquals(1, TransactionManagerServices.getJournal().collectDanglingRecords().size());
        assertNull(TransactionManagerServices.getRecoverer().getCompletionException());
        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(1, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);


        // the TM is running, adding this resource will kick incremental recovery on it
        PoolingDataSource pds = new PoolingDataSource() {
            public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
                JdbcPooledConnection pc = (JdbcPooledConnection) super.createPooledConnection(xaFactory, bean);
                MockXAResource xaResource = (MockXAResource) pc.getXAResource();
                xaResource.addInDoubtXid(UidGenerator.generateXid(new Uid(xid0.getGlobalTransactionId())));
                return pc;
            }
        };
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setUniqueName("no-such-registered-resource");
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(1);
        pds.init();

        Connection connection = pds.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection);
        XAResource xaResource = handle.getPooledConnection().getXAResource();
        handle.close();

        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
        assertEquals(0, TransactionManagerServices.getJournal().collectDanglingRecords().size());

        pds.close();

        TransactionManagerServices.getTransactionManager().shutdown();
    }

    volatile boolean listenerExecuted = false;
    public void testBackgroundRecovererSkippingInFlightTransactions() throws Exception {
        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journalRef");
        field.setAccessible(true);
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());

        pds.setMaxPoolSize(2);
        BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
        final Recoverer recoverer = TransactionManagerServices.getRecoverer();

        try {
            btm.begin();

            BitronixTransaction tx = btm.getCurrentTransaction();
            tx.addTransactionStatusChangeListener(new TransactionStatusChangeListener() {
                public void statusChanged(int oldStatus, int newStatus) {
                    if (newStatus != Status.STATUS_COMMITTING)
                        return;

                    recoverer.run();
                    assertEquals(0, recoverer.getCommittedCount());
                    assertEquals(0, recoverer.getRolledbackCount());
                    assertNull(recoverer.getCompletionException());
                    listenerExecuted = true;
                }
            });

            Connection c = pds.getConnection();
            c.createStatement();
            c.close();
    
            xaResource.addInDoubtXid(new MockXid(new byte[] {0, 1, 2}, tx.getResourceManager().getGtrid().getArray(), BitronixXid.FORMAT_ID));

            btm.commit();
        }
        finally {
            btm.shutdown();
        }

        assertTrue("recoverer did not run between phases 1 and 2", listenerExecuted);

        int committedCount = 0;

        List events = EventRecorder.getOrderedEvents();
        for (int i = 0; i < events.size(); i++) {
            Event event = (Event) events.get(i);
            if (event instanceof JournalLogEvent) {
                if (((JournalLogEvent) event).getStatus() == Status.STATUS_COMMITTED)
                    committedCount++;
            }
        }

        assertEquals("TX has been committed more or less times than just once", 1, committedCount);
    }


    public void testReentrance() throws Exception {
        log.debug("Start test RecovererTest.testReentrance()");
        final int THREAD_COUNT = 10;
        Recoverer recoverer = new Recoverer();
        xaResource.setRecoveryDelay(1000);

        List threads = new ArrayList();

        //create
        for (int i=0; i< THREAD_COUNT;i++) {
            Thread t = new Thread(recoverer);
            threads.add(t);
        }

        //start
        for (int i=0; i< THREAD_COUNT;i++) {
            Thread t = (Thread) threads.get(i);
            t.start();
        }

        //join
        for (int i=0; i< THREAD_COUNT;i++) {
            Thread t = (Thread) threads.get(i);
            t.join();
        }

        assertEquals(1, recoverer.getExecutionsCount());
    }
    
}
