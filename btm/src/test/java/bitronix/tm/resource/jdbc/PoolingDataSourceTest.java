package bitronix.tm.resource.jdbc;

import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import junit.framework.TestCase;

import java.sql.Connection;

/**
 * @author Ludovic Orban
 */
public class PoolingDataSourceTest extends TestCase {

    public void testInjectedXaFactory() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        try {
            pds.setUniqueName("pds");
            pds.setMinPoolSize(1);
            pds.setMaxPoolSize(1);
            pds.setXaDataSource(new MockitoXADataSource());

            pds.init();

            Connection connection = pds.getConnection();

            connection.close();
        } finally {
            pds.close();
        }
    }

    public void testEffectiveConnectionTimeoutWhenSet() {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setConnectionTestTimeout(10);
        assertEquals(10, pds.getEffectiveConnectionTestTimeout());
    }

    public void testEffectiveConnectionTimeoutWhenAcquisitionTimeoutSet() {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setAcquisitionTimeout(10);
        assertEquals(10, pds.getEffectiveConnectionTestTimeout());
    }

    public void testEffectiveConnectionTimeoutIsMinimumValue() {
        PoolingDataSource pds = new PoolingDataSource();

        pds.setConnectionTestTimeout(5);
        pds.setAcquisitionTimeout(10);
        assertEquals(5, pds.getEffectiveConnectionTestTimeout());

        pds.setAcquisitionTimeout(15);
        pds.setConnectionTestTimeout(20);
        assertEquals(15, pds.getEffectiveConnectionTestTimeout());
    }

    public void testDefaultEffectiveAcquisitionTimeout() {
        PoolingDataSource pds = new PoolingDataSource();
        assertEquals(30, pds.getEffectiveConnectionTestTimeout());
    }

}
