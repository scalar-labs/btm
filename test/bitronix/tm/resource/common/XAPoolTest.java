package bitronix.tm.resource.common;

import junit.framework.TestCase;
import bitronix.tm.*;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.utils.CryptoEngine;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 16-mrt-2006
 * Time: 18:27:34
 * To change this template use File | Settings | File Templates.
 */
public class XAPoolTest extends TestCase {

    public void testBuildXAFactory() throws Exception {
        ResourceBean rb = new ResourceBean() {};

        rb.setMaxPoolSize(1);
        rb.setClassName(MockitoXADataSource.class.getName());
        rb.getDriverProperties().setProperty("userName", "java");
        rb.getDriverProperties().setProperty("password", "{DES}" + CryptoEngine.crypt("DES", "java"));

        XAPool xaPool = new XAPool(null, rb);
        assertEquals(0, xaPool.totalPoolSize());
        assertEquals(0, xaPool.inPoolSize());

        MockitoXADataSource xads = (MockitoXADataSource) xaPool.getXAFactory();
        assertEquals("java", xads.getUserName());
        assertEquals("java", xads.getPassword());
    }

    public void testNoRestartOfTaskSchedulerDuringClose() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setMaxPoolSize(1);
        pds.setUniqueName("mock");
        pds.init();

        BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
        btm.shutdown();

        pds.close();

        assertFalse(TransactionManagerServices.isTaskSchedulerRunning());
    }

}