package bitronix.tm.mock;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;

import javax.transaction.*;

import org.slf4j.*;

import bitronix.tm.*;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;

public class JdbcSharedConnectionTest extends AbstractMockJdbcTest {
    private final static Logger log = LoggerFactory.getLogger(NewJdbcProperUsageMockTest.class);

    public void testSharedConnectionMultithreaded() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        final BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        final Transaction suspended = tm.suspend();

        final ArrayList twoConnections = new ArrayList();
        Thread thread1 = new Thread() {
        	public void run() {
        		try {
					tm.resume(suspended);
			        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
			        Connection connection = poolingDataSource1.getConnection();
			        connection.createStatement();
			        twoConnections.add(connection);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
        	}
        };
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread() {
        	public void run() {
        		try {
					tm.resume(suspended);
			        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
			        Connection connection = poolingDataSource1.getConnection();
			        connection.createStatement();
			        twoConnections.add(connection);
			        tm.commit();
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
        	}
        };
        thread2.start();
        thread2.join();

        JdbcConnectionHandle handle1 = (JdbcConnectionHandle) Proxy.getInvocationHandler(twoConnections.get(0));
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) Proxy.getInvocationHandler(twoConnections.get(1));
        assertNotSame(handle1.getConnection(), handle2.getConnection());

    }

    public void testUnSharedConnection() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection1 = poolingDataSource2.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting second connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        assertNotSame(connection1, connection2);

        connection1.close();
        connection2.close();

        tm.commit();
    }

    public void testSharedConnectionInLocalTransaction() throws Exception {

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting second connection from DS1");
        Connection connection2 = poolingDataSource1.getConnection();
        assertNotSame(connection1, connection2);

        connection1.close();
        connection2.close();
    }

    public void testUnSharedConnectionInLocalTransaction() throws Exception {

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS2");
        Connection connection1 = poolingDataSource2.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting second connection from DS2");
        Connection connection2 = poolingDataSource2.getConnection();
        assertNotSame(connection1, connection2);

        connection1.close();
        connection2.close();
    }

    public void testSharedConnectionInNonEnlistedTransaction() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();

        if (log.isDebugEnabled()) log.debug("*** getting second connection from DS1");
        Connection connection2 = poolingDataSource1.getConnection();
        assertNotSame(connection1, connection2);

        connection1.close();
        connection2.close();

        tm.commit();
    }
}
