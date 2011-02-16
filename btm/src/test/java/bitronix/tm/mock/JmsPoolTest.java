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
package bitronix.tm.mock;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import junit.framework.TestCase;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Queue;
import javax.jms.MessageProducer;
import java.lang.reflect.Field;

/**
 *
 * @author lorban
 */
public class JmsPoolTest extends TestCase {

    private PoolingConnectionFactory pcf;

    protected void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setJournal("null").setGracefulShutdownInterval(0);
        TransactionManagerServices.getTransactionManager();

        MockXAConnectionFactory.setStaticCloseXAConnectionException(null);
        MockXAConnectionFactory.setStaticCreateXAConnectionException(null);

        pcf = new PoolingConnectionFactory();
        pcf.setMinPoolSize(1);
        pcf.setMaxPoolSize(2);
        pcf.setMaxIdleTime(1);
        pcf.setClassName(MockXAConnectionFactory.class.getName());
        pcf.setUniqueName("pcf");
        pcf.setAllowLocalTransactions(true);
        pcf.setAcquisitionTimeout(1);
        pcf.init();
    }


    protected void tearDown() throws Exception {
        pcf.close();

        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public void testInitFailure() throws Exception {
        pcf.close();

        pcf = new PoolingConnectionFactory();
        pcf.setMinPoolSize(0);
        pcf.setMaxPoolSize(2);
        pcf.setMaxIdleTime(1);
        pcf.setClassName(MockXAConnectionFactory.class.getName());
        pcf.setUniqueName("pcf");
        pcf.setAllowLocalTransactions(true);
        pcf.setAcquisitionTimeout(1);

        TransactionManagerServices.getTransactionManager().begin();

        MockXAConnectionFactory.setStaticCreateXAConnectionException(new JMSException("not yet started"));
        try {
            pcf.init();
        } catch (Exception e) {

        }

        MockXAConnectionFactory.setStaticCreateXAConnectionException(null);
        pcf.init();

        pcf.createConnection().createSession(true, 0).createProducer(null).send(null);

        TransactionManagerServices.getTransactionManager().commit();
    }

    public void testReEnteringRecovery() throws Exception {
        pcf.startRecovery();
        try {
            pcf.startRecovery();
            fail("excpected RecoveryException");
        } catch (RecoveryException ex) {
            assertEquals("recovery already in progress on a PoolingConnectionFactory with an XAPool of resource pcf with 1 connection(s) (0 still available)", ex.getMessage());
        }

        // make sure startRecovery() can be called again once endRecovery() has been called
        pcf.endRecovery();
        pcf.startRecovery();
        pcf.endRecovery();
    }


    public void testPoolNotStartingTransactionManager() throws Exception {
        // make sure TM is not running
        TransactionManagerServices.getTransactionManager().shutdown();

        PoolingConnectionFactory pcf = new PoolingConnectionFactory();
        pcf.setMinPoolSize(1);
        pcf.setMaxPoolSize(2);
        pcf.setMaxIdleTime(1);
        pcf.setClassName(MockXAConnectionFactory.class.getName());
        pcf.setUniqueName("pcf2");
        pcf.setAllowLocalTransactions(true);
        pcf.setAcquisitionTimeout(1);
        pcf.init();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());

        Connection c = pcf.createConnection();
        Session s = c.createSession(false, 0);
        Queue q = s.createQueue("q");
        MessageProducer mp = s.createProducer(q);
        mp.send(s.createTextMessage("test123"));
        mp.close();
        s.close();
        c.close();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());

        pcf.close();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());
    }

    public void testPoolShrink() throws Exception {
        Field poolField = pcf.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pcf);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pcf.createConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pcf.createConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        c1.close();
        c2.close();

        Thread.sleep(1100); // leave enough time for the ide connections to expire
        TransactionManagerServices.getTaskScheduler().interrupt(); // wake up the task scheduler
        Thread.sleep(1100); // leave enough time for the scheduled shrinking task to do its work

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());
    }

    public void testPoolShrinkErrorHandling() throws Exception {
        Field poolField = pcf.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pcf);

        pcf.setMinPoolSize(0);
        pcf.reset();
        pcf.setMinPoolSize(1);
        MockXAConnectionFactory.setStaticCloseXAConnectionException(new JMSException("close fails because connection factory broken"));
        pcf.reset();

        // the pool is now loaded with one connection which will throw an exception when closed
        Thread.sleep(1100); // leave enough time for the ide connections to expire
        TransactionManagerServices.getTaskScheduler().interrupt(); // wake up the task scheduler
        Thread.sleep(100); // leave enough time for the scheduled shrinking task to do its work
        assertEquals(1, pool.inPoolSize());

        MockXAConnectionFactory.setStaticCreateXAConnectionException(new JMSException("createXAConnection fails because connection factory broken"));
        Thread.sleep(1100); // leave enough time for the ide connections to expire
        TransactionManagerServices.getTaskScheduler().interrupt(); // wake up the task scheduler
        Thread.sleep(100); // leave enough time for the scheduled shrinking task to do its work
        assertEquals(0, pool.inPoolSize());

        MockXAConnectionFactory.setStaticCreateXAConnectionException(null);
        Thread.sleep(1100); // leave enough time for the ide connections to expire
        TransactionManagerServices.getTaskScheduler().interrupt(); // wake up the task scheduler
        Thread.sleep(100); // leave enough time for the scheduled shrinking task to do its work
        assertEquals(1, pool.inPoolSize());
    }

    public void testPoolReset() throws Exception {
        Field poolField = pcf.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pcf);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pcf.createConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pcf.createConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        c1.close();
        c2.close();

        pcf.reset();

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());
    }

    public void testPoolResetErrorHandling() throws Exception {
        Field poolField = pcf.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pcf);

        pcf.setMinPoolSize(0);
        pcf.reset();
        pcf.setMinPoolSize(1);
        MockXAConnectionFactory.setStaticCloseXAConnectionException(new JMSException("close fails because connection factory broken"));
        pcf.reset();

        // the pool is now loaded with one connection which will throw an exception when closed
        pcf.reset();

        try {
            MockXAConnectionFactory.setStaticCreateXAConnectionException(new JMSException("createXAConnection fails because connection factory broken"));
            pcf.reset();
            fail("expected JMSException");
        } catch (JMSException ex) {
            assertEquals("createXAConnection fails because connection factory broken", ex.getMessage());
            assertEquals(0, pool.inPoolSize());
        }

        MockXAConnectionFactory.setStaticCreateXAConnectionException(null);
        pcf.reset();
        assertEquals(1, pool.inPoolSize());
    }
    
}
