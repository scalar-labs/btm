package bitronix.tm;

import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import java.sql.SQLException;

/**
 * (c) Bitronix
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
        poolingDataSource.setClassName(MockXADataSource.class.getName());
        poolingDataSource.setUniqueName("ds1");
        poolingDataSource.setMaxPoolSize(1);
        poolingDataSource.setMaxIdleTime(1); // set low shrink timeout
        poolingDataSource.init();

        IncrementalRecoverer.recover(poolingDataSource);

        MockXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));
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
        MockXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));

        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(MockXADataSource.class.getName());
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


        MockXADataSource.setStaticGetXAConnectionException(null);

        recoverer.run();
        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 1 connection(s) (1 still available)", poolingDataSource.toString());
        // recoverer must not unregister the resource
        assertSame(poolingDataSource, ResourceRegistrar.get("ds1"));


        poolingDataSource.close();
    }

    public void testSuccessfulRecoveryMarksAsNotFailed() throws Exception {
        MockXADataSource.setStaticGetXAConnectionException(new SQLException("creating a new connection does not work"));

        PoolingDataSource poolingDataSource = new PoolingDataSource();
        poolingDataSource.setClassName(MockXADataSource.class.getName());
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


        MockXADataSource.setStaticGetXAConnectionException(null);

        Recoverer recoverer = new Recoverer();
        recoverer.run();
        assertEquals("a PoolingDataSource containing an XAPool of resource ds1 with 1 connection(s) (1 still available)", poolingDataSource.toString());
        // recoverer must not unregister the resource
        assertSame(poolingDataSource, ResourceRegistrar.get("ds1"));

        poolingDataSource.close();
    }
}
