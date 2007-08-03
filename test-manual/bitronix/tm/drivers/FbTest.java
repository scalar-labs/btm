package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.firebirdsql.pool.FBConnectionPoolDataSource;

import javax.sql.XADataSource;
import java.util.Properties;


/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class FbTest extends XATestSuite {


    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(FBConnectionPoolDataSource.class.getName());
        bean.setUniqueName("fb1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "java");
        props.setProperty("password", "java");
        props.setProperty("database", "users1");
        bean.setDriverProperties(props);

        return bean;
   }

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(FBConnectionPoolDataSource.class.getName());
        bean.setUniqueName("fb2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "java");
        props.setProperty("password", "java");
        props.setProperty("database", "users2");
        bean.setDriverProperties(props);

        return bean;
    }

    public static FBConnectionPoolDataSource getXADataSource1() throws Exception {
        FBConnectionPoolDataSource dataSource = new FBConnectionPoolDataSource();
        dataSource.setUserName("java");
        dataSource.setPassword("java");
        dataSource.setDatabase("users1");
        return dataSource;
    }

    public static FBConnectionPoolDataSource getXADataSource2() throws Exception {
        FBConnectionPoolDataSource dataSource = new FBConnectionPoolDataSource();
        dataSource.setUserName("java");
        dataSource.setPassword("java");
        dataSource.setDatabase("users2");
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
        throw new RuntimeException("no API call to heuristically rollback, only gfix can do it");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("no API call to heuristically rollback, only gfix can do it");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (gen_id(USER_ID_SEQ, 1), '" + name + "')";
    }
}
