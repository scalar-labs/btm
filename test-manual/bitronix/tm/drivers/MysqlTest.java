package bitronix.tm.drivers;

import javax.sql.XADataSource;
import javax.sql.XAConnection;

import bitronix.tm.resource.jdbc.DataSourceBean;

import java.util.Properties;
import java.sql.*;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class MysqlTest extends XATestSuite {

    public static DataSourceBean getDataSourceBean1() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(MysqlXADataSource.class.getName());
        bean.setUniqueName("mysql1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users1");
        props.setProperty("password", "users1");
        props.setProperty("databaseName", "users1");
        bean.setDriverProperties(props);

        return bean;
    }

    public static DataSourceBean getDataSourceBean2() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(MysqlXADataSource.class.getName());
        bean.setUniqueName("mysql2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users2");
        props.setProperty("password", "users2");
        props.setProperty("databaseName", "users2");
        bean.setDriverProperties(props);

        return bean;
    }

    public static MysqlXADataSource getXADataSource1() throws Exception {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setDatabaseName("users1");
        return dataSource;
    }

    public static MysqlXADataSource getXADataSource2() throws Exception {
        MysqlXADataSource dataSource = new MysqlXADataSource();
        dataSource.setUser("users2");
        dataSource.setPassword("users2");
        dataSource.setDatabaseName("users2");
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

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        XAConnection xaConnection = dataSource.getXAConnection();
        Connection connection = xaConnection.getConnection();

        PreparedStatement stmt = connection.prepareStatement("xa recover");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            PreparedStatement stmt2 = null;
            try {
                String xactname = rs.getString("data");
                System.out.println("heuristically completing " + xactname);

                String sql = "xa rollback ?";
                stmt2 = connection.prepareStatement(sql);
                stmt2.setString(1, xactname);
                stmt2.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
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

        PreparedStatement stmt = connection.prepareStatement("xa recover");
        ResultSet rs = stmt.executeQuery();
        rs.next();
        String xactname = rs.getString("data");
        rs.next();
        if (!rs.isAfterLast())
            fail("there must be only 1 in-doubt TX in prepared state");
        rs.close();
        stmt.close();
        System.out.println("heuristically completing " + xactname);

        String sql = "xa rollback ?";
        stmt = connection.prepareStatement(sql);
        stmt.setString(1, xactname);
        stmt.executeUpdate();
        stmt.close();

        connection.close();
        xaConnection.close();
    }

    protected String getInsertQuery(String name) {
        return "insert into users(name) values ('" + name + "')";
    }

}
