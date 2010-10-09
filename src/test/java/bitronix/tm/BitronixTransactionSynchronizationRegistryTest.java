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
package bitronix.tm;

import junit.framework.TestCase;

import javax.transaction.*;

/**
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

    public void testMultiThreaded() throws Exception {
        final TransactionSynchronizationRegistry reg = TransactionManagerServices.getTransactionSynchronizationRegistry();

        btm.begin();
        reg.putResource("1", "one");
        assertEquals("one", reg.getResource("1"));
        btm.commit();

        Thread t = new Thread() {
            public void run() {
                try {
                    btm.begin();
                    reg.putResource("1", "one");
                    assertEquals("one", reg.getResource("1"));
                    btm.commit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        t.start();
        t.join();
    }

    public void testRegistryResources() throws Exception {
        TransactionSynchronizationRegistry reg = TransactionManagerServices.getTransactionSynchronizationRegistry();

        try {
            reg.putResource("0", "zero");
            fail("expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertEquals("no transaction started on current thread", ex.getMessage());
        }

        btm.begin();
        reg.putResource("1", "one");
        assertEquals("one", reg.getResource("1"));
        btm.commit();

        try {
            reg.getResource("1");
            fail("expected IllegalStateException");
        } catch (Exception ex) {
            assertEquals("no transaction started on current thread", ex.getMessage());
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
