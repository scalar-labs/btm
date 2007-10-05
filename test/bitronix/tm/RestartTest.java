package bitronix.tm;

import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import java.util.Iterator;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
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
            pds.setClassName(MockXADataSource.class.getName());
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
        pds.setClassName(MockXADataSource.class.getName());
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
            TransactionManagerServices.getConfiguration().setResourceConfigurationFilename("test/" + getClass().getName().replace('.', '/') + ".properties");
            BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
            assertEquals("at loop iteration #" + (i+1), 1, ResourceRegistrar.getResourcesUniqueNames().size());
            tm.shutdown();
            assertEquals("at loop iteration #" + (i+1), 0, ResourceRegistrar.getResourcesUniqueNames().size());
        }
    }

}
