package bitronix.tm;

import junit.framework.TestCase;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * <p>&copy; <a href="http://www.bitronix.be">Bitronix Software</a></p>
 *
 * @author lorban
 */
public class BitronixTransactionSynchronizationRegistryTest extends TestCase {

    private BitronixTransactionManager btm;

    protected void setUp() throws Exception {
        btm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        if (btm.getStatus() != Status.STATUS_NO_TRANSACTION)
            btm.rollback();
        btm.shutdown();
    }


    public void testRegistryResources() throws Exception {
        TransactionSynchronizationRegistry reg = TransactionManagerServices.getTransactionSynchronizationRegistry();

        try {
            reg.putResource("0", "zero");
            fail("expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertEquals("no transaction started on curent thread", ex.getMessage());
        }

        btm.begin();
        reg.putResource("1", "one");
        assertEquals("one", reg.getResource("1"));
        btm.commit();

        try {
            reg.getResource("1");
            fail("expected IllegalStateException");
        } catch (Exception ex) {
            assertEquals("no transaction started on curent thread", ex.getMessage());
        }

        btm.begin();
        assertNull(reg.getResource("1"));
        btm.commit();
    }


    public void testRegistrySynchronizations() throws Exception {
        TransactionSynchronizationRegistry reg = TransactionManagerServices.getTransactionSynchronizationRegistry();

        CoutingSynchronization normalSync = new CoutingSynchronization();
        CoutingSynchronization interposedSync = new CoutingSynchronization();

        btm.begin();

        reg.registerInterposedSynchronization(interposedSync);
        btm.getCurrentTransaction().registerSynchronization(normalSync);

        btm.commit();

        assertTrue(normalSync.getBeforeTimestamp() < interposedSync.getBeforeTimestamp());
        assertTrue(interposedSync.getBeforeTimestamp() < normalSync.getAfterTimestamp());
        assertTrue(interposedSync.getAfterTimestamp() < normalSync.getAfterTimestamp());
    }

    private class CoutingSynchronization implements Synchronization {

        private long beforeTimestamp;
        private long afterTimestamp;

        public long getBeforeTimestamp() {
            return beforeTimestamp;
        }

        public long getAfterTimestamp() {
            return afterTimestamp;
        }

        public void beforeCompletion() {
            beforeTimestamp = System.currentTimeMillis();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        public void afterCompletion(int status) {
            afterTimestamp = System.currentTimeMillis();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

}
