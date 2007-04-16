package bitronix.tm.drivers;

import javax.sql.XADataSource;

import COM.FirstSQL.Dbcp.DbcpXADataSource;
import bitronix.tm.resource.jdbc.DataSourceBean;

import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class FirstsqlTest extends XATestSuite {


    public static DataSourceBean getDataSourceBean1() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(DbcpXADataSource.class.getName());
        bean.setUniqueName("fistsql1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "java");
        props.setProperty("password", "");
        props.setProperty("serverName", "localhost");
        props.setProperty("portNumber", "8001");
        props.setProperty("loginTimeout", "3000");
        bean.setDriverProperties(props);

        return bean;
    }

    public static DataSourceBean getDataSourceBean2() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(DbcpXADataSource.class.getName());
        bean.setUniqueName("fistsql2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "java");
        props.setProperty("password", "");
        props.setProperty("serverName", "localhost");
        props.setProperty("portNumber", "8002");
        props.setProperty("loginTimeout", "3000");
        bean.setDriverProperties(props);

        return bean;
    }

    public static DbcpXADataSource getXADataSource1() throws Exception {
        DbcpXADataSource dataSource = new DbcpXADataSource();
        dataSource.setUser("java");
        dataSource.setPassword("");
        dataSource.setServerName("localhost");
        dataSource.setPortNumber(8001);
        dataSource.setLoginTimeout(3000);
        return dataSource;
    }

    public static DbcpXADataSource getXADataSource2() throws Exception {
        DbcpXADataSource dataSource = new DbcpXADataSource();
        dataSource.setUser("java");
        dataSource.setPassword("");
        dataSource.setServerName("localhost");
        dataSource.setPortNumber(8002);
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

    protected void singleHeuristicRollback(XADataSource dataSource) throws Exception {
        throw new RuntimeException("FirstSQL cannot heuristically roll back");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("FirstSQL cannot heuristically roll back");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (" + genPk() + ", '" + name + "')";
    }

    private long pk;
    private synchronized long genPk() {
        return System.currentTimeMillis() + (pk++);
    }

    public void testJoin() throws Exception {
        fail("skipped because it hangs");
    }

}
