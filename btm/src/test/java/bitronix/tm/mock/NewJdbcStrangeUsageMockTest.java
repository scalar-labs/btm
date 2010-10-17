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
import bitronix.tm.resource.common.XAPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class NewJdbcStrangeUsageMockTest extends AbstractMockJdbcTest {

    private final static Logger log = LoggerFactory.getLogger(NewJdbcStrangeUsageMockTest.class);


    public void testDeferredReuse() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        XAPool pool1 = getPool(poolingDataSource1);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        assertEquals(POOL_SIZE, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** getting again connection from DS1");
        connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing again connection 1");
        connection1.close();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        assertEquals(POOL_SIZE, pool1.inPoolSize());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(11, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testDeferredCannotReuse() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        // Use DataSource2 because it does not have shared accessible connections
        XAPool pool2 = getPool(poolingDataSource2);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        assertEquals(POOL_SIZE, pool2.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** getting connection 1 from DS1");
        Connection connection1 = poolingDataSource2.getConnection();
        connection1.createStatement();

        assertEquals(POOL_SIZE -1, pool2.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** getting connection 2 from DS1");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        assertEquals(POOL_SIZE -2, pool2.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        assertEquals(POOL_SIZE -2, pool2.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        assertEquals(POOL_SIZE -2, pool2.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        assertEquals(POOL_SIZE, pool2.inPoolSize());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
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
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testConnectionCloseInDifferentContext() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** beginning");
        tm.begin();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();


        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        if (log.isDebugEnabled()) log.debug("*** beginning");
        tm.begin();


        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(22, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

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

        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
    }


    public void testClosingSuspendedConnectionsInDifferentContext() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();

        XAPool pool1 = getPool(poolingDataSource1);

        assertEquals(POOL_SIZE, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** suspending");
        Transaction t1 = tm.suspend();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** starting 2nd tx");
        tm.begin();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** closing connection 1 too eagerly within another context");
        try {
            // TODO: the ConnectionHandler tries to 'veto' the connection close here like the old pool did.
            // Instead, close the resource immediately or defer its release.
            connection1.close();
            fail("successfully closed a connection participating in a global transaction, this should never be allowed");
        } catch (SQLException ex) {
            assertEquals("cannot close a resource when its XAResource is taking part in an unfinished global transaction", ex.getCause().getMessage());
        }
        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** committing 2nd tx");
        tm.commit();

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** resuming");
        tm.resume(t1);

        assertEquals(POOL_SIZE -1, pool1.inPoolSize());

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        assertEquals(POOL_SIZE, pool1.inPoolSize());

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(18, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

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

}
