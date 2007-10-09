package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.Uid;
import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

        // recoverer needs the journal to be open to be run manually
        journal = TransactionManagerServices.getJournal();
        journal.open();
    }


    protected void tearDown() throws Exception {
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
        xaResource.addInDoubtXid(new MockXid(0, 0, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(1, 1, BitronixXid.FORMAT_ID));
        xaResource.addInDoubtXid(new MockXid(2, 2, BitronixXid.FORMAT_ID));


        TransactionManagerServices.getRecoverer().run();

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(3, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(0, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    /**
     * Create 3 XIDs on the resource that are in the journal -> recoverer commits them.
     * @throws Exception
     */
    public void testRecoverCommitting() throws Exception {
        Xid xid0 = new MockXid(0, 0, BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);
        Xid xid1 = new MockXid(1, 1, BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid1);
        Xid xid2 = new MockXid(2, 2, BitronixXid.FORMAT_ID);
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

    public void testRecoverMissingResource() throws Exception {
        Xid xid0 = new MockXid(0, 0, BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);

        Set names = new HashSet();
        names.add("no-such-registered-resource");
        // recoverer needs the journal to be open to be run manually
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        TransactionManagerServices.getRecoverer().run();

        assertEquals("Recoverer could not find resource 'no-such-registered-resource' present in the journal, please " +
                "check ResourceLoader configuration file or make sure you manually created this resource before " +
                "starting the transaction manager", TransactionManagerServices.getRecoverer().getCompletionException().getMessage());

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(1, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    public void testRecoverError() throws Exception {
        xaResource.setRecoverException(new BitronixXAException("let's pretend recovery is not installed", XAException.XAER_RMERR));

        TransactionManagerServices.getRecoverer().run();

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals("error running recovery on resource mock-xads (XAER_RMERR)", TransactionManagerServices.getRecoverer().getCompletionException().getMessage());
        assertEquals("let's pretend recovery is not installed", TransactionManagerServices.getRecoverer().getCompletionException().getCause().getMessage());
    }

}
