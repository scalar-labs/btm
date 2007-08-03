package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.Uid;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.HashSet;
import java.util.Set;
import java.io.File;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class RecovererTest extends TestCase {

    private MockXAResource xaResource;
    private PoolingDataSource pds;


    protected void setUp() throws Exception {
        pds = new PoolingDataSource();
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("mock-xads");
        pds.setPoolSize(3);
        pds.init();

        new File(TransactionManagerServices.getConfiguration().getLogPart1Filename()).delete();
        new File(TransactionManagerServices.getConfiguration().getLogPart2Filename()).delete();

        JdbcConnectionHandle handle = (JdbcConnectionHandle) pds.getConnection();
        xaResource = (MockXAResource) handle.getPooledConnection().getXAResource();
    }


    protected void tearDown() throws Exception {
        pds.close();
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

        // recoverer needs the journal to be open to be run manually
        Journal journal = TransactionManagerServices.getJournal();
        journal.open();
        TransactionManagerServices.getRecoverer().run();
        journal.close();

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
        // recoverer needs the journal to be open to be run manually
        Journal journal = TransactionManagerServices.getJournal();
        journal.open();
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid1.getGlobalTransactionId()), names);
        journal.log(Status.STATUS_COMMITTING, new Uid(xid2.getGlobalTransactionId()), names);
        TransactionManagerServices.getRecoverer().run();
        journal.close();

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
        Journal journal = TransactionManagerServices.getJournal();
        journal.open();
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        TransactionManagerServices.getRecoverer().run();
        journal.close();

        assertEquals("Recoverer could not find resource 'no-such-registered-resource' present in the journal, please " +
                "check ResourceLoader configuration file or make sure you manually created this resource before " +
                "starting the transaction manager", TransactionManagerServices.getRecoverer().getCompletionException().getMessage());

        assertEquals(0, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(1, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

}
