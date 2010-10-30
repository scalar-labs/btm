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
package bitronix.tm;

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
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException ex) {
                assertEquals("resource with uniqueName 'ds' has already been registered", ex.getMessage());
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
                fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException ex) {
                assertEquals("resource with uniqueName 'ds' has already been registered", ex.getMessage());
            }

            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            tm.shutdown();
            assertEquals(1, ResourceRegistrar.getResourcesUniqueNames().size());
        }

        pds.close();
    }

    public void testRestartWithLoader() throws Exception {
        for (int i=0; i<3 ;i++) {
            TransactionManagerServices.getConfiguration().setResourceConfigurationFilename(getClass().getResource("RestartTest.properties").getFile());
            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            assertEquals("at loop iteration #" + (i+1), 1, ResourceRegistrar.getResourcesUniqueNames().size());
            tm.shutdown();
            assertEquals("at loop iteration #" + (i+1), 0, ResourceRegistrar.getResourcesUniqueNames().size());
        }
    }

}
