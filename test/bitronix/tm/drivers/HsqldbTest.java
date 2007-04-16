package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.DataSourceBean;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;

import javax.sql.XADataSource;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class HsqldbTest extends XATestSuite {

    public static DataSourceBean getDataSourceBean1() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(LrcXADataSource.class.getName());
        bean.setUniqueName("hsql1");
        bean.setPoolSize(1);

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("url", "jdbc:hsqldb:hsqldb/users1;shutdown=true");
        props.setProperty("driverClassName", "org.hsqldb.jdbcDriver");
        bean.setDriverProperties(props);

        return bean;
   }

    public static DataSourceBean getDataSourceBean2() {
        DataSourceBean bean = new DataSourceBean();
        bean.setClassName(LrcXADataSource.class.getName());
        bean.setUniqueName("hsql2");
        bean.setPoolSize(1);

        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("url", "jdbc:hsqldb:hsqldb/users2;shutdown=true");
        props.setProperty("driverClassName", "org.hsqldb.jdbcDriver");
        bean.setDriverProperties(props);

        return bean;
    }

    public static LrcXADataSource getXADataSource1() throws Exception {
        LrcXADataSource dataSource = new LrcXADataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:hsqldb/users1;shutdown=true");
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        return dataSource;
    }

    public static LrcXADataSource getXADataSource2() throws Exception {
        LrcXADataSource dataSource = new LrcXADataSource();
        dataSource.setUser("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:hsqldb/users2;shutdown=true");
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
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
        throw new RuntimeException("no API call to heuristically rollback");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("no API call to heuristically rollback");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (gen_id(USER_ID_SEQ, 1), '" + name + "')";
    }
}
