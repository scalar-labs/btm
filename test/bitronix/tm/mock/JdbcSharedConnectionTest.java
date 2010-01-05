package bitronix.tm.mock;

import java.sql.Connection;

import org.slf4j.*;

import bitronix.tm.*;
import bitronix.tm.resource.jdbc.JdbcConnectionHandle;

public class JdbcSharedConnectionTest extends AbstractMockJdbcTest {
    private final static Logger log = LoggerFactory.getLogger(NewJdbcProperUsageMockTest.class);

    public void testSharedConnection() throws Exception {
        if (log.isDebugEnabled()) log.debug("*** getting TM");
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) log.debug("*** before begin");
        tm.begin();
        if (log.isDebugEnabled()) log.debug("*** after begin");

        if (log.isDebugEnabled()) log.debug("*** getting connection from DS1");
        Connection connection1 = poolingDataSource1.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) log.debug("*** getting second connection from DS1");
        Connection connection2 = poolingDataSource1.getConnection();

        JdbcConnectionHandle handle1 = (JdbcConnectionHandle) connection1;
        JdbcConnectionHandle handle2 = (JdbcConnectionHandle) connection2;
        assertEquals(handle1.getConnection(), handle2.getConnection());

        connection1.close();
        connection2.close();

        tm.commit();
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
