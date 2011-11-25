/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA 02110-1301 USA
 */
package bitronix.tm.resource.jdbc.lrc;

import bitronix.tm.utils.ClassLoaderUtils;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * XADataSource implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author lorban, brettw
 */
public class LrcXADataSource implements XADataSource {

    private volatile int loginTimeout;
    private volatile String driverClassName;
    private volatile String url;
    private volatile String user;
    private volatile String password;

    public LrcXADataSource() {
    }

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
            Class driverClazz = ClassLoaderUtils.loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            if (user != null) props.setProperty("user", user);
            if (password != null) props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            LrcXAConnection lrcXAConnection = new LrcXAConnection(connection);
            return (XAConnection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { XAConnection.class }, lrcXAConnection);
        } catch (Exception ex) {
            throw (SQLException) new SQLException("unable to connect to non-XA resource " + driverClassName).initCause(ex);
        }
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        try {
            Class driverClazz = ClassLoaderUtils.loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            LrcXAConnection lrcXAConnection = new LrcXAConnection(connection);
            return (XAConnection) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class[] { XAConnection.class }, lrcXAConnection);
        } catch (Exception ex) {
            throw (SQLException) new SQLException("unable to connect to non-XA resource " + driverClassName).initCause(ex);
        }
    }

    public String toString() {
        return "a JDBC LrcXADataSource on " + driverClassName + " with URL " + url;
    }
}
