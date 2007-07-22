package bitronix.tm.mock;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.jdbc.MockXAConnection;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;

import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.util.List;

/**
 * (c) Bitronix, 20-oct.-2005
 *
 * @author lorban
 */
public class NewJdbcWrongUsageMockTest extends AbstractMockJdbcTest {

    public void testPrepareXAFailureCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcPooledConnection pc1 = ((JdbcConnectionHandle) connection1).getPooledConnection();

        MockXAConnection mockXAConnection1 = (MockXAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) mockXAConnection1.getXAResource();
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
            assertEquals("transaction failed during prepare, error=XAER_RMERR", ex.getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        System.out.println(EventRecorder.dumpToString());

        assertEquals(18, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(ConnectionCloseEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(ConnectionCloseEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        XAResourcePrepareEvent prepareEvent1 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        if (prepareEvent1.getException() != null) {
            assertEquals("resource failed", prepareEvent1.getException().getMessage());
        }
        else {
            assertEquals("resource failed", prepareEvent2.getException().getMessage());
        }

        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertNotNull(orderedEvents.get(i++));
        assertNotNull(orderedEvents.get(i++));
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testPrepareRuntimeFailureCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcPooledConnection pc1 = ((JdbcConnectionHandle) connection1).getPooledConnection();
            MockXAConnection mockXAConnection1 = (MockXAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) mockXAConnection1.getXAResource();
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
            assertEquals("driver error", ex.getCause().getMessage());
            assertEquals("caught runtime exception during commit", ex.getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        System.out.println(EventRecorder.dumpToString());

        assertEquals(18, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(ConnectionCloseEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(ConnectionCloseEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        XAResourcePrepareEvent prepareEvent1 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        if (prepareEvent1.getException() != null) {
            assertEquals("driver error", prepareEvent1.getException().getMessage());
        }
        else {
            assertEquals("driver error", prepareEvent2.getException().getMessage());
        }
        
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertNotNull(orderedEvents.get(i++));
        assertNotNull(orderedEvents.get(i++));
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

}
