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
package bitronix.tm.resource;

import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.utils.PropertyUtils;
import junit.framework.TestCase;

import javax.sql.XADataSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Ludovic Orban
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
        p.setProperty("resource.ds1.driverProperties.clonedProperties.a.key", "1000");
        p.setProperty("resource.ds1.driverProperties.clonedProperties.b.key", "2000");


        loader.initXAResourceProducers(p);
        Map dataSources = loader.getResources();

        assertEquals(1, dataSources.size());
        String uniqueName = (String) dataSources.keySet().iterator().next();
        assertEquals("dataSource1", uniqueName);
        PoolingDataSource pds = (PoolingDataSource) dataSources.get(uniqueName);
        assertEquals("bitronix.tm.mock.resource.jdbc.MockitoXADataSource", pds.getClassName());
        assertEquals("dataSource1", pds.getUniqueName());
        assertEquals(123, pds.getMaxPoolSize());
        assertEquals(5, pds.getDriverProperties().size());

        MockitoXADataSource mockXA = pds.unwrap(MockitoXADataSource.class);
        assertNotNull(mockXA);

        Properties clonedProperties = mockXA.getClonedProperties();
        assertNotNull(clonedProperties);
        assertEquals(2, clonedProperties.size());
        assertEquals("1000", clonedProperties.getProperty("a.key"));
        assertEquals("2000", clonedProperties.getProperty("b.key"));
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
