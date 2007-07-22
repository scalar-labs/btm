package bitronix.tm.resource.jms;

import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import junit.framework.TestCase;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class JmsPoolTest extends TestCase {

    private static final int POOL_SIZE = 5;

    private PoolingConnectionFactory pcf;

    protected void setUp() throws Exception {
        ConnectionFactoryBean cfb = new ConnectionFactoryBean();
        cfb.setClassName(MockXAConnectionFactory.class.getName());
        cfb.setUniqueName("pcf");
        cfb.setPoolSize(POOL_SIZE);

        pcf = (PoolingConnectionFactory) cfb.createResource();
    }

    
    public void testSerialization() throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("test-jms-pool.ser"));
        oos.writeObject(pcf);
        oos.close();

        pcf.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("test-jms-pool.ser"));
        pcf = (PoolingConnectionFactory) ois.readObject();
        ois.close();
    }

}
