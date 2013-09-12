package bitronix.tm.integration.spring;

import static junit.framework.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import bitronix.tm.resource.jdbc.PoolingDataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
public class PoolingDataSourceFactoryBeanTest {

    @Inject @Named("dataSource2")
    private PoolingDataSource dataSource2;
    
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
    public void validateConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource2.getConnection();
            assertNotNull(connection);
        }
        finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
