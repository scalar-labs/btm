package bitronix.tm.drivers;

import bitronix.tm.mock.resource.MockXid;
import bitronix.tm.internal.Decoder;

import javax.sql.XADataSource;
import javax.sql.XAConnection;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.XAException;
import java.sql.*;

import junit.framework.TestCase;

/**
 * (c) Bitronix, 01-janv.-2006
 *
 * @author lorban
 */
public abstract class XATestSuite extends TestCase {

    protected abstract XADataSource _getXADataSource1() throws Exception;
    protected abstract XADataSource _getXADataSource2() throws Exception;

    /**
     * Should find exactly one transaction in prepared state and heuristically roll it back
     */
    protected abstract void singleHeuristicRollback(XADataSource dataSource) throws Exception;

    /**
     * Should heuristically rollback all prepared transactions
     */
    protected abstract void heuristicRollbackAll(XADataSource dataSource) throws Exception;

    /**
     * @return a SQL query that can be used to perform an insert
     */
    protected abstract String getInsertQuery(String name);


    public void testJoin() throws Exception {
        Xid xid1 = genXid();
        Xid xid2 = genXid();
        Xid usedXid2 = xid1;

        XAConnection xaConnection1 = _getXADataSource1().getXAConnection();
        XAResource xaResource1 = xaConnection1.getXAResource();
        XAConnection xaConnection2 = _getXADataSource2().getXAConnection();
        XAResource xaResource2 = xaConnection2.getXAResource();

        xaResource1.start(xid1, XAResource.TMNOFLAGS);
        boolean join = xaResource2.isSameRM(xaResource1);
        if (join) {
            System.out.println("joining");
            xaResource2.start(xid1, XAResource.TMJOIN);
        }
        else {
            System.out.println("not joining");
            usedXid2 = xid2;
            xaResource2.start(xid2, XAResource.TMNOFLAGS);
        }

        xaResource1.end(xid1, XAResource.TMSUCCESS);
        xaResource2.end(usedXid2, XAResource.TMSUCCESS);

        xaConnection1.close();
        xaConnection2.close();
    }

    public void testLocalCommitIgnored() throws Exception {
        XAConnection xaConnection = _getXADataSource1().getXAConnection();

        Xid xid = genXid();

        XAResource xaResource = xaConnection.getXAResource();
        xaResource.start(xid, XAResource.TMNOFLAGS);

        Connection connection = xaConnection.getConnection();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(getInsertQuery("testLocalTxEnding"));
        stmt.close();

        try {
            connection.commit();
            System.err.println("WARN: java.sql.Connection.commit() calls are allowed in global TX");
        } catch (SQLException ex) {
            // expected
        }

        xaResource.end(xid, XAResource.TMSUCCESS);
        xaConnection.close();


        xid = genXid();
        xaConnection = _getXADataSource1().getXAConnection();
        connection = xaConnection.getConnection();
        xaResource = xaConnection.getXAResource();

        xaResource.start(xid, XAResource.TMNOFLAGS);

        stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from users where id = -1");
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();

        xaResource.end(xid, XAResource.TMSUCCESS);
        xaConnection.close();

        assertEquals("DB allowed local TX commit during global TX, count=", 0, count);
    }

    public void testConcurrentStart() throws Exception {
        XAConnection xaConnection1 = null;
        try {
            Xid xid1 = genXid();
            Xid xid2 = genXid();

            xaConnection1 = _getXADataSource1().getXAConnection();
            XAResource xaResource1 = xaConnection1.getXAResource();

            xaResource1.start(xid1, XAResource.TMNOFLAGS);
            xaResource1.end(xid1, XAResource.TMSUCCESS);

            xaResource1.start(xid2, XAResource.TMNOFLAGS);
            xaResource1.end(xid2, XAResource.TMSUCCESS);
        } catch (XAException ex) {
            throw new RuntimeException("starting on 2 different XIDs concurrently should be allowed, errorCode=" + Decoder.decodeXAExceptionErrorCode(ex), ex);
        }
        finally {
            if (xaConnection1 != null) xaConnection1.close();
        }
    }

