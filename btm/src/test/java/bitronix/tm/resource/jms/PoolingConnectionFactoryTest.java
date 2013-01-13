package bitronix.tm.resource.jms;

import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import junit.framework.TestCase;

import javax.jms.Connection;


/**
 * @author Ludovic Orban
 */
public class PoolingConnectionFactoryTest extends TestCase {

    public void testInjectedXaFactory() throws Exception {
        PoolingConnectionFactory pcf = new PoolingConnectionFactory();
        try {
            pcf.setUniqueName("pcf");
            pcf.setMinPoolSize(1);
            pcf.setMaxPoolSize(1);
            pcf.setXaConnectionFactory(new MockXAConnectionFactory());

            pcf.init();

            Connection connection = pcf.createConnection();
            connection.close();
        } finally {
            pcf.close();
        }
    }

}
