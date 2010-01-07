package bitronix.tm.mock.resource.jdbc;

import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.util.Properties;

/**
 * <p></p>
 * <p>&copy; Bitronix 2005, 2006</p>
 *
 * @author lorban
 */
public class MockDriver implements Driver {

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }

    public boolean acceptsURL(String url) throws SQLException {
        return false;
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return MockitoXADataSource.createMockConnection();
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }
}
