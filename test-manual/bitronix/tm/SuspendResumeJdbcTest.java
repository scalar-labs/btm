package bitronix.tm;

import bitronix.tm.drivers.*;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transaction;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * (c) Bitronix, 20-oct.-2005
 *
 * @author lorban
 */
public class SuspendResumeJdbcTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(SuspendResumeJdbcTest.class);

    private String query;
    private PoolingDataSource poolingDataSource1;

    protected void setUp() throws Exception {
        // change transactionRetryInterval to 1 second
        Field field = TransactionManagerServices.getConfiguration().getClass().getDeclaredField("transactionRetryInterval");
        field.setAccessible(true);
        field.set(TransactionManagerServices.getConfiguration(), new Integer(1));

        setUpJdbc_FB();
        dropAll();
    }

    private void setUpJdbc_FB() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (id, name) values (?, ?)";

        poolingDataSource1 = FbTest.getPoolingDataSource1();
    }


    private void setUpJdbc_Derby() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = DerbyTest.getPoolingDataSource1();
    }

    private void setUpJdbc_Mysql() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (name) values (?)";

        poolingDataSource1 = MysqlTest.getPoolingDataSource1();
    }

    private void setUpJdbc_ORA() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = OracleTest.getPoolingDataSource1();
    }

    private void setUpJdbc_ASE() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (name) values (?)";

        poolingDataSource1 = SybaseTest.getPoolingDataSource1();
    }


    private void setUpJdbc_FSQL() throws Exception {
        if (poolingDataSource1 != null)
            return;

        query = "insert into users (id, name) values (" + genPk() + ", ?)";

        poolingDataSource1 = FirstsqlTest.getPoolingDataSource1();
    }

    public void testInterleaveLocalGlobal() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

        if (log.isDebugEnabled()) log.debug(" ****** about to begin");
        tm.begin();

        if (log.isDebugEnabled()) log.debug(" ****** getting connection");
        Connection connection1 = poolingDataSource1.getConnection();

        if (log.isDebugEnabled()) log.debug(" ****** doing some read-write SQL globally");
        PreparedStatement psg = connection1.prepareStatement(query);
        psg.setInt(1, (int) genPk());
        psg.setString(2, "testAutoEnlistment(" + getUniqueNumber() + ")");
        psg.executeUpdate();
        psg.close();
        if (log.isDebugEnabled()) log.debug(" ****** executed global update");

        Transaction t1 = tm.suspend();

        if (log.isDebugEnabled()) log.debug(" ****** doing some read-write SQL locally");
        connection1.setAutoCommit(false);
        PreparedStatement psl = connection1.prepareStatement(query);
        psg.setInt(1, (int) genPk());
        psl.setString(2, "testAutoEnlistment(" + getUniqueNumber() + ")");
        psl.executeUpdate();
        psl.close();
        connection1.rollback();
        if (log.isDebugEnabled()) log.debug(" ****** executed local update");

        tm.resume(t1);

        if (log.isDebugEnabled()) log.debug(" ****** about to close connection 1");
        connection1.close();

        if (log.isDebugEnabled()) log.debug(" ****** about to commit");
        tm.commit();

        if (log.isDebugEnabled()) log.debug(" ****** checking DBt");

        connection1 = poolingDataSource1.getConnection();
        PreparedStatement psv = connection1.prepareStatement("select count(*) from users");
        ResultSet rs = psv.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        psv.close();

        assertEquals("expected only 1 record in DB", 1, count);
    }

    static int cpt = 0;
    private long getUniqueNumber() {
        synchronized (getClass()) {
            return cpt++;
        }
    }

    private long genPk() {
        return System.currentTimeMillis() + getUniqueNumber();
    }

    public void dropAll() throws Exception {
        Connection connection = poolingDataSource1.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate("delete from users");
        connection.close();
    }

}
