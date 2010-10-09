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
import bitronix.tm.mock.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author lorban
 */
public class NewJdbcSuspendResumeMockTest extends AbstractMockJdbcTest {

    private final static Logger log = LoggerFactory.getLogger(NewJdbcSuspendResumeMockTest.class);

    public void testSimpleAssertions() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        assertNull(tm.suspend());

        try {
            tm.resume(null);
            fail("expected InvalidTransactionException");
        } catch (InvalidTransactionException ex) {
            assertEquals("resumed transaction cannot be null", ex.getMessage());
        }

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");
    }

    public void testSimpleWorkingCase() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        if (log.isDebugEnabled()) log.debug("*** resuming");
        tm.resume(t1);

        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(13, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testNoTmJoin() throws Exception {
        poolingDataSource1.setUseTmJoin(false);

        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        if (log.isDebugEnabled()) log.debug("*** resuming");
        tm.resume(t1);

        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(15, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
        assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testReEnlistmentAfterSuspend() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        if (log.isDebugEnabled()) log.debug("*** before begin2");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin2");
        if (log.isDebugEnabled()) log.debug("*** reusing connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** marking subTX as rollback only");
        tm.setRollbackOnly();
        if (log.isDebugEnabled()) log.debug("*** rolling back");
        tm.rollback();
        if (log.isDebugEnabled()) log.debug("*** rolling back");

        if (log.isDebugEnabled()) log.debug("*** subTX is done");
        tm.resume(t1);

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(20, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testClosingSuspendedConnections() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 1 too eagerly");
        try {
            // TODO: the TM tries to 'veto' the connection close here like the old pool did.
            // Instead, close the resource immediately or defer its release.
            connection1.close();
            fail("successfully closed a connection participating in a global transaction, this should never be allowed");
        } catch (SQLException ex) {
            assertEquals("cannot close a resource when its XAResource is taking part in an unfinished global transaction", ex.getCause().getMessage());
        }
        assertFalse(connection1.isClosed());

        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** resuming");
        tm.resume(t1);

        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        assertEquals(POOL_SIZE, getPool(poolingDataSource1).inPoolSize());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(13, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testInterleavedLocalGlobalTransactions() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        Connection connection2 = poolingDataSource1.getConnection();
        assertNull(tm.getTransaction());
        connection2.createStatement();
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** resuming");
        tm.resume(t1);

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(15, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
            assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames().size());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testInterleavedGlobalGlobalTransactionsWithDifferentConnections() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug(" *** suspending transaction");
        Transaction t1 = tm.suspend();
        assertNull(tm.getTransaction());

        if (log.isDebugEnabled()) log.debug(" *** begin interleaved transaction");
        tm.begin();
        Connection connection2 = poolingDataSource1.getConnection();
        connection2.createStatement();
        connection2.close();
        if (log.isDebugEnabled()) log.debug(" *** commit interleaved transaction");
        tm.commit();

        if (log.isDebugEnabled()) log.debug(" *** resuming transaction");
        tm.resume(t1);

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(23, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        // interleaved transaction
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
            assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames().size());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testInterleavedGlobalGlobalTransactionsWithDifferentConnectionsLateSuspend() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug(" *** suspending transaction");
        Transaction t1 = tm.suspend();
        assertNull(tm.getTransaction());

        if (log.isDebugEnabled()) log.debug(" *** begin interleaved transaction");
        tm.begin();
        Connection connection2 = poolingDataSource1.getConnection();
        assertEquals(POOL_SIZE -2, getPool(poolingDataSource1).inPoolSize());
        connection2.createStatement();
        connection2.close();
        if (log.isDebugEnabled()) log.debug(" *** commit interleaved transaction");
        tm.commit();
        assertEquals(POOL_SIZE -1, getPool(poolingDataSource1).inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** resuming transaction");
        tm.resume(t1);

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");
        assertEquals(POOL_SIZE, getPool(poolingDataSource1).inPoolSize());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(23, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        // interleaved transaction
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
            assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames().size());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testJoinAfterSuspend() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        if (log.isDebugEnabled()) log.debug("*** get C1");
        Connection c1 = poolingDataSource1.getConnection();
        c1.createStatement();
        c1.close();

        if (log.isDebugEnabled()) log.debug("*** get C2");
        Connection c2 = poolingDataSource2.getConnection();
        c2.createStatement();
        c2.close();

        Transaction tx = tm.suspend();
        tm.resume(tx);

        if (log.isDebugEnabled()) log.debug("*** get C3");
        Connection c3 = poolingDataSource2.getConnection();
        c3.createStatement();
        c3.close();

        if (log.isDebugEnabled()) log.debug("*** get C4");
        Connection c4 = poolingDataSource1.getConnection();
        c4.createStatement();
        c4.close();

        tm.commit();

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(25, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

        // suspend happens here
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        // resume happens here
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

        XAResourceIsSameRmEvent evt = (XAResourceIsSameRmEvent) orderedEvents.get(i++);
        XAResource src = (XAResource) evt.getSource();
        XAResource comp = evt.getXAResource();
        assertTrue(poolingDataSource2.findXAResourceHolder(src) != null);
        assertTrue(poolingDataSource2.findXAResourceHolder(comp) != null);

        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
        assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testReusePreparedStatementAfterSuspendResume() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();

        Transaction tx = tm.suspend();
        tm.resume(tx);

        connection1.prepareStatement("some sql");

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());
    }

}
