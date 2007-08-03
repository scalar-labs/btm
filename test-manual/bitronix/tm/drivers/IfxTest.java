package bitronix.tm.drivers;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import com.informix.jdbcx.IfxXADataSource;

import javax.sql.XADataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;


/**
 * (c) Bitronix, 05-nov.-2005
 *
 * @author lorban
 */
public class IfxTest extends XATestSuite {

    public static PoolingDataSource getPoolingDataSource1() throws UnknownHostException {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(IfxXADataSource.class.getName());
        bean.setUniqueName("ifx1");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "informix");
        props.setProperty("password", "all4one");
        props.setProperty("databaseName", "users1");
        props.setProperty("portNumber", "1526");
        props.setProperty("serverName", "ifxtest");
        props.setProperty("ifxIFXHOST", InetAddress.getLocalHost().getHostName());
        props.setProperty("ifxIFX_LOCK_MODE_WAIT", "5");
        props.setProperty("ifxIFX_XASPEC", "Y");
        bean.setDriverProperties(props);

        return bean;
   }

    public static PoolingDataSource getPoolingDataSource2() throws UnknownHostException {
        PoolingDataSource bean = new PoolingDataSource();
        bean.setClassName(IfxXADataSource.class.getName());
        bean.setUniqueName("ifx2");
        bean.setPoolSize(5);
        bean.setDeferConnectionRelease(true);

        Properties props = new Properties();
        props.setProperty("user", "informix");
        props.setProperty("password", "all4one");
        props.setProperty("databaseName", "users2");
        props.setProperty("portNumber", "1526");
        props.setProperty("serverName", "ifxtest");
        props.setProperty("ifxIFXHOST", InetAddress.getLocalHost().getHostName());
        props.setProperty("ifxIFX_LOCK_MODE_WAIT", "5");
        props.setProperty("ifxIFX_XASPEC", "Y");
        bean.setDriverProperties(props);

        return bean;
    }

    public static IfxXADataSource getXADataSource1() throws Exception {
        IfxXADataSource dataSource = new IfxXADataSource();
        dataSource.setUser("informix");
        dataSource.setPassword("all4one");
        dataSource.setDatabaseName("users1");
        dataSource.setPortNumber(1526);
        dataSource.setServerName("ifxtest");
        dataSource.setIfxIFXHOST(InetAddress.getLocalHost().getHostName());
        dataSource.setIfxIFX_LOCK_MODE_WAIT(5);
        dataSource.setIfxIFX_XASPEC("Y");
        return dataSource;
    }

    public static IfxXADataSource getXADataSource2() throws Exception {
        IfxXADataSource dataSource = new IfxXADataSource();

        dataSource.setUser("informix");
        dataSource.setPassword("all4one");
        dataSource.setDatabaseName("users2");
        dataSource.setPortNumber(1526);
        dataSource.setServerName("ifxtest");
        dataSource.setIfxIFXHOST(InetAddress.getLocalHost().getHostName());
        dataSource.setIfxIFX_LOCK_MODE_WAIT(5);
        dataSource.setIfxIFX_XASPEC("Y");
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
        return "insert into users values (gen_id(USER_ID_SEQ, 1), '" + name + "')";
    }
}
