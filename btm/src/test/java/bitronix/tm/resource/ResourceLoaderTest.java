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
package bitronix.tm.resource;

import java.lang.reflect.Field;
import java.util.*;

import javax.sql.XADataSource;

import junit.framework.TestCase;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.utils.PropertyUtils;

/**
 *
 * @author lorban
 */
public class ResourceLoaderTest extends TestCase {

    public void testBindOneJdbc() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds1.uniqueName", "dataSource1");
        p.setProperty("resource.ds1.maxPoolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.userName", "java");
        p.setProperty("resource.ds1.driverProperties.password", "java");
        p.setProperty("resource.ds1.driverProperties.database", "users1");


        loader.initXAResourceProducers(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String uniqueName = (String) dataSources.keySet().iterator().next();
        assertEquals("dataSource1", uniqueName);
        PoolingDataSource pds = (PoolingDataSource) dataSources.get(uniqueName);
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", pds.getClassName());
        assertEquals("dataSource1", pds.getUniqueName());
        assertEquals(123, pds.getMaxPoolSize());
        assertEquals(3, pds.getDriverProperties().size());

    }


    public void testDecryptPassword() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds1.uniqueName", "dataSource10");
        p.setProperty("resource.ds1.maxPoolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.userName", "java");
        p.setProperty("resource.ds1.driverProperties.password", "{DES}UcXKog312decCrwu51xGmw==");
        p.setProperty("resource.ds1.driverProperties.database", "users1");


        loader.initXAResourceProducers(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String uniqueName = (String) dataSources.keySet().iterator().next();
        assertEquals("dataSource10", uniqueName);
        PoolingDataSource pds = (PoolingDataSource) dataSources.get(uniqueName);
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", pds.getClassName());
        assertEquals("dataSource10", pds.getUniqueName());
        assertEquals(123, pds.getMaxPoolSize());
        assertEquals(3, pds.getDriverProperties().size());
        String decryptedPassword = (String) PropertyUtils.getProperty(getXADataSource(pds), "password");
        assertEquals("java", decryptedPassword);
    }

    protected XADataSource getXADataSource(PoolingDataSource poolingDataSource) throws NoSuchFieldException, IllegalAccessException {
        Field field = PoolingDataSource.class.getDeclaredField("xaDataSource");
        field.setAccessible(true);
        return (XADataSource) field.get(poolingDataSource);
    }

    public void testBindOneJms() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockXAConnectionFactory.class.getName());
        p.setProperty("resource.ds1.uniqueName", "mq1");
        p.setProperty("resource.ds1.maxPoolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.endpoint", "tcp://somewhere");


        loader.initXAResourceProducers(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String uniqueName = (String) dataSources.keySet().iterator().next();
        assertEquals("mq1", uniqueName);
        PoolingConnectionFactory pcf = (PoolingConnectionFactory) dataSources.get(uniqueName);
        assertEquals("bitronix.tm.mock.resource.jms.MockXAConnectionFactory", pcf.getClassName());
        assertEquals("mq1", pcf.getUniqueName());
        assertEquals(123, pcf.getMaxPoolSize());
        assertEquals(1, pcf.getDriverProperties().size());

    }

    public void testBind2WithSomeDefaults() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds1.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds1.uniqueName", "dataSource2");
        p.setProperty("resource.ds1.maxPoolSize", "123");
        p.setProperty("resource.ds1.automaticEnlistingEnabled", "true");
        p.setProperty("resource.ds1.useTmJoin", "false");
        p.setProperty("resource.ds1.deferConnectionRelease", "true");
        p.setProperty("resource.ds1.driverProperties.userName", "java");
        p.setProperty("resource.ds1.driverProperties.password", "java");
        p.setProperty("resource.ds1.driverProperties.database", "users1");

        p.setProperty("resource.ds2.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.unique.Name");
        p.setProperty("resource.ds2.maxPoolSize", "123");

        loader.initXAResourceProducers(p);
        Map dataSources = loader.getResources();

        assertEquals(2, dataSources.size());
        PoolingDataSource pds = (PoolingDataSource) dataSources.get("dataSource2");
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", pds.getClassName());
        assertEquals("dataSource2", pds.getUniqueName());
        assertEquals(123, pds.getMaxPoolSize());
        assertEquals(3, pds.getDriverProperties().size());

        pds = (PoolingDataSource) dataSources.get("some.unique.Name");
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", pds.getClassName());
        assertEquals("some.unique.Name", pds.getUniqueName());
        assertEquals(123, pds.getMaxPoolSize());
        assertEquals(true, pds.getDeferConnectionRelease());
        assertEquals(true, pds.getAutomaticEnlistingEnabled());
        assertEquals(true, pds.getUseTmJoin());
        assertEquals(0, pds.getDriverProperties().size());
    }

    public void testConfigErrors() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.className", "some.class.Name");

            loader.initXAResourceProducers(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("cannot configure resource for configuration entries with name [ds2] - failing property is [className]", ex.getMessage());
            assertEquals(ClassNotFoundException.class, ex.getCause().getClass());
            assertEquals("some.class.Name", ex.getCause().getMessage());
        }

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.className", MockitoXADataSource.class.getName());

            loader.initXAResourceProducers(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("missing mandatory property [uniqueName] of resource [ds2] in resources configuration file", ex.getMessage());
        }

        try {
            Properties p = new Properties();
            p.setProperty("resource.ds2.jndiName", "some.jndi.Name");

            loader.initXAResourceProducers(p);
            fail("should have thrown ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("missing mandatory property [className] for resource [ds2] in resources configuration file", ex.getMessage());
        }

        Properties p = new Properties();
        p.setProperty("resource.ds2.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.other.unique.Name");
        p.setProperty("resource.ds2.maxPoolSize", "123");

        loader.initXAResourceProducers(p);
    }

    public void testFormatErrors() throws Exception {
        ResourceLoader loader = new ResourceLoader();

        Properties p = new Properties();
        p.setProperty("resource.ds2.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.more.unique.Name");
        p.setProperty("resource.ds2.maxPoolSize", "abc"); // incorrect format

        try {
            loader.initXAResourceProducers(p);
            fail("expected ResourceConfigurationException");
        } catch (ResourceConfigurationException ex) {
            assertEquals("cannot configure resource for configuration entries with name [ds2] - failing property is [maxPoolSize]", ex.getMessage());
        }

        p.setProperty("resource.ds2.className", MockitoXADataSource.class.getName());
        p.setProperty("resource.ds2.uniqueName", "some.also.other.unique.Name");
        p.setProperty("resource.ds2.maxPoolSize", "123");
        p.setProperty("resource.ds2.useTmJoin", "unknown"); // incorrect format, will default to false
        loader.initXAResourceProducers(p);


        PoolingDataSource pds = (PoolingDataSource) loader.getResources().get("some.also.other.unique.Name");
        assertFalse(pds.getUseTmJoin());
    }
}
