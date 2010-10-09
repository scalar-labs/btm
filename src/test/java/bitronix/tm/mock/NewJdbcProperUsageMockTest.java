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

import java.io.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.*;

import org.slf4j.*;

import bitronix.tm.*;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockXAResource;
import bitronix.tm.mock.resource.jdbc.MockDriver;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.jdbc.*;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;

/**
 *
 * @author lorban
 */
public class NewJdbcProperUsageMockTest extends AbstractMockJdbcTest {

    private final static Logger log = LoggerFactory.getLogger(NewJdbcProperUsageMockTest.class);
    private static final int LOOPS = 2;

    public void testSimpleWorkingCase() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(10);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 2");
        connection2.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 2");
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testOrderedCommitResources() throws Exception {
        poolingDataSource1.setTwoPcOrderingPosition(200);
        poolingDataSource2.setTwoPcOrderingPosition(-1);

        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(10);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 2");
        connection2.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 2");
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
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
        assertEquals(XAResource.XA_OK, prepareEvent1.getReturnCode());
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals(XAResource.XA_OK, prepareEvent2.getReturnCode());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourceCommitEvent commitEvent1 = (XAResourceCommitEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent2.getSource() == commitEvent1.getSource());
        assertEquals(false, commitEvent1.isOnePhase());
        XAResourceCommitEvent commitEvent2 = (XAResourceCommitEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent1.getSource() == commitEvent2.getSource());
        assertEquals(false, commitEvent2.isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testReversePhase2Order() throws Exception {
        poolingDataSource1.setTwoPcOrderingPosition(1);
        poolingDataSource2.setTwoPcOrderingPosition(1);

        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(10);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 2");
        connection2.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 2");
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
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
        assertEquals(XAResource.XA_OK, prepareEvent1.getReturnCode());
        XAResourcePrepareEvent prepareEvent2 = (XAResourcePrepareEvent) orderedEvents.get(i++);
        assertEquals(XAResource.XA_OK, prepareEvent2.getReturnCode());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        XAResourceCommitEvent commitEvent1 = (XAResourceCommitEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent2.getSource() == commitEvent1.getSource());
        assertEquals(false, commitEvent1.isOnePhase());
        XAResourceCommitEvent commitEvent2 = (XAResourceCommitEvent) orderedEvents.get(i++);
        assertTrue(prepareEvent1.getSource() == commitEvent2.getSource());
        assertEquals(false, commitEvent2.isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testLrc() throws Exception {
        PoolingDataSource poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(LrcXADataSource.class.getName());
        poolingDataSource2.setUniqueName(DATASOURCE2_NAME + "_lrc");
        poolingDataSource2.setMinPoolSize(POOL_SIZE);
        poolingDataSource2.setMaxPoolSize(POOL_SIZE);
        poolingDataSource2.setAllowLocalTransactions(true);
        poolingDataSource2.getDriverProperties().setProperty("driverClassName", MockDriver.class.getName());
        poolingDataSource2.getDriverProperties().setProperty("user", "user");
        poolingDataSource2.getDriverProperties().setProperty("password", "password");
        poolingDataSource2.init();


        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(10);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 2");
        connection2.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 2");
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 1");
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(12, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
        assertEquals(LocalCommitEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testStatementTimeout() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(1);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();

        Thread.sleep(1500);

        try {
            if (log.isDebugEnabled()) log.debug("*** creating statement 2 on connection 1");
            connection1.createStatement();
            fail("expected transaction to time out");
        } catch (SQLException ex) {
            assertEquals("transaction timed out", ex.getCause().getMessage());
        }

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** rolling back");
        tm.rollback();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(9, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testCommitTimeout() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.setTransactionTimeout(1);
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** creating statement 1 on connection 1");
        connection1.createStatement();

        Thread.sleep(1500);

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        try {
            tm.commit();
            fail("expected transaction to time out");
        } catch (RollbackException ex) {
            assertEquals("transaction timed out and has been rolled back", ex.getMessage());
        }
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(9, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testGlobalAfterLocal() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1 in local ctx");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2 in local ctx");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1 in global ctx");
        connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2 in global ctx");
        connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(21, orderedEvents.size());
        int i=0;
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());

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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }


    public void testDeferredReleaseAfterMarkedRollback() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** marking TX for rollback only");
        tm.setRollbackOnly();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug("*** rolling back");
        tm.rollback();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(9, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }


    public void testRollingBackSynchronization() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        final BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        tm.getTransaction().registerSynchronization(new Synchronization() {
            public void beforeCompletion() {
                try {
                    if (log.isDebugEnabled()) log.debug("**** before setRollbackOnly");
                    tm.setRollbackOnly();
                    if (log.isDebugEnabled()) log.debug("**** after setRollbackOnly");
                } catch (SystemException ex) {
                    throw new RuntimeException("could not setRollbackOnly", ex);
                }
            }
            public void afterCompletion(int status) {
            }
        });
        if (log.isDebugEnabled()) log.debug("*** after registerSynchronization");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        try {
            tm.commit();
            fail("transaction should not have been able to commit as it has been marked as rollback only");
        } catch (RollbackException ex) {
            assertEquals("transaction was marked as rollback only and has been rolled back", ex.getMessage());
        }
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(14, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++).getClass());
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }


    public void testSuspendResume() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** suspending transaction");
        Transaction tx = tm.suspend();
        if (log.isDebugEnabled()) log.debug("*** resuming transaction");
        tm.resume(tx);

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

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
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
        assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testLooseWorkingCaseOutsideOutside() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");
        connection1.createStatement();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
        int i=0;
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testLooseWorkingCaseOutsideInside() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");
        connection1.createStatement();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
        int i=0;
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testLooseWorkingCaseInsideOutside() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        connection1.createStatement();
        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        if (log.isDebugEnabled()) log.debug("*** committing");
        tm.commit();
        if (log.isDebugEnabled()) log.debug("*** TX is done");

        if (log.isDebugEnabled()) log.debug("*** closing connection 1");
        connection1.close();
        if (log.isDebugEnabled()) log.debug("*** closing connection 2");
        connection2.close();

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(17, orderedEvents.size());
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
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testHeuristicCommitWorkingCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        JdbcPooledConnection pc1 = handle.getPooledConnection();
            XAConnection mockXAConnection1 = (XAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) mockXAConnection1.getXAResource();
            mockXAResource.setCommitException(new XAException(XAException.XA_HEURCOM));
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        connection1.close();
        connection2.close();

        tm.commit();

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(18, orderedEvents.size());
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

        XAResourceCommitEvent event = ((XAResourceCommitEvent) orderedEvents.get(i++));
        assertEquals(false, event.isOnePhase());
        if (event.getException() != null) {
            assertNotNull(orderedEvents.get(i++));

            assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i)).isOnePhase());
            assertNull(((XAResourceCommitEvent) orderedEvents.get(i++)).getException());
        }
        else {
            assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i)).isOnePhase());
            assertNotNull(((XAResourceCommitEvent) orderedEvents.get(i++)).getException());

            assertNotNull(orderedEvents.get(i++));
        }

        assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }

    public void testHeuristicRollbackWorkingCase() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection connection1 = poolingDataSource1.getConnection();
        JdbcConnectionHandle handle = (JdbcConnectionHandle) Proxy.getInvocationHandler(connection1);
        JdbcPooledConnection pc1 = handle.getPooledConnection();
            XAConnection mockXAConnection1 = (XAConnection) getWrappedXAConnectionOf(pc1);
            MockXAResource mockXAResource = (MockXAResource) mockXAConnection1.getXAResource();
            mockXAResource.setRollbackException(new XAException(XAException.XA_HEURRB));
        connection1.createStatement();

        Connection connection2 = poolingDataSource2.getConnection();
        connection2.createStatement();

        connection1.close();
        connection2.close();

        tm.setTransactionTimeout(3);
        tm.rollback();

        // check flow
        List orderedEvents = EventRecorder.getOrderedEvents();
        log.info(EventRecorder.dumpToString());

        assertEquals(14, orderedEvents.size());
        int i=0;
        assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
        assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

        XAResourceRollbackEvent event = ((XAResourceRollbackEvent) orderedEvents.get(i++));
        assertNotNull(event);
        if (event.getException() != null) {
            assertNotNull(orderedEvents.get(i++));
            assertNotNull(orderedEvents.get(i++));
        }
        else {
            assertNotNull(orderedEvents.get(i));
            assertNotNull(((XAResourceRollbackEvent) orderedEvents.get(i++)).getException());
            assertNotNull(orderedEvents.get(i++));
        }
        assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
        assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
        assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl().getPoolingDataSource().getUniqueName());
    }


    public void testNonXaPool() throws Exception {
        for (int i=0; i<LOOPS ;i++) {
            TransactionManagerServices.getTransactionManager().begin();
            assertEquals(1, TransactionManagerServices.getTransactionManager().getInFlightTransactions().size());

            assertEquals(0, ((BitronixTransaction)TransactionManagerServices.getTransactionManager().getTransaction()).getResourceManager().size());
            Connection c = poolingDataSource1.getConnection();
            c.createStatement();
            c.close();
            assertEquals(1, ((BitronixTransaction)TransactionManagerServices.getTransactionManager().getTransaction()).getResourceManager().size());

            // rollback is necessary if deferConnectionRelease=true and to avoid nested TX
            TransactionManagerServices.getTransactionManager().rollback();
            assertEquals(0, TransactionManagerServices.getTransactionManager().getInFlightTransactions().size());
        }

        log.info(EventRecorder.dumpToString());

        List events = EventRecorder.getOrderedEvents();

        /* LOOPS * 9 events:
            JournalLogEvent ACTIVE
            ConnectionDequeuedEvent
            XAResourceStartEvent
            ConnectionCloseEvent
            XAResourceEndEvent
            JournalLogEvent ROLLINGBACK
            XAResourceRollbackEvent
            JournalLogEvent ROLLEDBACK
            ConnectionQueuedEvent
         */
        assertEquals(8 * LOOPS, events.size());
        for (int i = 0; i < 8 * LOOPS; ) {
            Event event;

            event = (Event) events.get(i++);
            assertEquals("at " + i, JournalLogEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, ConnectionDequeuedEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceStartEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceEndEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, JournalLogEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceRollbackEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, JournalLogEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, ConnectionQueuedEvent.class, event.getClass());
        }

    }


    public void testDuplicateClose() throws Exception {
        Field poolField = poolingDataSource1.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(poolingDataSource1);
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** getting connection");
        Connection c = poolingDataSource1.getConnection();
        assertEquals(POOL_SIZE -1, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** closing once");
        c.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** closing twice");
        c.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** checking pool size");
        Connection c1 = poolingDataSource1.getConnection();
        Connection c2 = poolingDataSource1.getConnection();
        Connection c3 = poolingDataSource1.getConnection();
        Connection c4 = poolingDataSource1.getConnection();
        Connection c5 = poolingDataSource1.getConnection();
        assertEquals(POOL_SIZE -5, pool.inPoolSize());

        c1.close();
        c2.close();
        c3.close();
        c4.close();
        c5.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** done");
    }

    public void testPoolBoundsWithLooseEnlistment() throws Exception {
        ArrayList list = new ArrayList();

        for (int i=0; i<LOOPS ;i++) {
            Thread t = new LooseTransactionThread(i, poolingDataSource1);
            list.add(t);
            t.start();
        }

        for (int i = 0; i < list.size(); i++) {
            LooseTransactionThread thread = (LooseTransactionThread) list.get(i);
            thread.join(5000);
            if (!thread.isSuccesful())
                log.info("thread " + thread.getNumber() + " failed");
        }

        assertEquals(LOOPS, LooseTransactionThread.successes);
        assertEquals(0, LooseTransactionThread.failures);

        LooseTransactionThread thread = new LooseTransactionThread(-1, poolingDataSource1);
        thread.run();
        assertTrue(thread.isSuccesful());
    }

    static class LooseTransactionThread extends Thread {

        static int successes = 0;
        static int failures = 0;

        private int number;
        private PoolingDataSource poolingDataSource;
        private boolean succesful = false;

        public LooseTransactionThread(int number, PoolingDataSource poolingDataSource) {
            this.number = number;
            this.poolingDataSource = poolingDataSource;
        }

        public void run() {
            try {
                UserTransaction ut = TransactionManagerServices.getTransactionManager();
                if (log.isDebugEnabled()) log.debug("*** getting connection - " + number);
                Connection c1 = poolingDataSource.getConnection();

                if (log.isDebugEnabled()) log.debug("*** beginning the transaction - " + number);
                ut.begin();

                c1.prepareStatement("");

                if (log.isDebugEnabled()) log.debug("*** committing the transaction - " + number);
                ut.commit();


                if (log.isDebugEnabled()) log.debug("*** closing connection - " + number);
                c1.close();

                if (log.isDebugEnabled()) log.debug("*** all done - " + number);

                synchronized (LooseTransactionThread.class) {
                    successes++;
                }
                succesful = true;

            } catch (Exception ex) {
                log.warn("*** catched exception, waiting 500ms - " + number, ex);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (log.isDebugEnabled()) log.debug("*** catched exception, waited 500ms - " + number, ex);
                synchronized (LooseTransactionThread.class) {
                    failures++;
                }
            }
        } // run

        public int getNumber() {
            return number;
        }

        public boolean isSuccesful() {
            return succesful;
        }

    }

    public void testNonEnlistingMethodInTxContext() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();
        
        Connection c = poolingDataSource1.getConnection();
        assertTrue(c.getAutoCommit());
        c.close();

        tm.commit();

        tm.shutdown();
    }

    public void testAutoCommitFalseWhenEnlisted() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();

        Connection c = poolingDataSource1.getConnection();
        c.prepareStatement("");
        assertFalse(c.getAutoCommit());
        c.close();

        tm.commit();

        tm.shutdown();
    }

    public void testAutoCommitTrueWhenEnlistedButSuspended() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        tm.begin();

        Connection c = poolingDataSource1.getConnection();
        c.prepareStatement("");

        Transaction tx = tm.suspend();
        assertNull(tm.getTransaction());

        assertTrue(c.getAutoCommit());

        tm.resume(tx);
        c.close();

        tm.commit();

        tm.shutdown();
    }

    public void testSerialization() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(poolingDataSource1);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        poolingDataSource1 = (PoolingDataSource) ois.readObject();
        ois.close();
    }

}
