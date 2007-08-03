package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import oracle.jdbc.xa.client.OracleXADataSource;

import javax.sql.XADataSource;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class OracleTest extends XATestSuite {


    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(OracleXADataSource.class.getName());
        bean.setUniqueName("oracle1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users1");
        props.setProperty("password", "users1");
        props.setProperty("URL", "jdbc:oracle:thin:@localhost:1521:XE");
        bean.setDriverProperties(props);

        return bean;
    }

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(OracleXADataSource.class.getName());
        bean.setUniqueName("oracle2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users2");
        props.setProperty("password", "users2");
        props.setProperty("URL", "jdbc:oracle:thin:@localhost:1521:XE");
        bean.setDriverProperties(props);

        return bean;
    }

    public static OracleXADataSource getXADataSource1() throws Exception {
        OracleXADataSource dataSource = new OracleXADataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setURL("jdbc:oracle:thin:@localhost:1521:XE");
        return dataSource;
    }

    public static OracleXADataSource getXADataSource2() throws Exception {
        OracleXADataSource dataSource = new OracleXADataSource();
        dataSource.setUser("users2");
        dataSource.setPassword("users2");
        dataSource.setURL("jdbc:oracle:thin:@localhost:1521:XE");
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

    public void testJoin() throws Exception {
        throw new RuntimeException("skipped because it hangs");
    }

    protected void singleHeuristicRollback(XADataSource dataSource) throws Exception {
        throw new RuntimeException("Oracle XE cannot heuristically rollback");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("Oracle XE cannot heuristically rollback");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (seq_users_id.nextval, '" + name + "')";
    }

}
