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
package bitronix.tm;

import java.sql.SQLException;

import junit.framework.TestCase;
import bitronix.tm.mock.resource.jdbc.*;
import bitronix.tm.recovery.*;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 *
 * @author lorban
 */
public class JdbcFailedPoolTest extends TestCase {

    protected void setUp() throws Exception {
        TransactionManagerServices.getJournal().open();
        TransactionManagerServices.getTaskScheduler();
    }

    protected void tearDown() throws Exception {
        TransactionManagerServices.getJournal().close();
        TransactionManagerServices.getTaskScheduler().shutdown();
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

//        try {
//            IncrementalRecoverer.recover(poolingDataSource);
//            fail("expected RecoveryException");
//        } catch (RecoveryException ex) {
//            assertEquals("cannot start recovery on a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available)", ex.getMessage());
//        }
//
//        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 0 connection(s) (0 still available) -failed-", poolingDataSource.toString());

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
