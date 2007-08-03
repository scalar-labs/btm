package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.apache.derby.jdbc.EmbeddedXADataSource;

import javax.sql.XADataSource;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class DerbyTest extends XATestSuite {


    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(EmbeddedXADataSource.class.getName());
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

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(EmbeddedXADataSource.class.getName());
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

    public static EmbeddedXADataSource getXADataSource1() throws Exception {
        EmbeddedXADataSource dataSource = new EmbeddedXADataSource();
        dataSource.setUser("users1");
        dataSource.setPassword("users1");
        dataSource.setDatabaseName("users1");
        return dataSource;
    }

    public static EmbeddedXADataSource getXADataSource2() throws Exception {
        EmbeddedXADataSource dataSource = new EmbeddedXADataSource();
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
