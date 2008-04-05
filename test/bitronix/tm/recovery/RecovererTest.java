package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
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
        final Xid xid0 = new MockXid(0, 0, BitronixXid.FORMAT_ID);
        xaResource.addInDoubtXid(xid0);

        Set names = new HashSet();
        names.add("no-such-registered-resource");
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);
        assertEquals(1, TransactionManagerServices.getJournal().collectDanglingRecords().size());

        // the TM must run the recoverer in this scenario
        TransactionManagerServices.getTransactionManager();

        assertEquals(1, TransactionManagerServices.getJournal().collectDanglingRecords().size());
        assertNull(TransactionManagerServices.getRecoverer().getCompletionException());
        assertEquals(1, TransactionManagerServices.getRecoverer().getCommittedCount());
        assertEquals(0, TransactionManagerServices.getRecoverer().getRolledbackCount());
        assertEquals(1, xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);


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

        assertEquals(0, TransactionManagerServices.getJournal().collectDanglingRecords().size());

        TransactionManagerServices.getTransactionManager().shutdown();
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
