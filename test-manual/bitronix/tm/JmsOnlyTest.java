package bitronix.tm;

import bitronix.tm.drivers.*;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import bitronix.tm.resource.jms.inbound.asf.BitronixServerSessionPool;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * (c) Bitronix, 20-oct.-2005
 *
 * @author lorban
 */
public class JmsOnlyTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(JmsOnlyTest.class);

    private PoolingConnectionFactory poolingConnectionFactory1;
    private String inboundQueueName;
    private String outboundQueueName;

    protected void setUp() throws Exception {
        // change transactionRetryInterval to 1 second
        Field field = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        field.setAccessible(true);
        field.set(TransactionManagerServices.getConfiguration(), new Integer(1));

        CountingMessageListener.receivedCount = 0;

        setUpJms_AMQ();

        TransactionManagerServices.getTransactionManager();
    }

    protected void tearDown() throws Exception {
        if (poolingConnectionFactory1 != null)
            poolingConnectionFactory1.close();
    }

    private void setUpJms_AMQ() throws Exception {
        if (poolingConnectionFactory1 != null)
            return;

        poolingConnectionFactory1 = AmqTest.getPoolingConnectionFactory1();
        inboundQueueName = "queue-testAutoEnlistment";
        outboundQueueName = "queue-testAutoEnlistment";
    }

    private void setUpJms_Swiftmq() throws Exception {
        if (poolingConnectionFactory1 != null)
            return;

        poolingConnectionFactory1 = SwiftmqTest.getPoolingConnectionFactory1();
        inboundQueueName = "queue-testAutoEnlistment@router1";
        outboundQueueName = "queue-testAutoEnlistment@router1";
    }

    private void setUpJms_Mantaray() throws Exception {
        if (poolingConnectionFactory1 != null)
            return;

        poolingConnectionFactory1 = MantarayTest.getPoolingConnectionFactory1();
        inboundQueueName = "queue-testAutoEnlistment";
        outboundQueueName = "queue-testAutoEnlistment";
    }

    public void testOutboundAutoEnlistment() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug(" ****** about to begin");
        tm.begin();
        tm.setTransactionTimeout(5);
        if (log.isDebugEnabled()) log.debug(" ****** TX is: " + tm.getTransaction());

        Connection connection1 = poolingConnectionFactory1.createConnection();

        for (int i=0;i<1;i++) {
            sendmsg(connection1);
        }

        if (log.isDebugEnabled()) log.debug(" ****** about to close connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug(" ****** about to commit");
        tm.commit();
    }

    public void testAsyncInbound() throws Exception {
        final int SEND_COUNT = 100;

        Connection connection1 = poolingConnectionFactory1.createConnection();
        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session1.createQueue(inboundQueueName);
        session1.close();

        BitronixServerSessionPool ssp = new BitronixServerSessionPool(poolingConnectionFactory1, CountingMessageListener.class, 3);

        connection1.createConnectionConsumer(queue, null, ssp, 1);
        log.info(" *** starting inbound connection");
        connection1.start();

        log.info(" *** sending messages");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();
        for (int i=0;i<SEND_COUNT;i++) {
            sendmsg(connection1);
        }
        tm.commit();
        log.info(" *** messages sent");
        Thread.sleep(1000 + (40 * SEND_COUNT));

        log.info(" *** closing server session pool");
        ssp.close();
        connection1.close();

        assertEquals(SEND_COUNT, CountingMessageListener.receivedCount);
        assertEquals(SEND_COUNT, CountingMessageListener.gtrids.size());
    }

    public void testSyncInbound() throws Exception {
        final int SEND_COUNT = 100;
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        log.info(" *** sending message");
        Connection connection1 = poolingConnectionFactory1.createConnection();
        tm.begin();
        for (int i=0;i<SEND_COUNT;i++) {
            sendmsg(connection1);
        }
        tm.commit();
        connection1.close();

        log.info(" *** receiving message");
        int receivedCount = 0;
        connection1 = poolingConnectionFactory1.createConnection();
        connection1.start();
        tm.begin();
        while (true) {
            Message message = recvmsg(connection1);
            if (message == null)
                break;

            log.info(" *** receiving message");
            BitronixTransaction bt = TransactionManagerServices.getTransactionManager().getCurrentTransaction();
            System.out.println("tx=" + bt);
            System.out.println("message: " + message.getClass());
            receivedCount++;
        }
        tm.commit();
        connection1.close();

        assertEquals(SEND_COUNT, receivedCount);
    }

    private Message recvmsg(Connection connection1) throws JMSException {
        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session1.createQueue(inboundQueueName);
        MessageConsumer consumer1 = session1.createConsumer(queue);

        try {
            return consumer1.receive(100);
        } finally {
            consumer1.close();
            session1.close();
        }
    }

    private void sendmsg(Connection connection1) throws JMSException {
        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session1.createQueue(outboundQueueName);
        MessageProducer producer1 = session1.createProducer(queue);

        producer1.send(session1.createTextMessage("testAutoEnlistment-producer1-" + new Date()));

        producer1.close();
        session1.close();
    }

    public static class CountingMessageListener implements MessageListener {
        private static int listenerCount = 0;
        public int listenerNumber;
        public static int receivedCount = 0;
        public static Set gtrids = Collections.synchronizedSet(new HashSet());

        public CountingMessageListener() {
            listenerNumber = ++listenerCount;
            log.info(" *** listener " + listenerNumber + " created");
        }

        public void onMessage(Message message) {
            log.info(" *** listener " + listenerNumber + " receiving message");
            BitronixTransaction bt = TransactionManagerServices.getTransactionManager().getCurrentTransaction();

            gtrids.add(bt.getResourceManager().getGtrid());

            System.out.println("tx=" + bt);
            System.out.println("message: " + message.getClass());
//                throw new RuntimeException("test must fail");
            receivedCount++;
        }
    }

}
