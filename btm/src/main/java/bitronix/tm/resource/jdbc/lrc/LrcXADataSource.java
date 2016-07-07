/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.resource.jdbc.lrc;

import bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory;
import bitronix.tm.utils.ClassLoaderUtils;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * XADataSource implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 * @author Brett Wooldridge
 */
public class LrcXADataSource implements XADataSource {

    private volatile int loginTimeout;
    private volatile String driverClassName;
    private volatile String url;
    private volatile String user;
    private volatile String password;

    public LrcXADataSource() {
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
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

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        try {
            Class<?> driverClazz = ClassLoaderUtils.loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            if (user != null) props.setProperty("user", user);
            if (password != null) props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            XAConnection xaConnection = JdbcProxyFactory.INSTANCE.getProxyXaConnection(connection);
            return xaConnection;
        } catch (Exception ex) {
            throw new SQLException("unable to connect to non-XA resource " + driverClassName, ex);
        }
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        try {
            Class<?> driverClazz = ClassLoaderUtils.loadClass(driverClassName);
            Driver driver = (Driver) driverClazz.newInstance();
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            Connection connection = driver.connect(url, props);
            XAConnection xaConnection = JdbcProxyFactory.INSTANCE.getProxyXaConnection(connection);
            return xaConnection;
        } catch (Exception ex) {
            throw new SQLException("unable to connect to non-XA resource " + driverClassName, ex);
        }
    }

    @Override
    public String toString() {
        return "a JDBC LrcXADataSource on " + driverClassName + " with URL " + url;
    }
    
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}