    public void testConcurrentGetConnection() throws Exception {
        XAConnection xaConnection = null;
        Connection connection1 = null;
        Connection connection2 = null;
        try {
            xaConnection = _getXADataSource1().getXAConnection();
            connection1 = xaConnection.getConnection();
            connection2 = xaConnection.getConnection();
        }
        finally {
            if (connection2 != null) connection2.close(); // if() is in case 2nd getConnection threw an exception
            if (!connection1.isClosed()) connection1.close(); // if() is in case connection1 == connection2
            xaConnection.close();
        }
    }

    public void testRecover() throws Exception {
        XAConnection xaConnection = _getXADataSource1().getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = genXid();

        xaResource.start(xid, XAResource.TMNOFLAGS);

        Connection connection = xaConnection.getConnection();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(getInsertQuery("testRecover"));
        stmt.close();

        //FB: if java.sql.Connection.close() is called  when pooling=true, XAResource.recover fails wih NPE
        //FB: if java.sql.Connection.close() is called  when pooling=false, 'IllegalStateException: Can't destroy managed connection  with active transaction' is thrown
        System.out.println("connection " + connection);
        System.out.println("xaconnection " + xaConnection);
        connection.close();

        xaResource.end(xid, XAResource.TMSUCCESS);
        xaResource.prepare(xid);
        xaConnection.close();

        xaConnection = _getXADataSource1().getXAConnection();
        xaResource = xaConnection.getXAResource();

        Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertNotNull("there should have been one XID to recover", xids);
        assertEquals("there should have been one XID to recover", 1, xids.length);
        for (int i = 0; i < xids.length; i++) {
            Xid aXid = xids[i];
            System.out.println("recovered xid: " + xidToString(aXid));
            xaResource.rollback(aXid);
        }
    }

