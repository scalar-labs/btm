package bitronix.tm.mock;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.resource.jms.ConnectionFactoryBean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 * User: OrbanL
 * Date: 3-sep-2006
 * Time: 14:59:31
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractMockJmsTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(AbstractMockJmsTest.class);

    protected PoolingConnectionFactory poolingConnectionFactory1;
    protected PoolingConnectionFactory poolingConnectionFactory2;
    protected static final int POOL_SIZE = 5;
    protected static final String CONNECTION_FACTORY1_NAME = "pcf1";
    protected static final String CONNECTION_FACTORY2_NAME = "pcf2";

    protected void setUp() throws Exception {
        ConnectionFactoryBean cfb1 = new ConnectionFactoryBean();
        cfb1.setClassName(MockXAConnectionFactory.class.getName());
        cfb1.setUniqueName(CONNECTION_FACTORY1_NAME);
        cfb1.setAcquisitionTimeout(5);
        cfb1.setPoolSize(POOL_SIZE);
        poolingConnectionFactory1 = (PoolingConnectionFactory) cfb1.createResource();

        ConnectionFactoryBean cfb2 = new ConnectionFactoryBean();
        cfb2.setClassName(MockXAConnectionFactory.class.getName());
        cfb2.setUniqueName(CONNECTION_FACTORY2_NAME);
        cfb2.setAcquisitionTimeout(5);
        cfb2.setPoolSize(POOL_SIZE);
        poolingConnectionFactory2 = (PoolingConnectionFactory) cfb2.createResource();

        // change disk journal into mock journal
        Field journalField = TransactionManagerServices.class.getDeclaredField("journal");
        journalField.setAccessible(true);
        journalField.set(TransactionManagerServices.class, new MockJournal());

        // change transactionRetryInterval to 1 second
        Field transactionRetryIntervalField = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        transactionRetryIntervalField.setAccessible(true);
        transactionRetryIntervalField.set(TransactionManagerServices.getConfiguration(), new Integer(1));

        // start TM
        TransactionManagerServices.getTransactionManager();

        // clear event recorder list
        EventRecorder.clear();
    }
    protected void tearDown() throws Exception {
        try {
            if (log.isDebugEnabled()) log.debug("*** tearDown rollback");
            TransactionManagerServices.getTransactionManager().rollback();
        } catch (Exception ex) {
            // ignore
        }
        poolingConnectionFactory1.close();
        poolingConnectionFactory2.close();
    }

}
