package bitronix.tm.mock.resource.jdbc;

import javax.sql.XADataSource;
import javax.sql.XAConnection;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * (c) Bitronix, 19-déc.-2005
 *
 * @author lorban
 */
public class MockXADataSource implements XADataSource {

    private List xaConnections = new ArrayList();
    private String userName;
    private String password;
    private String database;

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
    }

    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public XAConnection getXAConnection() throws SQLException {
        MockXAConnection mockXAConnection = new MockXAConnection();
        xaConnections.add(mockXAConnection);
        return mockXAConnection;
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return getXAConnection();
    }

    public void setXaConnections(List xaConnections) {
        this.xaConnections = xaConnections;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
