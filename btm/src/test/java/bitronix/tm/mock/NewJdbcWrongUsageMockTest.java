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

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.List;

import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.*;

import bitronix.tm.*;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.*;
import bitronix.tm.mock.resource.jms.MockConnectionFactory;
import bitronix.tm.resource.jdbc.*;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.resource.jms.lrc.LrcXAConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lorban
 */
public class NewJdbcWrongUsageMockTest extends AbstractMockJdbcTest {

    private final static Logger log = LoggerFactory.getLogger(NewJdbcWrongUsageMockTest.class);

    public void testPrepareXAFailureCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        JdbcPooledConnection pc1 = handle.getPooledConnection();

        XAConnection xaConnection1 = (XAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) xaConnection1.getXAResource();
            XAException xaException = new XAException("resource failed");
            xaException.errorCode = XAException.XAER_RMERR;
            mockXAResource.setPrepareException(xaException);
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        connection1.close();
        connection2.close();

        try {
            tm.commit();
            fail("TM should have thrown rollback exception");
        } catch (RollbackException ex) {
            assertTrue("Got: " + ex.getMessage(), ex.getMessage().matches("transaction failed to prepare: a Bitronix Transaction with GTRID (.*?) status=ROLLEDBACK, 2 resource\\(s\\) enlisted (.*?)"));
            assertTrue("Got: " + ex.getCause().getMessage(), ex.getCause().getMessage().matches("transaction failed during prepare of a Bitronix Transaction with GTRID (.*?) status=PREPARING, 2 resource\\(s\\) enlisted (.*?) resource\\(s\\) \\[pds1\\] threw unexpected exception"));

            assertEquals("collected 1 exception(s):" + System.getProperty("line.separator") +
                    " [pds1 - javax.transaction.xa.XAException(XAER_RMERR) - resource failed]", ex.getCause().getCause().getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(16, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourcePrepareEvent prepareEvent1 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals("resource failed", prepareEvent1.getException().getMessage());
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals(XAResource.XA_OK, prepareEvent2.getReturnCode());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourceRollbackEvent rollbackEvent1 = (XAResourceRollbackEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent2.getSource() == rollbackEvent1.getSource());
        XAResourceRollbackEvent rollbackEvent2 = (XAResourceRollbackEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent1.getSource() == rollbackEvent2.getSource());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testPrepareRuntimeFailureCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        JdbcPooledConnection pc1 = handle.getPooledConnection();
            XAConnection xaConnection1 = (XAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) xaConnection1.getXAResource();
            mockXAResource.setPrepareException(new RuntimeException("driver error"));
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        connection1.close();
        connection2.close();

        try {
            tm.commit();
            fail("TM should have thrown exception");
        } catch (RollbackException ex) {
            assertTrue("Got: " + ex.getMessage(), ex.getMessage().matches("transaction failed to prepare: a Bitronix Transaction with GTRID (.*?) status=ROLLEDBACK, 2 resource\\(s\\) enlisted (.*?)"));
            assertTrue("Got: " + ex.getCause().getMessage(), ex.getCause().getMessage().matches("transaction failed during prepare of a Bitronix Transaction with GTRID (.*?) status=PREPARING, 2 resource\\(s\\) enlisted (.*?) resource\\(s\\) \\[pds1\\] threw unexpected exception"));

            assertEquals("collected 1 exception(s):" + System.getProperty("line.separator") +
                    " [pds1 - java.lang.RuntimeException - driver error]", ex.getCause().getCause().getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(16, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourcePrepareEvent prepareEvent1 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals("driver error", prepareEvent1.getException().getMessage());
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals(XAResource.XA_OK, prepareEvent2.getReturnCode());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourceRollbackEvent rollbackEvent1 = (XAResourceRollbackEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent2.getSource() == rollbackEvent1.getSource());
        XAResourceRollbackEvent rollbackEvent2 = (XAResourceRollbackEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent1.getSource() == rollbackEvent2.getSource());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testIncorrectSuspendResume() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        Transaction tx = tm.suspend();

        assertNull(tm.suspend());

        try {
            tm.resume(null);
            fail("TM has allowed resuming a null TX context");
        } catch (InvalidTransactionException ex) {
            assertEquals("resumed transaction cannot be null", ex.getMessage());
        }

        tm.resume(tx);

        try {
            tm.resume(tx);
            fail("TM has allowed resuming a TX context when another one is still running");
        } catch (IllegalStateException ex) {
            assertEquals("a transaction is already running on this thread", ex.getMessage());
        }

        connection1.close();
        connection2.close();

        tm.commit();
    }

    public void testEagerEnding() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        try {
            tm.rollback();
            fail("TM allowed rollback with no TX started");
        } catch (IllegalStateException ex) {
            assertEquals("no transaction started on this thread", ex.getMessage());
        }
        try {
            tm.commit();
            fail("TM allowed commit with no TX started");
        } catch (IllegalStateException ex) {
            assertEquals("no transaction started on this thread", ex.getMessage());
        }
    }

     public void testRegisterTwoLrc() throws Exception {
         BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

         PoolingDataSource lrcDs1 = new PoolingDataSource();
         lrcDs1.setClassName(LrcXADataSource.class.getName());
         lrcDs1.setUniqueName(DATASOURCE1_NAME + "_lrc");
         lrcDs1.setMinPoolSize(POOL_SIZE);
         lrcDs1.setMaxPoolSize(POOL_SIZE);
         lrcDs1.setAllowLocalTransactions(true);
         lrcDs1.getDriverProperties().setProperty("driverClassName", MockDriver.class.getName());
         lrcDs1.getDriverProperties().setProperty("url", "");
         lrcDs1.init();

         PoolingDataSource lrcDs2 = new PoolingDataSource();
         lrcDs2.setClassName(LrcXADataSource.class.getName());
         lrcDs2.setUniqueName(DATASOURCE2_NAME + "_lrc");
         lrcDs2.setMinPoolSize(POOL_SIZE);
         lrcDs2.setMaxPoolSize(POOL_SIZE);
         lrcDs2.setAllowLocalTransactions(true);
         lrcDs2.getDriverProperties().setProperty("driverClassName", MockDriver.class.getName());
         lrcDs2.getDriverProperties().setProperty("url", "");
         lrcDs2.init();

         tm.begin();

         Connection c1 = lrcDs1.getConnection();
         c1.createStatement();
         c1.close();

         Connection c2 = lrcDs2.getConnection();
         try {
             c2.createStatement();
             fail("expected SQLException");
         } catch (SQLException ex) {
        	 assertTrue(ex.getMessage().startsWith("error enlisting a JdbcConnectionHandle of a JdbcPooledConnection from datasource pds2_lrc in state ACCESSIBLE with usage count 1 wrapping a JDBC LrcXAConnection on Mock for Connection"));
             assertTrue(ex.getCause().getMessage().matches("cannot enlist more than one non-XA resource, tried enlisting an XAResourceHolderState with uniqueName=pds2_lrc XAResource=a JDBC LrcXAResource in state NO_TX with XID null, already enlisted: an XAResourceHolderState with uniqueName=pds1_lrc XAResource=a JDBC LrcXAResource in state STARTED \\(started\\) with XID a Bitronix XID .*"));
         }
         c2.close();

         tm.commit();

         lrcDs2.close();
         lrcDs1.close();
     }

     public void testRegisterTwoLrcJms() throws Exception {
         BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

         PoolingConnectionFactory pcf = new PoolingConnectionFactory();
         pcf.setClassName(LrcXAConnectionFactory.class.getName());
         pcf.setUniqueName("pcf_lrc");
         pcf.setMaxPoolSize(1);
         pcf.getDriverProperties().setProperty("connectionFactoryClassName", MockConnectionFactory.class.getName());
         pcf.init();

         PoolingDataSource lrcDs2 = new PoolingDataSource();
         lrcDs2.setClassName(LrcXADataSource.class.getName());
         lrcDs2.setUniqueName(DATASOURCE2_NAME + "_lrc");
         lrcDs2.setMinPoolSize(POOL_SIZE);
         lrcDs2.setMaxPoolSize(POOL_SIZE);
         lrcDs2.setAllowLocalTransactions(true);
         lrcDs2.getDriverProperties().setProperty("driverClassName", MockDriver.class.getName());
         lrcDs2.getDriverProperties().setProperty("url", "");
         lrcDs2.init();

         tm.begin();

         javax.jms.Connection c = pcf.createConnection();
         javax.jms.Session s = c.createSession(true, 0);
         javax.jms.MessageProducer p = s.createProducer(null);
         p.send(null);
         c.close();

         Connection c2 = lrcDs2.getConnection();
         try {
             c2.createStatement();
             fail("expected SQLException");
         } catch (SQLException ex) {
        	 assertTrue(ex.getMessage().startsWith("error enlisting a JdbcConnectionHandle of a JdbcPooledConnection from datasource pds2_lrc in state ACCESSIBLE with usage count 1 wrapping a JDBC LrcXAConnection on Mock for Connection"));
             assertTrue(ex.getCause().getMessage().startsWith("cannot enlist more than one non-XA resource, tried enlisting an XAResourceHolderState with uniqueName=pds2_lrc XAResource=a JDBC LrcXAResource in state NO_TX with XID null, already enlisted: an XAResourceHolderState with uniqueName=pcf_lrc XAResource=a JMS LrcXAResource in state STARTED of session Mock for Session"));
         }
         c2.close();

         tm.commit();

         lrcDs2.close();
         pcf.close();
     }

}
