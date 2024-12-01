/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.mock;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.events.ConnectionDequeuedEvent;
import bitronix.tm.mock.events.ConnectionQueuedEvent;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.common.XAStatefulHolder.State;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Ludovic Orban
 */
public abstract class AbstractMockJdbcTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(AbstractMockJdbcTest.class);

    protected PoolingDataSource poolingDataSource1;
    protected PoolingDataSource poolingDataSource2;
    protected static final int POOL_SIZE = 5;
    protected static final String DATASOURCE1_NAME = "pds1";
    protected static final String DATASOURCE2_NAME = "pds2";

    @Override
    protected void setUp() throws Exception {
        // clear event recorder list
        EventRecorder.clear();
       
        Iterator<String> it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = it.next();
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
        }

        // DataSource1 has shared accessible connections
        poolingDataSource1 = new PoolingDataSource();
        poolingDataSource1.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource1.setUniqueName(DATASOURCE1_NAME);
        poolingDataSource1.setMinPoolSize(POOL_SIZE);
        poolingDataSource1.setMaxPoolSize(POOL_SIZE);
        poolingDataSource1.setAllowLocalTransactions(true);
        poolingDataSource1.setShareTransactionConnections(true);
        poolingDataSource1.setPreparedStatementCacheSize(10);
        poolingDataSource1.init();

        // DataSource2 does not have shared accessible connections
        poolingDataSource2 = new PoolingDataSource();
        poolingDataSource2.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource2.setUniqueName(DATASOURCE2_NAME);
        poolingDataSource2.setMinPoolSize(POOL_SIZE);
        poolingDataSource2.setMaxPoolSize(POOL_SIZE);
        poolingDataSource2.setAllowLocalTransactions(true);
        poolingDataSource2.init();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journalRef");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());

        // change connection pools into mock pools
        XAPool<JdbcPooledConnection, JdbcPooledConnection> p1 = getPool(this.poolingDataSource1);
        registerPoolEventListener(p1);
        XAPool<JdbcPooledConnection, JdbcPooledConnection> p2 = getPool(this.poolingDataSource2);
        registerPoolEventListener(p2);

        TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(2);
    }

    @SuppressWarnings("unchecked")
    protected XAPool<JdbcPooledConnection, JdbcPooledConnection> getPool(PoolingDataSource poolingDataSource) throws NoSuchFieldException, IllegalAccessException {
        Field poolField = PoolingDataSource.class.getDeclaredField("pool");
        poolField.setAccessible(true);
        return (XAPool<JdbcPooledConnection, JdbcPooledConnection>) poolField.get(poolingDataSource);
    }

    private void registerPoolEventListener(XAPool<JdbcPooledConnection, JdbcPooledConnection> pool) throws Exception {
        Iterator<JdbcPooledConnection> iterator = pool.getXAResourceHolders().iterator();
        while (iterator.hasNext()) {
        	JdbcPooledConnection jdbcPooledConnection = iterator.next();
            jdbcPooledConnection.addStateChangeEventListener(new StateChangeListener<JdbcPooledConnection>() {
                @Override
                public void stateChanged(JdbcPooledConnection source, State oldState, State newState) {
                    if (newState == State.IN_POOL)
                        EventRecorder.getEventRecorder(this).addEvent(new ConnectionQueuedEvent(this, source));
                    if (newState == State.ACCESSIBLE)
                        EventRecorder.getEventRecorder(this).addEvent(new ConnectionDequeuedEvent(this, source));
                }

                @Override
                public void stateChanging(JdbcPooledConnection source, State currentState, State futureState) {
                }
            });
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (log.isDebugEnabled()) { log.debug("*** tearDown rollback"); }
            TransactionManagerServices.getTransactionManager().rollback();
        } catch (Exception ex) {
            // ignore
        }
        poolingDataSource1.close();
        poolingDataSource2.close();

        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public static Object getWrappedXAConnectionOf(Object pc1) throws NoSuchFieldException, IllegalAccessException {
        Field f = pc1.getClass().getDeclaredField("xaConnection");
        f.setAccessible(true);
        return f.get(pc1);
    }
}
