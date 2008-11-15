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
        CountingSynchronization sync = new CountingSynchronization();
        btm.getTransaction().registerSynchronization(sync);

        Thread.sleep(2000);
        assertEquals(Status.STATUS_MARKED_ROLLBACK, btm.getTransaction().getStatus());

        try {
            btm.commit();
            fail("commit should have thrown an RollbackException");
        } catch (RollbackException ex) {
            assertEquals("transaction timed out and has been rolled back", ex.getMessage());
        }
        assertEquals(0, sync.beforeCount);
        assertEquals(1, sync.afterCount);
    }

    public void testMarkedRollback() throws Exception {
        btm.begin();
        CountingSynchronization sync = new CountingSynchronization();
        btm.getTransaction().registerSynchronization(sync);
        btm.setRollbackOnly();

        assertEquals(Status.STATUS_MARKED_ROLLBACK, btm.getTransaction().getStatus());

        try {
            btm.commit();
            fail("commit should have thrown an RollbackException");
        } catch (RollbackException ex) {
            assertEquals("transaction was marked as rollback only and has been rolled back", ex.getMessage());
        }
        assertEquals(0, sync.beforeCount);
        assertEquals(1, sync.afterCount);
    }

    public void testRecycleAfterSuspend() throws Exception {
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

    public void testTransactionContextCleanup() throws Exception {
        assertEquals(Status.STATUS_NO_TRANSACTION, btm.getStatus());

        btm.begin();
        assertEquals(Status.STATUS_ACTIVE, btm.getStatus());
        
        final Transaction tx = btm.getTransaction();

        // commit on a different thread
        Thread t = new Thread() {
            public void run() {
                try {
                    tx.commit();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    fail();
                }
            }
        };

        t.start();
        t.join();

        assertNull(btm.getTransaction());
    }

    public void testBeforeCompletionAddsExtraSynchronizationInDifferentPriority() throws Exception {
        btm.begin();
        
        btm.getCurrentTransaction().getSynchronizationScheduler().add(new SynchronizationRegisteringSynchronization(btm.getCurrentTransaction()), 5);

        btm.commit();
    }

    private class SynchronizationRegisteringSynchronization implements Synchronization {

        private BitronixTransaction transaction;

        public SynchronizationRegisteringSynchronization(BitronixTransaction transaction) {
            this.transaction = transaction;
        }

        public void beforeCompletion() {
            try {
                transaction.getSynchronizationScheduler().add(new Synchronization() {

                    public void beforeCompletion() {
                    }

                    public void afterCompletion(int i) {
                    }
                }, 10);
            } catch (Exception e) {
                fail("unexpected exception: " + e);
            }
        }

        public void afterCompletion(int i) {
        }
    }

    private class CountingSynchronization implements Synchronization {
        public int beforeCount = 0;
        public int afterCount = 0;

        public void beforeCompletion() {
            beforeCount++;
        }

        public void afterCompletion(int i) {
            afterCount++;
        }
    }

}