    public void testRecoveryProtocol() throws Exception {
        XAConnection xaConnection = _getXADataSource1().getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();
        Xid xid = genXid();

        xaResource.start(xid, XAResource.TMNOFLAGS);

        Connection connection = xaConnection.getConnection();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(getInsertQuery("testRecover"));
        stmt.close();

        xaResource.end(xid, XAResource.TMSUCCESS);
        xaResource.prepare(xid);
        xaConnection.close();

        xaConnection = _getXADataSource1().getXAConnection();
        xaResource = xaConnection.getXAResource();

        Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN);
        for (int i = 0; i < xids.length; i++) {
            Xid aXid = xids[i];
            System.out.println("TMSTARTRSCAN recovered xid: " + xidToString(aXid));
        }
        while(true) {
            xids = xaResource.recover(XAResource.TMNOFLAGS);
            if (xids.length == 0)
                break;
            for (int i = 0; i < xids.length; i++) {
                Xid aXid = xids[i];
                System.out.println("TMNOFLAGS recovered xid: " + xidToString(aXid));
            }
        }
        xids = xaResource.recover(XAResource.TMENDRSCAN);
        for (int i = 0; i < xids.length; i++) {
            Xid aXid = xids[i];
            System.out.println("TMENDRSCAN recovered xid: " + xidToString(aXid));
        }
    }

    public void testHeuristic() throws Exception {
        XAConnection xaConnection = _getXADataSource1().getXAConnection();
        try {
            XAResource xaResource = xaConnection.getXAResource();
            Xid xid = genXid();

            xaResource.start(xid, XAResource.TMNOFLAGS);

            Connection connection = xaConnection.getConnection();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(getInsertQuery("testHeuristic"));
            connection.close();

            xaResource.end(xid, XAResource.TMSUCCESS);

            int rc = xaResource.prepare(xid);
            System.out.println("prepare vote=" + Decoder.decodePrepareVote(rc));

            xaConnection.close();
            xaConnection = null;

            singleHeuristicRollback(_getXADataSource1());

            xaConnection = _getXADataSource1().getXAConnection();
            xaResource = xaConnection.getXAResource();

            Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
            assertNotNull("there should have been one XID to recover", xids);
            for (int i = 0; i < xids.length; i++) {
                Xid aXid = xids[i];
                System.out.println("recovered XID: " + xidToString(aXid));
                try {
                    xaResource.rollback(aXid);
                    fail("XAException should have been thrown because of heuristic rollback");
                } catch (XAException ex) {
                    assertEquals("XAException should have been XA_HEURRB, was: " + Decoder.decodeXAExceptionErrorCode(ex) + " -", XAException.XA_HEURRB, ex.errorCode);
                }
            }
        }
        finally {
            if (xaConnection != null) xaConnection.close();
        }

    }

    public void testIncorrect1Pc() throws Exception {
        boolean prepared = false;
        try {
            Xid xid = genXid();

            XAConnection xaConnection1 = _getXADataSource1().getXAConnection();
            XAResource xaResource1 = xaConnection1.getXAResource();

            xaResource1.start(xid, XAResource.TMNOFLAGS);
            Connection connection = xaConnection1.getConnection();

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(getInsertQuery("test1Pc"));
            stmt.close();
            connection.close();
            xaResource1.end(xid, XAResource.TMSUCCESS);

            int rc = xaResource1.prepare(xid);
            System.out.println("prepare vote: " + Decoder.decodePrepareVote(rc));

            prepared = true;
            xaResource1.commit(xid, true);

            fail("DB accepted commit 1PC optimization after prepare");
        } catch (XAException e) {
            if (!prepared)
                throw e;
        }
    }

    public void testSqlConnectionClose() throws Exception {
        long before = System.currentTimeMillis();
        XAConnection xaConnection = _getXADataSource1().getXAConnection();

        for (int i=0; i<5000 ;i++) {
            Connection connection = xaConnection.getConnection();
            connection.close();
        }

        xaConnection.close();
        long after = System.currentTimeMillis();
        System.out.println("time taken: " + (after - before) + "ms");
    }

    public void testXAResourceEnd() throws Exception {
        XADataSource dataSource = _getXADataSource1();

        Xid xid = genXid();

        XAConnection xaConnection = dataSource.getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();

        xaResource.start(xid, XAResource.TMNOFLAGS);
        Connection connection = xaConnection.getConnection();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(getInsertQuery("testXAResourceEnd"));
        stmt.close();
        // FB: if XAResource.end is not called, rollback fails with an unintelligible message
        // 'Rollback called with current xid'
        xaResource.end(xid, XAResource.TMSUCCESS);

        xaResource.rollback(xid);
        xaConnection.close();

        xaConnection = dataSource.getXAConnection();
        xaResource = xaConnection.getXAResource();

        xaResource.start(xid, XAResource.TMNOFLAGS);
        connection = xaConnection.getConnection();
        stmt = connection.createStatement();
        stmt.executeUpdate(getInsertQuery("testXAResourceEnd"));
        stmt.close();
        xaResource.end(xid, XAResource.TMSUCCESS);

        xaResource.rollback(xid);

        xaConnection.close();
    }

    /*
     * Test suite internals
     */

    protected void setUp() throws Exception {
        clean();
    }

    private static byte uid = 0;
    private static Xid genXid() {
        synchronized (XATestSuite.class) {
            uid++;
            long value = System.currentTimeMillis() + uid;
            MockXid xid = new MockXid(value, value);
            System.out.println("generated XID: " + xid);
            return xid;
        }
    }

    private static String xidToString(Xid xid) {
        if (xid == null)
            return null;
        return new MockXid(xid.getBranchQualifier(), xid.getGlobalTransactionId()).toString();
    }

    /**
     * Use this to clean all in-doubt TX by rolling them back or forgetting them.
     *
     * @throws Exception
     */
    public void clean() throws Exception {
        try {
            heuristicRollbackAll(_getXADataSource1());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

        XAConnection xaConnection = _getXADataSource1().getXAConnection();
        XAResource xaResource = xaConnection.getXAResource();

        Xid[] xids = xaResource.recover(XAResource.TMSTARTRSCAN );
        if (xids != null)
            for (int i = 0; i < xids.length; i++) {
                Xid aXid = xids[i];
                System.out.println("recovered XID: " + xidToString(aXid));
                try {
                    xaResource.rollback(aXid);
                } catch (XAException e) {
                    xaResource.forget(aXid);
                }
            }

        xaConnection.close();

        try {
            heuristicRollbackAll(_getXADataSource2());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }

        xaConnection = _getXADataSource2().getXAConnection();
        xaResource = xaConnection.getXAResource();

        xids = xaResource.recover(XAResource.TMSTARTRSCAN );
        if (xids != null)
            for (int i = 0; i < xids.length; i++) {
                Xid aXid = xids[i];
                System.out.println("recovered XID: " + xidToString(aXid));
                try {
                    xaResource.rollback(aXid);
                } catch (XAException e) {
                    xaResource.forget(aXid);
                }
            }

        xaConnection.close();
    }

}
