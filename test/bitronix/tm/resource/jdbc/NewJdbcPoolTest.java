package bitronix.tm.resource.jdbc;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.jdbc.MockXADataSource;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.UserTransaction;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class NewJdbcPoolTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(NewJdbcPoolTest.class);

    private static final int LOOPS = 2;
    private static final int POOL_SIZE = 5;

    private PoolingDataSource pds;

    protected void setUp() throws Exception {
        this.pds = new PoolingDataSource();
        pds.setClassName(MockXADataSource.class.getName());
        pds.setUniqueName("pds");
        pds.setPoolSize(POOL_SIZE);
        pds.init();
    }

    protected void tearDown() throws Exception {
        pds.close();
    }

    public void testNonXaPool() throws Exception {
        for (int i=0; i<LOOPS ;i++) {
            TransactionManagerServices.getTransactionManager().begin();
            assertEquals(1, TransactionManagerServices.getTransactionManager().getInFlightTransactions().size());

            assertEquals(0, ((BitronixTransaction)TransactionManagerServices.getTransactionManager().getTransaction()).getResourceManager().size());
            Connection c = pds.getConnection();
            c.createStatement();
            c.close();
            assertEquals(1, ((BitronixTransaction)TransactionManagerServices.getTransactionManager().getTransaction()).getResourceManager().size());

            // rollback is necessary if deferConnectionRelease=true and to avoid nested TX
            TransactionManagerServices.getTransactionManager().rollback();
            assertEquals(0, TransactionManagerServices.getTransactionManager().getInFlightTransactions().size());
        }

//        System.out.println(EventRecorder.dumpToString());

        List events = EventRecorder.getOrderedEvents();
        // LOOPS * 4 events: XAResourceStartEvent, XAResourceEndEvent, ConnectionCloseEvent, XAResourceRollbackEvent
        // +1 for closing the connection used for recovery
        assertEquals(4 * LOOPS +1, events.size());
        for (int i = 1; i < 4 * LOOPS +1; ) {
            Event event;

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceStartEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, ConnectionCloseEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceEndEvent.class, event.getClass());

            event = (Event) events.get(i++);
            assertEquals("at " + i, XAResourceRollbackEvent.class, event.getClass());
        }

    }


    public void testDuplicateClose() throws Exception {
        Field poolField = pds.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pds);
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** getting connection");
        Connection c = pds.getConnection();
        assertEquals(POOL_SIZE -1, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** closing once");
        c.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** closing twice");
        c.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** checking pool size");
        Connection c1 = pds.getConnection();
        Connection c2 = pds.getConnection();
        Connection c3 = pds.getConnection();
        Connection c4 = pds.getConnection();
        Connection c5 = pds.getConnection();
        assertEquals(POOL_SIZE -5, pool.inPoolSize());

        c1.close();
        c2.close();
        c3.close();
        c4.close();
        c5.close();
        assertEquals(POOL_SIZE, pool.inPoolSize());

        if (log.isDebugEnabled()) log.debug(" *** done");
    }


    public void testPoolBoundsWithLooseEnlistment() throws Exception {
        ArrayList list = new ArrayList();

        for (int i=0; i<LOOPS ;i++) {
            Thread t = new LooseTransactionThread(i, pds);
            list.add(t);
            t.start();
        }

        for (int i = 0; i < list.size(); i++) {
            LooseTransactionThread thread = (LooseTransactionThread) list.get(i);
            thread.join(5000);
            if (!thread.isSuccesful())
                log.info("thread " + thread.getNumber() + " failed");
        }

        assertEquals(LOOPS, LooseTransactionThread.successes);
        assertEquals(0, LooseTransactionThread.failures);

        LooseTransactionThread thread = new LooseTransactionThread(-1, pds);
        thread.run();
        assertTrue(thread.isSuccesful());
    }


    static class LooseTransactionThread extends Thread {

        static int successes = 0;
        static int failures = 0;

        private int number;
        private PoolingDataSource poolingDataSource;
        private boolean succesful = false;

        public LooseTransactionThread(int number, PoolingDataSource poolingDataSource) {
            this.number = number;
            this.poolingDataSource = poolingDataSource;
        }

        public void run() {
            try {
                UserTransaction ut = TransactionManagerServices.getTransactionManager();
                if (log.isDebugEnabled()) log.debug("*** getting connection - " + number);
                Connection c1 = poolingDataSource.getConnection();

                if (log.isDebugEnabled()) log.debug("*** beginning the transaction - " + number);
                ut.begin();

                c1.prepareStatement("");

                if (log.isDebugEnabled()) log.debug("*** committing the transaction - " + number);
                ut.commit();


                if (log.isDebugEnabled()) log.debug("*** closing connection - " + number);
                c1.close();

                if (log.isDebugEnabled()) log.debug("*** all done - " + number);

                synchronized (LooseTransactionThread.class) {
                    successes++;
                }
                succesful = true;

            } catch (Exception ex) {
                log.warn("*** catched exception, waiting 500ms - " + number, ex);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (log.isDebugEnabled()) log.debug("*** catched exception, waited 500ms - " + number, ex);
                synchronized (LooseTransactionThread.class) {
                    failures++;
                }
            }
        } // run

        public int getNumber() {
            return number;
        }

        public boolean isSuccesful() {
            return succesful;
        }

    }

    public void testSerialization() throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("test-jdbc-pool.ser"));
        oos.writeObject(pds);
        oos.close();

        pds.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("test-jdbc-pool.ser"));
        pds = (PoolingDataSource) ois.readObject();
        ois.close();
    }

}
