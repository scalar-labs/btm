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

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;
import bitronix.tm.mock.events.XAResourceEndEvent;
import bitronix.tm.mock.events.XAResourceRollbackEvent;
import bitronix.tm.mock.events.XAResourceStartEvent;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class DelistmentTest extends TestCase {
    private final static Logger log = LoggerFactory.getLogger(DelistmentTest.class);

    private PoolingDataSource poolingDataSource1;
    private PoolingDataSource poolingDataSource2;
    private BitronixTransactionManager btm;


    protected void setUp() throws Exception {
        EventRecorder.clear();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journalRef");
        field.setAccessible(true);
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());

        poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource1.setUniqueName("pds1");
        poolingDataSource1.setMinPoolSize(5);
        poolingDataSource1.setMaxPoolSize(5);
        poolingDataSource1.setAutomaticEnlistingEnabled(true);
        poolingDataSource1.init();

        poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource2.setUniqueName("pds2");
        poolingDataSource2.setMinPoolSize(5);
        poolingDataSource2.setMaxPoolSize(5);
        poolingDataSource2.setAutomaticEnlistingEnabled(true);
        poolingDataSource2.init();

        btm = TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        poolingDataSource1.close();
        poolingDataSource2.close();
        btm.shutdown();
    }

    public void testDelistErrorOnCommit() throws Exception {
        btm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle1 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection1 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle1.getPooledConnection());
        MockXAResource xaResource1 = (MockXAResource) xaConnection1.getXAResource();
        connection1.createStatement(); // triggers enlistment

        xaResource1.setEndException(new BitronixXAException("screw delistment", XAException.XAER_RMERR));
        xaResource1.setRollbackException(new BitronixXAException("delistment was screwed, cannot rollback", XAException.XAER_RMERR));

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle2.getPooledConnection());
        MockXAResource xaResource2 = (MockXAResource) xaConnection2.getXAResource();
        connection2.createStatement(); // triggers enlistment

        try {
            btm.commit();
            fail("expected RollbackException");
        } catch (RollbackException ex) {
            assertEquals("delistment error caused transaction rollback" + System.getProperty("line.separator") + "  resource(s) [pds1] could not be delisted", ex.getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(9, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertTrue(((XAResourceRollbackEvent) orderedEvents.get(i++)).getSource() == xaResource2);
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
    }

    public void testDelistUnilateralRollbackOnCommit() throws Exception {
        btm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle1 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection1 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle1.getPooledConnection());
        MockXAResource xaResource1 = (MockXAResource) xaConnection1.getXAResource();
        connection1.createStatement();

        xaResource1.setEndException(new BitronixXAException("what was that transaction again?", XAException.XAER_NOTA));
        xaResource1.setRollbackException(new BitronixXAException("delistment unilaterally rolled back, cannot rollback twice", XAException.XAER_RMERR));

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle2.getPooledConnection());
        MockXAResource xaResource2 = (MockXAResource) xaConnection2.getXAResource();
        connection2.createStatement(); // triggers enlistment

        try {
            btm.commit();
            fail("expected RollbackException");
        } catch (RollbackException ex) {
            assertEquals("delistment error caused transaction rollback" + System.getProperty("line.separator") + "  resource(s) [pds1] unilaterally rolled back", ex.getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(9, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertTrue(((XAResourceRollbackEvent) orderedEvents.get(i++)).getSource() == xaResource2);
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
    }

    public void testDelistErrorAndUnilateralRollbackOnCommit() throws Exception {
        btm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle1 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection1 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle1.getPooledConnection());
        MockXAResource xaResource1 = (MockXAResource) xaConnection1.getXAResource();
        xaResource1.setEndException(new BitronixXAException("screw delistment", XAException.XAER_RMERR));
        xaResource1.setRollbackException(new BitronixXAException("delistment was screwed, cannot rollback", XAException.XAER_RMERR));

        connection1.createStatement(); // triggers enlistment

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle2.getPooledConnection());
        MockXAResource xaResource2 = (MockXAResource) xaConnection2.getXAResource();
        xaResource2.setEndException(new BitronixXAException("what was that transaction again?", XAException.XAER_NOTA));
        xaResource2.setRollbackException(new BitronixXAException("delistment unilaterally rolled back, cannot rollback twice", XAException.XAER_RMERR));

        connection2.createStatement(); // triggers enlistment

        try {
            btm.commit();
            fail("expected RollbackException");
        } catch (RollbackException ex) {
            assertEquals("delistment error caused transaction rollback" + System.getProperty("line.separator")
                    + "  resource(s) [pds2] unilaterally rolled back" + System.getProperty("line.separator")
                    + "  resource(s) [pds1] could not be delisted"
                    , ex.getMessage());
        }

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(8, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
    }

    public void testDelistErrorAndUnilateralRollbackOnRollback() throws Exception {
        btm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        XAConnection xaConnection1 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle.getPooledConnection());
        MockXAResource xaResource1 = (MockXAResource) xaConnection1.getXAResource();
        xaResource1.setEndException(new BitronixXAException("screw delistment", XAException.XAER_RMERR));
        xaResource1.setRollbackException(new BitronixXAException("delistment was screwed, cannot rollback", XAException.XAER_RMERR));

        connection1.createStatement(); // triggers enlistment

        Connection connection2 = poolingDataSource2.getConnection();
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection2);
        XAConnection xaConnection2 = (XAConnection) AbstractMockJdbcTest.getWrappedXAConnectionOf(handle2.getPooledConnection());
        MockXAResource xaResource2 = (MockXAResource) xaConnection2.getXAResource();
        xaResource2.setEndException(new BitronixXAException("what was that transaction again?", XAException.XAER_NOTA));
        xaResource2.setRollbackException(new BitronixXAException("delistment unilaterally rolled back, cannot rollback twice", XAException.XAER_RMERR));

        connection2.createStatement(); // triggers enlistment

        btm.rollback();

        log.info(EventRecorder.dumpToString());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(8, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
    }


}
