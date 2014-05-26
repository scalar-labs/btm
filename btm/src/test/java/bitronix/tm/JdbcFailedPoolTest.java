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
package bitronix.tm;

import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import java.sql.SQLException;

/**
 *
 * @author Ludovic Orban
 */
public class JdbcFailedPoolTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        TransactionManagerServices.getJournal().open();
        TransactionManagerServices.getTaskScheduler();
    }

    @Override
    protected void tearDown() throws Exception {
        TransactionManagerServices.getJournal().close();
        TransactionManagerServices.getTaskScheduler().shutdown();

        MockitoXADataSource.setStaticGetXAConnectionException(null);
    }

    public void testAcquiringConnectionAfterRecoveryDoesNotMarkAsFailed() throws Exception {
        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource.setUniqueName("ds1");
        poolingDataSource.setMaxPoolSize(1);
        poolingDataSource.setMaxIdleTime(1); // set low shrink timeout
        poolingDataSource.init();

        IncrementalRecoverer.recover(poolingDataSource);

        MockitoXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));
        Thread.sleep(2000); // wait for shrink

        // should not work but should not mark the pool as failed as it could recover
        try {
            poolingDataSource.getConnection();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("unable to get a connection from pool of a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available)", ex.getMessage());
        }

        poolingDataSource.close();
    }

    public void testFailingRecoveryMarksAsFailed() throws Exception {
        MockitoXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));

        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource.setUniqueName("ds1");
        poolingDataSource.setMaxPoolSize(1);
        poolingDataSource.init();

        Recoverer recoverer = new Recoverer();
        recoverer.run();
        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available) -failed-", poolingDataSource.toString());
        // recoverer must not unregister the resource
        assertSame(poolingDataSource, ResourceRegistrar.get("ds1"));

        MockitoXADataSource.setStaticGetXAConnectionException(null);

        recoverer.run();
        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 1 connection(s) (1 still available)", poolingDataSource.toString());
        // recoverer must not unregister the resource
        assertSame(poolingDataSource, ResourceRegistrar.get("ds1"));

        poolingDataSource.close();
    }

    public void testSuccessfulRecoveryMarksAsNotFailed() throws Exception {
        MockitoXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));

        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(MockitoXADataSource.class.getName());
        poolingDataSource.setUniqueName("ds1");
        poolingDataSource.setMaxPoolSize(1);
        poolingDataSource.init();

        try {
            IncrementalRecoverer.recover(poolingDataSource);
            fail("expected RecoveryException");
        } catch (RecoveryException ex) {
            assertEquals("cannot start recovery on a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available)", ex.getMessage());
        }

        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available) -failed-", poolingDataSource.toString());

        MockitoXADataSource.setStaticGetXAConnectionException(null);

        Recoverer recoverer = new Recoverer();
        recoverer.run();
        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 1 connection(s) (1 still available)", poolingDataSource.toString());
        // recoverer must not unregister the resource
        assertSame(poolingDataSource, ResourceRegistrar.get("ds1"));

        poolingDataSource.close();
    }
}
