package bitronix.tm.mock.resource.jdbc;

import javax.sql.XADataSource;
import javax.sql.XAConnection;
import javax.transaction.xa.Xid;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
    private List inDoubtXids = new ArrayList();
    private SQLException getXAConnectionException;
    private static SQLException staticGetXAConnectionException;

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
        if (staticGetXAConnectionException != null)
            throw staticGetXAConnectionException;
        if (getXAConnectionException != null)
            throw getXAConnectionException;

        MockXAConnection mockXAConnection = new MockXAConnection(this);
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

    public void addInDoubtXid(Xid xid) {
        inDoubtXids.add(xid);
    }

    public boolean removeInDoubtXid(Xid xid) {
        for (int i = 0; i < inDoubtXids.size(); i++) {
            Xid xid1 = (Xid) inDoubtXids.get(i);
            if (Arrays.equals(xid1.getGlobalTransactionId(), xid.getGlobalTransactionId()) && Arrays.equals(xid1.getBranchQualifier(), xid.getBranchQualifier()) ) {
                inDoubtXids.remove(xid1);
                return true;
            }
        }
        return false;
    }

    public Xid[] getInDoubtXids() {
        return (Xid[]) inDoubtXids.toArray(new Xid[inDoubtXids.size()]);
    }

    public void setGetXAConnectionException(SQLException ex) {
        this.getXAConnectionException = ex;
    }

    public static void setStaticGetXAConnectionException(SQLException ex) {
        staticGetXAConnectionException = ex;
    }
}
