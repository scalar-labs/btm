package bitronix.tm.resource.common;

import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import bitronix.tm.utils.CryptoEngine;
import junit.framework.TestCase;

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
        rb.setClassName(MockXADataSource.class.getName());
        rb.getDriverProperties().setProperty("userName", "java");
        rb.getDriverProperties().setProperty("password", "{DES}" + CryptoEngine.crypt("DES", "java"));

        XAPool xaPool = new XAPool(null, rb);
        assertEquals(0, xaPool.totalPoolSize());
        assertEquals(0, xaPool.inPoolSize());

        MockXADataSource xads = (MockXADataSource) xaPool.getXAFactory();
        assertEquals("java", xads.getUserName());
        assertEquals("java", xads.getPassword());
    }

}