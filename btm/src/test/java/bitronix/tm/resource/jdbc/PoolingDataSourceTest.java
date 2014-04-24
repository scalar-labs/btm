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

    public void testEffectiveJdbc4ConnectionTimeoutWhenSet() {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setJdbc4ConnectionTestTimeout(10);
        assertEquals(10, pds.getEffectiveJdbc4ConnectionTestTimeout());
    }

    public void testEffectiveJdbc4ConnectionTimeoutWhenAcquisitionTimeoutSet() {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setAcquisitionTimeout(10);
        assertEquals(10, pds.getEffectiveJdbc4ConnectionTestTimeout());
    }

    public void testEffectiveJdbc4ConnectionTimeoutIsMinimumValue() {
        PoolingDataSource pds = new PoolingDataSource();

        pds.setJdbc4ConnectionTestTimeout(5);
        pds.setAcquisitionTimeout(10);
        assertEquals(5, pds.getEffectiveJdbc4ConnectionTestTimeout());

        pds.setAcquisitionTimeout(15);
        pds.setJdbc4ConnectionTestTimeout(20);
        assertEquals(15, pds.getEffectiveJdbc4ConnectionTestTimeout());
    }

}
