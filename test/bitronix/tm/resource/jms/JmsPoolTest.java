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
        this.pcf = new PoolingConnectionFactory();
        pcf.setClassName(MockXAConnectionFactory.class.getName());
        pcf.setUniqueName("pcf");
        pcf.setPoolSize(POOL_SIZE);
        pcf.init();
    }

    protected void tearDown() throws Exception {
        pcf.close();
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
