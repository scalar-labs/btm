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
package bitronix.tm;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 *
 * @author lorban
 */
public class RestartTest extends TestCase {


    protected void setUp() throws Exception {
        Iterator it = ResourceRegistrar.getResourcesUniqueNames().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            ResourceRegistrar.unregister(ResourceRegistrar.get(name));
        }
    }

    public void testRestartWithoutLoaderNoReuseResource() throws Exception {
        for (int i=0; i<3 ;i++) {
            PoolingDataSource pds = new PoolingDataSource();
            pds.setClassName(MockitoXADataSource.class.getName());
            pds.setUniqueName("ds");
            pds.setMaxPoolSize(1);
            pds.init();

            try {
                ResourceRegistrar.register(pds);
                fail("expected IllegalStateException");
            } catch (IllegalStateException ex) {
                String expected = "A resource with uniqueName 'ds' has already been registered";
                assertEquals(expected, ex.getMessage().substring(0, expected.length()));
            }

            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            tm.shutdown();
            assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());

            pds.close();
        }
    }

    public void testRestartWithoutLoaderReuseResource() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setUniqueName("ds");
        pds.setMaxPoolSize(1);
        pds.init();

        for (int i=0; i<3 ;i++) {
            try {
                ResourceRegistrar.register(pds);
                fail("expected IllegalStateException");
            } catch (IllegalStateException ex) {
                String expected = "A resource with uniqueName 'ds' has already been registered";
                assertEquals(expected, ex.getMessage().substring(0, expected.length()));
            }

            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            tm.shutdown();
            assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        }

        pds.close();
    }

    public void testRestartWithLoader() throws Exception {
        for (int i=0; i<3 ;i++) {
            String configFile = new File(getClass().getResource("RestartTest.properties").toURI()).getPath();
            TransactionManagerServices.getConfiguration().setResourceConfigurationFilename(configFile);
            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            assertEquals("at loop iteration #" + (i+1), 1, ResourceRegistrar.getResourcesUniqueNames().size());
            tm.shutdown();
            assertEquals("at loop iteration #" + (i+1), 0, ResourceRegistrar.getResourcesUniqueNames().size());
        }
    }

}
