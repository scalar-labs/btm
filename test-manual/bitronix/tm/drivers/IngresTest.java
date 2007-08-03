package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import com.ingres.jdbc.IngresXADataSource;

import javax.sql.XADataSource;
import java.util.Properties;

/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class IngresTest extends XATestSuite {


    public static PoolingDataSource getPoolingDataSource1() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(IngresXADataSource.class.getName());
        bean.setUniqueName("ingres1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("serverName", "localhost");
        props.setProperty("databaseName", "users1");
        props.setProperty("portName", "II7");
        bean.setDriverProperties(props);

        return bean;
    }

    public static PoolingDataSource getPoolingDataSource2() {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(IngresXADataSource.class.getName());
        bean.setUniqueName("ingres2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("serverName", "localhost");
        props.setProperty("databaseName", "users2");
        props.setProperty("portName", "II7");
        bean.setDriverProperties(props);

        return bean;
    }

    public static IngresXADataSource getXADataSource1() throws Exception {
        IngresXADataSource dataSource = new IngresXADataSource();
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName("users1");
        dataSource.setPortName("II7");
        return dataSource;
    }

    public static IngresXADataSource getXADataSource2() throws Exception {
        IngresXADataSource dataSource = new IngresXADataSource();
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName("users2");
        dataSource.setPortName("II7");
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
        throw new RuntimeException("ingres cannot heuristically rollback");
    }

    protected void heuristicRollbackAll(XADataSource dataSource) throws Exception {
        throw new RuntimeException("ingres cannot heuristically rollback");
    }

    protected String getInsertQuery(String name) {
        return "insert into users values (" + genPk() + ", '" + name + "')";
    }

    private long pk;
    private synchronized long genPk() {
        return System.currentTimeMillis() + (pk++);
    }

}
