package bitronix.tm.integration.cdi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;

import com.oneandone.ejbcdiunit.EjbUnitRunner;
import org.jglue.cdiunit.AdditionalClasses;
import org.junit.Test;
import org.junit.runner.RunWith;
import bitronix.tm.resource.jdbc.PoolingDataSource;

@RunWith(EjbUnitRunner.class)
public class PoolingDataSourceFactoryBeanTest {

    @Inject
    private DataSource2 dataSource2;
    
    @Test
    public void validateProperties() {
        assertEquals("btm-spring-test-ds2", dataSource2.getUniqueName());
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", dataSource2.getClassName());
        assertEquals(1, dataSource2.getMinPoolSize());
        assertEquals(2, dataSource2.getMaxPoolSize());
        assertEquals(true, dataSource2.getAutomaticEnlistingEnabled());
        assertEquals(false, dataSource2.getUseTmJoin());
        assertEquals(60, dataSource2.getMaxIdleTime()); // default value not overridden in bean configuration
        assertEquals("5", dataSource2.getDriverProperties().get("loginTimeout"));
    }
    
    @Test
    public void validateConnection() throws Exception {
        Connection connection = null;
        try {
            connection = dataSource2.getObject().getConnection();
            assertNotNull(connection);
        }
        finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
