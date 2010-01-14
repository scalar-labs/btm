package bitronix.tm.mock;

import java.lang.reflect.Field;
import java.sql.*;

import javax.transaction.TransactionManager;

import bitronix.tm.recovery.RecoveryException;
import junit.framework.TestCase;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.resource.jdbc.*;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class JdbcPoolTest extends TestCase {

    private PoolingDataSource pds;

    protected void setUp() throws Exception {
        pds = new PoolingDataSource();
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(2);
        pds.setMaxIdleTime(1);
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setUniqueName("pds");
        pds.setAllowLocalTransactions(true);
        pds.setAcquisitionTimeout(1);
        pds.init();
    }


    protected void tearDown() throws Exception {
        pds.close();
    }

    public void testReEnteringRecovery() throws Exception {
        pds.startRecovery();
        try {
            pds.startRecovery();
            fail("excpected RecoveryException");
        } catch (RecoveryException ex) {
            assertEquals("recovery already in progress on a PoolingDataSource containing an XAPool of resource pds with 1 connection(s) (0 still available)", ex.getMessage());
        }

        // make sure startRecovery() can be called again once endRecovery() has been called
        pds.endRecovery();
        pds.startRecovery();
        pds.endRecovery();
    }

    public void testPoolGrowth() throws Exception {
        Field poolField = pds.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pds);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        try {
            pds.getConnection();
            fail("should not be able to get a 3rd connection");
        } catch (SQLException ex) {
            assertEquals("unable to get a connection from pool of a PoolingDataSource containing an XAPool of resource pds with 2 connection(s) (0 still available)", ex.getMessage());
        }

        c1.close();
        c2.close();
        assertEquals(2, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());
    }

    public void testPoolShrink() throws Exception {
        Field poolField = pds.getClass().getDeclaredField("pool");
        poolField.setAccessible(true);
        XAPool pool = (XAPool) poolField.get(pds);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c1 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());

        Connection c2 = pds.getConnection();
        assertEquals(0, pool.inPoolSize());
        assertEquals(2, pool.totalPoolSize());

        c1.close();
        c2.close();

        Thread.sleep(2500);

        assertEquals(1, pool.inPoolSize());
        assertEquals(1, pool.totalPoolSize());
    }

    public void testCloseLocalContext() throws Exception {
        Connection c = pds.getConnection();
        Statement stmt = c.createStatement();
        stmt.close();
        c.close();
        assertTrue(c.isClosed());

        try {
            c.createStatement();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }
    }

    public void testCloseGlobalContextRecycle() throws Exception {
        TransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection c1 = pds.getConnection();
        c1.createStatement();
        c1.close();
        assertTrue(c1.isClosed());

        try {
            c1.createStatement();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }

        Connection c2 = pds.getConnection();
        c2.createStatement();

         try {
            c2.commit();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("cannot commit a resource enlisted in a global transaction", ex.getMessage());
        }

        tm.commit();
        assertFalse(c2.isClosed());

        c2.close();
        assertTrue(c2.isClosed());

        try {
            c2.createStatement();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }

        try {
            c2.commit();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }
    }

    public void testCloseGlobalContextNoRecycle() throws Exception {
        TransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.begin();

        Connection c1 = pds.getConnection();
        Connection c2 = pds.getConnection();
        c1.createStatement();
        c1.close();
        assertTrue(c1.isClosed());

        try {
            c1.createStatement();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }

        c2.createStatement();

        try {
            c2.commit();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("cannot commit a resource enlisted in a global transaction", ex.getMessage());
        }

        tm.commit();
        assertFalse(c2.isClosed());

        c2.close();
        assertTrue(c2.isClosed());

        try {
            c2.createStatement();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }

        try {
            c2.commit();
            fail("expected SQLException");
        } catch (SQLException ex) {
            assertEquals("connection handle already closed", ex.getMessage());
        }
    }

    public void testPoolNotStartingTransactionManager() throws Exception {
        // make sure TM is not running
        TransactionManagerServices.getTransactionManager().shutdown();
        
        PoolingDataSource pds = new PoolingDataSource();
        pds.setMinPoolSize(1);
        pds.setMaxPoolSize(2);
        pds.setMaxIdleTime(1);
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setUniqueName("pds2");
        pds.setAllowLocalTransactions(true);
        pds.setAcquisitionTimeout(1);
        pds.init();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());

        Connection c = pds.getConnection();
        Statement stmt = c.createStatement();
        stmt.close();
        c.close();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());

        pds.close();

        assertFalse(TransactionManagerServices.isTransactionManagerRunning());
    }

}
