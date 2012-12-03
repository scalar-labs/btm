package bitronix.tm.mock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;

import junit.framework.Assert;

import org.junit.Test;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;

public class JdbcProxyTest extends AbstractMockJdbcTest {

    @Test
    public void testSetters() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(30);
        tm.begin();

        Connection connection = poolingDataSource1.getConnection();

        long start = System.nanoTime();
        PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Date date = new Date(0);
        for (int i = 0; i < 50000; i++) {
            stmt.setString(1, "foo");
            stmt.setInt(2, 999);
            stmt.setDate(3, date);
            stmt.setFloat(4, 9.99f);
            stmt.clearParameters();
        }
        System.out.println(System.nanoTime() - start);

        stmt.executeQuery();

        connection.close();

        tm.commit();
        tm.shutdown();
    }

    @Test
    public void testPrepares() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource2.getConnection();

        for (int i = 0; i < 1000; i++) {
            PreparedStatement prepareStatement = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
            prepareStatement.close();
        }

        connection.close();

        tm.commit();
        tm.shutdown();
    }

    @Test
    public void testCachedPrepared() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource1.getConnection();

        PreparedStatement prepareStatement1 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        PreparedStatement prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");

        Assert.assertSame(prepareStatement1, prepareStatement2);

        prepareStatement2.close();

        prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Assert.assertSame(prepareStatement1, prepareStatement2);

        prepareStatement1.close();
        prepareStatement2.close();

        connection.close();
        tm.shutdown();
    }

    @Test
    public void testUnCachedPrepared() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource2.getConnection();

        PreparedStatement prepareStatement1 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        PreparedStatement prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");

        Assert.assertNotSame(prepareStatement1, prepareStatement2);

        prepareStatement2.close();

        prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Assert.assertNotSame(prepareStatement1, prepareStatement2);

        prepareStatement1.close();
        prepareStatement2.close();

        connection.close();
        tm.shutdown();
    }
}
