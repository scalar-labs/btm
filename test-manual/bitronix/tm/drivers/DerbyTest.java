package bitronix.tm.drivers;

import javax.sql.XADataSource;

import org.apache.derby.jdbc.ClientXADataSource;
import bitronix.tm.resource.jdbc.DataSourceBean;

import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class DerbyTest extends XATestSuite {


    public static DataSourceBean getDataSourceBean1() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(ClientXADataSource.class.getName());
        bean.setUniqueName("derby1");
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
        bean.setClassName(ClientXADataSource.class.getName());
        bean.setUniqueName("derby2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users2");
        props.setProperty("password", "users2");
        props.setProperty("databaseName", "users2");
        bean.setDriverProperties(props);

        return bean;
    }

    public static ClientXADataSource getXADataSource1() throws Exception {
        ClientXADataSource dataSource = new ClientXADataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setDatabaseName("users1");
        return dataSource;
    }

    public static ClientXADataSource getXADataSource2() throws Exception {
        ClientXADataSource dataSource = new ClientXADataSource();
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

    protected void singleHeuristicRollback(XADataSource dataSource) throws Exception {
        throw new RuntimeException("Derby cannot heuristically rollback");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("Derby cannot heuristically rollback");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (" + genPk() + ", '" + name + "')";
    }

    private long pk;
    private synchronized long genPk() {
        return System.currentTimeMillis() + (pk++);
    }

}
