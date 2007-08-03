package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import com.sybase.jdbc3.jdbc.SybXADataSource;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class SybaseTest extends XATestSuite {

    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(SybXADataSource.class.getName());
        bean.setUniqueName("sybase1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users1");
        props.setProperty("password", "users1");
        props.setProperty("serverName", "localhost");
        props.setProperty("portNumber", "5001");
        props.setProperty("loginTimeout", "3000");
        bean.setDriverProperties(props);

        return bean;
    }

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(SybXADataSource.class.getName());
        bean.setUniqueName("sybase2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users2");
        props.setProperty("password", "users2");
        props.setProperty("serverName", "localhost");
        props.setProperty("portNumber", "5001");
        props.setProperty("loginTimeout", "3000");
        bean.setDriverProperties(props);

        return bean;
    }

    public static SybXADataSource getXADataSource1() throws Exception {
        SybXADataSource dataSource = new SybXADataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setServerName("localhost");
        dataSource.setPortNumber(5001);
        dataSource.setLoginTimeout(3000);
        return dataSource;
    }

    public static SybXADataSource getXADataSource2() throws Exception {
        SybXADataSource dataSource = new SybXADataSource();
        dataSource.setUser("users2");
        dataSource.setPassword("users2");
        dataSource.setServerName("localhost");
        dataSource.setPortNumber(5001);
        dataSource.setLoginTimeout(3000);
        return dataSource;
    }

    private XADataSource _xaDataSource1;
    protected XADataSource _getXADataSource1() throws Exception {
        if (_xaDataSource1 == null) {
            _xaDataSource1 = getXADataSource1();
        }
        return _xaDataSource1;
    }

    private XADataSource _xaDataSource2;
    protected XADataSource _getXADataSource2() throws Exception {
        if (_xaDataSource2 == null) {
            _xaDataSource2 = getXADataSource2();
        }
        return _xaDataSource2;
    }

    protected String getInsertQuery(String name) {
        return "insert into users(name) values ('" + name + "')";
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        XAConnection xaConnection = dataSource.getXAConnection();
        Connection connection = xaConnection.getConnection();

        CallableStatement stmt = connection.prepareCall("sp_transactions");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            CallableStatement stmt2 = null;
            try {
                String xactname = rs.getString("xactname");
                System.out.println("heuristically completing " + xactname);

                String sql = "dbcc complete_xact (?, 'rollback')";
                stmt2 = connection.prepareCall(sql);
                stmt2.setString(1, xactname);
                stmt2.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            stmt2.close();
        }
        rs.close();
        stmt.close();

        connection.close();
        xaConnection.close();
    }

    protected void singleHeuristicRollback(XADataSource dataSource) throws Exception {
        XAConnection xaConnection = dataSource.getXAConnection();
        Connection connection = xaConnection.getConnection();

        CallableStatement stmt = connection.prepareCall("sp_transactions 'state', 'prepared'");
        ResultSet rs = stmt.executeQuery();
        rs.next();
        String xactname = rs.getString("xactname");
        rs.next();
        if (!rs.isAfterLast())
            fail("there must be only 1 in-doubt TX in prepared state");
        rs.close();
        stmt.close();
        System.out.println("heuristically completing " + xactname);

        String sql = "dbcc complete_xact (?, 'rollback')";
        stmt = connection.prepareCall(sql);
        stmt.setString(1, xactname);
        stmt.executeUpdate();
        stmt.close();

        connection.close();
        xaConnection.close();
    }

    public void testJoin() throws Exception {
        fail("skipped because it hangs");
    }

    public void testClean() throws Exception {
        clean();
    }
// testJoin:  ASE hangs forever when join is called
// testLocalTxEnding: ASE correctly throws an exception on setAutoCommit but isAutoCommit incorrectly returns true
// testLocalTxEnding: ASE even if connection.commit is correctly ignored, it should not be allowed to call it
// testConcurrentStart ASE driver throws XAException while JTA allows this
// testHeuristic: ASE fails with meaningless error 'Unrecognized return code from server: -259'

}
