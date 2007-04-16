package bitronix.tm.recovery;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixXid;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.journal.Journal;
import bitronix.tm.internal.Uid;
import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.drivers.FbTest;
import junit.framework.TestCase;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.Status;
import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 4-apr-2006
 * Time: 10:26:03
 * To change this template use File | Settings | File Templates.
 */
public class RecovererTest extends TestCase {

    public void testRecoverPresumeAborted() throws Exception {
        // create an in-doubt TX
        XAConnection xac = FbTest.getXADataSource1().getXAConnection();
        XAResource xar = xac.getXAResource();

        Xid xid0 = new MockXid(0, 0, BitronixXid.FORMAT_ID);
        createInDoubt(xid0);

        Xid xid1 = new MockXid(1, 0, BitronixXid.FORMAT_ID);
        createInDoubt(xid1);

        Xid xid2 = new MockXid(0, 2, BitronixXid.FORMAT_ID);
        createInDoubt(xid2);

        Journal journal = TransactionManagerServices.getJournal();
        journal.open();

        // creating the resource registers it for recovery
        XAResourceProducer producer = FbTest.getDataSourceBean1().createResource();
        TransactionManagerServices.getRecoverer().run();

        xac = FbTest.getXADataSource1().getXAConnection();
        xar = xac.getXAResource();

        producer.close();
        journal.close();
        assertEquals(0, xar.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    public void testRecoverCommitting() throws Exception {
        // create an in-doubt TX
        Xid xid0 = new MockXid(0, 0, BitronixXid.FORMAT_ID);
        createInDoubt(xid0);

        Set names = new HashSet();
        names.add(FbTest.getDataSourceBean1().getUniqueName());
        Journal journal = TransactionManagerServices.getJournal();
        journal.open();
        journal.log(Status.STATUS_COMMITTING, new Uid(xid0.getGlobalTransactionId()), names);

        // creating the resource registers it for recovery
        XAResourceProducer producer = FbTest.getDataSourceBean1().createResource();
        TransactionManagerServices.getRecoverer().run();

        XAConnection xac = FbTest.getXADataSource1().getXAConnection();
        XAResource xar = xac.getXAResource();

        producer.close();
        journal.close();
        assertEquals(0, xar.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN).length);
    }

    private void createInDoubt(Xid xid) throws Exception {
        XAConnection xac = FbTest.getXADataSource1().getXAConnection();
        XAResource xar = xac.getXAResource();

        xar.start(xid, XAResource.TMNOFLAGS);
        xar.end(xid, XAResource.TMSUCCESS);
        xar.prepare(xid);

        xac.close();
    }

}
