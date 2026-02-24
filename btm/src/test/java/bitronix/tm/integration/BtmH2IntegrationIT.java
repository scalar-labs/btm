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
package bitronix.tm.integration;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Integration test that exercises BTM with a real H2 in-memory database
 * using the Last Resource Commit (LRC) emulation.
 */
public class BtmH2IntegrationIT extends TestCase {

    private BitronixTransactionManager btm;
    private PoolingDataSource dataSource;

    @Override
    protected void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(1);

        dataSource = new PoolingDataSource();
        dataSource.setClassName(LrcXADataSource.class.getName());
        dataSource.setUniqueName("h2-test-ds");
        dataSource.setMaxPoolSize(5);
        dataSource.setAllowLocalTransactions(true);
        dataSource.getDriverProperties().setProperty("driverClassName", "org.h2.Driver");
        dataSource.getDriverProperties().setProperty("url", "jdbc:h2:mem:btm_test;DB_CLOSE_DELAY=-1");
        dataSource.init();

        btm = TransactionManagerServices.getTransactionManager();
    }

    @Override
    protected void tearDown() throws Exception {
        btm.shutdown();
        dataSource.close();
    }

    public void testCommitPersistsData() throws Exception {
        // Create table.
        btm.begin();
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(100))");
        stmt.close();
        conn.close();
        btm.commit();

        // Insert data in a transaction.
        btm.begin();
        conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("INSERT INTO test_table (id, name) VALUES (?, ?)");
        ps.setInt(1, 1);
        ps.setString(2, "btm-integration-test");
        ps.executeUpdate();
        ps.close();
        conn.close();
        btm.commit();

        // Read back and verify.
        btm.begin();
        conn = dataSource.getConnection();
        ps = conn.prepareStatement("SELECT name FROM test_table WHERE id = ?");
        ps.setInt(1, 1);
        ResultSet rs = ps.executeQuery();
        assertTrue("Expected a row to be returned", rs.next());
        assertEquals("btm-integration-test", rs.getString("name"));
        assertFalse("Expected only one row", rs.next());
        rs.close();
        ps.close();
        conn.close();
        btm.commit();
    }

    public void testRollbackDiscardsData() throws Exception {
        // Create table.
        btm.begin();
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE rollback_table (id INT PRIMARY KEY, name VARCHAR(100))");
        stmt.close();
        conn.close();
        btm.commit();

        // Insert data but rollback.
        btm.begin();
        conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("INSERT INTO rollback_table (id, name) VALUES (?, ?)");
        ps.setInt(1, 1);
        ps.setString(2, "should-not-persist");
        ps.executeUpdate();
        ps.close();
        conn.close();
        btm.rollback();

        // Verify no data was persisted.
        btm.begin();
        conn = dataSource.getConnection();
        ps = conn.prepareStatement("SELECT COUNT(*) FROM rollback_table");
        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        assertEquals(0, rs.getInt(1));
        rs.close();
        ps.close();
        conn.close();
        btm.commit();
    }
}
