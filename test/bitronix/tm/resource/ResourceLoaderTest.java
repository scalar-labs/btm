package bitronix.tm.resource;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Properties;

import bitronix.tm.resource.jdbc.DataSourceBean;
import bitronix.tm.resource.jms.ConnectionFactoryBean;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 16-mrt-2006
 * Time: 18:27:34
 * To change this template use File | Settings | File Templates.
 */
public class ResourceLoaderTest extends TestCase {

    public void testBindOneJdbc() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds1.uniqueName", "dataSource1");
        p.setProperty("resource.ds1.poolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.userName", "java");
        p.setProperty("resource.ds1.driverProperties.password", "java");
        p.setProperty("resource.ds1.driverProperties.database", "users1");


        loader.initResourceBeans(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String confName = (String) dataSources.keySet().iterator().next();
        assertEquals("ds1", confName);
        DataSourceBean bean = (DataSourceBean) dataSources.get(confName);
        assertEquals("bitronix.tm.mock.resource.jdbc.MockXADataSource", bean.getClassName());
        assertEquals("dataSource1", bean.getUniqueName());
        assertEquals(123, bean.getPoolSize());
        assertEquals(3, bean.getDriverProperties().size());

    }

    public void testBindOneJms() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockXAConnectionFactory.class.getName());
        p.setProperty("resource.ds1.uniqueName", "mq1");
        p.setProperty("resource.ds1.poolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.endpoint", "tcp://somewhere");


        loader.initResourceBeans(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String confName = (String) dataSources.keySet().iterator().next();
        assertEquals("ds1", confName);
        ConnectionFactoryBean bean = (ConnectionFactoryBean) dataSources.get(confName);
        assertEquals("bitronix.tm.mock.resource.jms.MockXAConnectionFactory", bean.getClassName());
        assertEquals("mq1", bean.getUniqueName());
        assertEquals(123, bean.getPoolSize());
        assertEquals(1, bean.getDriverProperties().size());

    }

    public void testBind2WithSomeDefaults() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds1.uniqueName", "dataSource2");
        p.setProperty("resource.ds1.poolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.userName", "java");
        p.setProperty("resource.ds1.driverProperties.password", "java");
        p.setProperty("resource.ds1.driverProperties.database", "users1");

        p.setProperty("resource.ds2.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.unique.Name");
        p.setProperty("resource.ds2.poolSize", "123");

        loader.initResourceBeans(p);
        Map dataSources = loader.getResources();

        assertEquals(2, dataSources.size());
        DataSourceBean bean = (DataSourceBean) dataSources.get("ds1");
        assertEquals("ds1", bean.getConfigurationName());
        assertEquals("bitronix.tm.mock.resource.jdbc.MockXADataSource", bean.getClassName());
        assertEquals("dataSource2", bean.getUniqueName());
        assertEquals(123, bean.getPoolSize());
        assertEquals(3, bean.getDriverProperties().size());

        bean = (DataSourceBean) dataSources.get("ds2");
        assertEquals("ds2", bean.getConfigurationName());
        assertEquals("bitronix.tm.mock.resource.jdbc.MockXADataSource", bean.getClassName());
        assertEquals("some.unique.Name", bean.getUniqueName());
        assertEquals(123, bean.getPoolSize());
        assertEquals(true, bean.getDeferConnectionRelease());
        assertEquals(true, bean.getAutomaticEnlistingEnabled());
        assertEquals(true, bean.getUseTmJoin());
        assertEquals(0, bean.getDriverProperties().size());
    }

    public void testConfigErrors() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.className", "some.class.Name");

            loader.initResourceBeans(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("cannot configure resource for configuration entries with name <ds2> - failing property is <className>", ex.getMessage());
            assertEquals(ClassNotFoundException.class, ex.getCause().getClass());
            assertEquals("some.class.Name", ex.getCause().getMessage());
        }

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.className", MockXADataSource.class.getName());

            loader.initResourceBeans(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("missing mandatory property <uniqueName> for resource <ds2> in resources configuration file", ex.getMessage());
        }

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.jndiName", "some.jndi.Name");

            loader.initResourceBeans(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("missing mandatory property <className> for resource <ds2> in resources configuration file", ex.getMessage());
        }

        Properties p = new Properties();
        p.setProperty("resource.ds2.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.other.unique.Name");
        p.setProperty("resource.ds2.poolSize", "123");

        loader.initResourceBeans(p);
    }

    public void testFormatErrors() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds2.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.more.unique.Name");
        p.setProperty("resource.ds2.poolSize", "abc"); // incorrect format

        try {
            loader.initResourceBeans(p);
            fail("expected ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("cannot configure resource for configuration entries with name <ds2> - failing property is <poolSize>", ex.getMessage());
        }

        p.setProperty("resource.ds2.className", MockXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.also.other.unique.Name");
        p.setProperty("resource.ds2.poolSize", "123");
        p.setProperty("resource.ds2.useTmJoin", "unknown"); // incorrect format, will default to false
        loader.initResourceBeans(p);


        DataSourceBean dsb = (DataSourceBean) loader.getResources().get("ds2");
        assertFalse(dsb.getUseTmJoin());
    }
}
