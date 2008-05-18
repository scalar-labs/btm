package bitronix.tm;

import junit.framework.TestCase;

import javax.transaction.*;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import bitronix.tm.mock.resource.jdbc.MockDriver;

import java.sql.Connection;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class JtaTest extends TestCase {

    private BitronixTransactionManager btm;

    protected void setUp() throws Exception {
        btm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        btm.shutdown();
    }

    public void testTransactionManagerGetTransaction() throws Exception {
        assertNull(btm.getTransaction());
        
        btm.begin();
        assertNotNull(btm.getTransaction());

        btm.commit();
        assertNull(btm.getTransaction());

        btm.begin();
        assertNotNull(btm.getTransaction());

        btm.rollback();
    }

    public void testTimeout() throws Exception {
        btm.setTransactionTimeout(1);
        btm.begin();
        Thread.sleep(1100);
        assertEquals(Status.STATUS_MARKED_ROLLBACK, btm.getTransaction().getStatus());

        try {
            btm.commit();
            fail("commit should have thrown an RollbackException");
        } catch (RollbackException ex) {
            assertEquals("transaction timed out and has been rolled back", ex.getMessage());
        }
    }

    public void testMarkedRollback() throws Exception {
        btm.begin();

        btm.setRollbackOnly();

        assertEquals(Status.STATUS_MARKED_ROLLBACK, btm.getTransaction().getStatus());

        try {
            btm.commit();
            fail("commit should have thrown an RollbackException");
        } catch (RollbackException ex) {
            assertEquals("transaction was marked as rollback only and has been rolled back", ex.getMessage());
        }
    }

    public void test() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setClassName(LrcXADataSource.class.getName());
        pds.setUniqueName("lrc-pds");
        pds.setMaxPoolSize(2);
        pds.getDriverProperties().setProperty("driverClassName", MockDriver.class.getName());
        pds.init();

        btm.begin();

        Connection c1 = pds.getConnection();
        c1.createStatement();
        c1.close();

        Transaction tx = btm.suspend();

            btm.begin();

            Connection c11 = pds.getConnection();
            c11.createStatement();
            c11.close();

            btm.commit();


        btm.resume(tx);

        Connection c2 = pds.getConnection();
        c2.createStatement();
        c2.close();

        btm.commit();

        pds.close();
    }

}