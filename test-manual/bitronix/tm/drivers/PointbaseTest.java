package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import com.pointbase.xa.xaDataSource;

import javax.sql.XADataSource;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class PointbaseTest extends XATestSuite {

    private final static String PB_INI_PATH = "C:/java/pointbase55/tools/embedded/pointbase.ini";

    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(xaDataSource.class.getName());
        bean.setUniqueName("pb1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users1");
        props.setProperty("password", "users1");
        props.setProperty("databaseName", "jdbc:pointbase:embedded:users1,pointbase.ini=" + PB_INI_PATH);
        bean.setDriverProperties(props);

        return bean;
   }

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(xaDataSource.class.getName());
        bean.setUniqueName("pb2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "users2");
        props.setProperty("password", "users2");
        props.setProperty("databaseName", "jdbc:pointbase:embedded:users2,pointbase.ini=" + PB_INI_PATH);
        bean.setDriverProperties(props);

        return bean;
    }

    public static xaDataSource getXADataSource1() throws Exception {
        xaDataSource dataSource = new xaDataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setDatabaseName("jdbc:pointbase:embedded:users1,pointbase.ini=" + PB_INI_PATH);
        return dataSource;
    }

    public static xaDataSource getXADataSource2() throws Exception {
        xaDataSource dataSource = new xaDataSource();
        dataSource.setUser("users2");
        dataSource.setPassword("users2");
        dataSource.setDatabaseName("jdbc:pointbase:embedded:users2,pointbase.ini=" + PB_INI_PATH);
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

    public void testClean() throws Exception {
        super.clean();
    }

    protected void singleHeuristicRollback(XADataSource dataSource) throws Exception {
        throw new RuntimeException("no API call to heuristically rollback");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("no API call to heuristically rollback");
    }

    protected String getInsertQuery(String name) {
        return "insert into users(name) values ('" + name + "')";
    }

}
