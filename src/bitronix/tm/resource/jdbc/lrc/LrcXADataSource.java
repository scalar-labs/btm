package bitronix.tm.resource.jdbc.lrc;

import javax.sql.XADataSource;
import javax.sql.XAConnection;
import java.sql.SQLException;
import java.sql.Driver;
import java.sql.Connection;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * XADataSource implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class LrcXADataSource implements XADataSource {

    private int loginTimeout;
    private String driverClassName;
    private String url;
    private String user;
    private String password;

    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public XAConnection getXAConnection() throws SQLException {
        try {
            Class driverClazz = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            return new LrcXAConnection(connection);
        } catch (Exception ex) {
            throw (SQLException) new SQLException("unable to connect to non-XA resource " + driverClassName).initCause(ex);
        }
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        try {
            Class driverClazz = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            return new LrcXAConnection(connection);
        } catch (Exception ex) {
            throw (SQLException) new SQLException("unable to connect to non-XA resource " + driverClassName).initCause(ex);
        }
    }

    public String toString() {
        return "a LrcXADataSource on " + driverClassName + " with URL " + url;
    }
}
