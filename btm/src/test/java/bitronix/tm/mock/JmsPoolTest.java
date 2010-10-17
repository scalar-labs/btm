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
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import junit.framework.TestCase;

import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.Queue;
import javax.jms.MessageProducer;

/**
 *
 * @author lorban
 */
public class JmsPoolTest extends TestCase {

    private PoolingConnectionFactory pcf;

    protected void setUp() throws Exception {
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

}