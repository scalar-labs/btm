package bitronix.tm.mock;

import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.common.XAPool;
import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Field;

public class JdbcPoolTest extends TestCase {

    private PoolingDataSource pds;

    protected void setUp() throws Exception {
        pds = new PoolingDataSource();
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(2);
        pds.setMaxIdleTime(1);
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("pds");
        pds.setAllowLocalTransactions(true);
        pds.setAcquisitionTimeout(1);
        pds.init();
    }


    protected void tearDown() throws Exception {
        pds.close();
    }

    public void testPoolGrowth() throws Exception {
        Field poolField = pds.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pds);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        try {
            pds.getConnection();
            fail("should not be able to get a 3rd connection");
        } catch (SQLException ex) {
            assertEquals("unable to get a connection from pool of a PoolingDataSource containing an XAPool of resource pds with 2 connection(s) (0 still available)", ex.getMessage());
        }

        c1.close();
        c2.close();
        assertEquals(2, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());
    }

    public void testPoolShrink() throws Exception {
        Field poolField = pds.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pds);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        c1.close();
        c2.close();

        Thread.sleep(2500);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());
    }

}
