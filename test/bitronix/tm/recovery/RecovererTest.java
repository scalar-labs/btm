package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransaction;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.internal.TransactionStatusChangeListener;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.Event;
import bitronix.tm.mock.events.JournalLogEvent;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import junit.framework.TestCase;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.sql.Connection;
import java.lang.reflect.Field;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class RecovererTest extends TestCase {

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
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("mock-xads");
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(1);
        pds.init();

        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();

        JdbcConnectionHandle handle = (JdbcConnectionHandle) pds.getConnection();
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

        TransactionManagerServices.getConfiguration().setRetryUnrecoverableResourcesRegistrationInterval(0);
        journal.close();
        pds.close();
        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();
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
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("no-such-registered-resource");
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(1);
        pds.init();

        JdbcConnectionHandle handle = (JdbcConnectionHandle) pds.getConnection();
        XAResource xaResource = handle.getPooledConnection().getXAResource();
        handle.close();

        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
        assertEquals(0, TransactionManagerServices.getJournal().collectDanglingRecords().size());

        pds.close();

        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public void testRecoverBlockingStartup() throws Exception {
        xaResource.setRecoverException(new BitronixXAException("let's pretend recovery failed", XAException.XAER_RMERR));
        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());

        TransactionManagerServices.getRecoverer().run();
        assertEquals("error running recovery on resource mock-xads (XAER_RMERR)", TransactionManagerServices.getRecoverer().getCompletionException().getMessage());
        assertEquals("let's pretend recovery failed", TransactionManagerServices.getRecoverer().getCompletionException().getCause().getMessage());

        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
    }

    public void testRecoverByTmBlockingStartup() throws Exception {
        xaResource.setRecoverException(new BitronixXAException("let's pretend recovery failed", XAException.XAER_RMERR));
        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());

        try {
            TransactionManagerServices.getTransactionManager();
            fail("startup should have failed");
        } catch (Exception ex) {
            assertEquals("recovery failed, cannot safely start the transaction manager", ex.getMessage());
            assertEquals("error running recovery on resource mock-xads (XAER_RMERR)", ex.getCause().getMessage());
            assertEquals("let's pretend recovery failed", ex.getCause().getCause().getMessage());
        }

        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
    }

    public void testRecoverNotBlockingStartup() throws Exception {
        xaResource.setRecoverException(new BitronixXAException("let's pretend recovery failed", XAException.XAER_RMERR));
        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());

        TransactionManagerServices.getConfiguration().setRetryUnrecoverableResourcesRegistrationInterval(1);
        TransactionManagerServices.getConfiguration().setResourceConfigurationFilename("bitronix-res.properties");
        TransactionManagerServices.getRecoverer().run();
        assertNull(TransactionManagerServices.getRecoverer().getCompletionException());

        assertEquals(0, ResourceRegistrar.getResourcesUniqueNames().size());
        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
    }

    public void testRecoverByTmNotBlockingStartup() throws Exception {
        xaResource.setRecoverException(new BitronixXAException("let's pretend recovery failed", XAException.XAER_RMERR));
        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());

        TransactionManagerServices.getConfiguration().setRetryUnrecoverableResourcesRegistrationInterval(1);
        TransactionManagerServices.getConfiguration().setResourceConfigurationFilename("test/" + getClass().getName().replace('.', '/') + ".properties");
        TransactionManagerServices.getTransactionManager();

        assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
    }

    boolean listenerExecuted = false;
    public void testBackgroundRecovererSkippingInFlightTransactions() throws Exception {
        // change disk journal into mock journal
        Field journalField = TransactionManagerServices.class.getDeclaredField("journal");
        journalField.setAccessible(true);
        journalField.set(TransactionManagerServices.class, new MockJournal());

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

}
