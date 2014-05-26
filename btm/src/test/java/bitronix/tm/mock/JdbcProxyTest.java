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
package bitronix.tm.mock;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import junit.framework.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;

public class JdbcProxyTest extends AbstractMockJdbcTest {

    @Test
    public void testSetters() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(30);
        tm.begin();

        Connection connection = poolingDataSource1.getConnection();

        long start = System.nanoTime();
        PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Date date = new Date(0);
        for (int i = 0; i < 50000; i++) {
            stmt.setString(1, "foo");
            stmt.setInt(2, 999);
            stmt.setDate(3, date);
            stmt.setFloat(4, 9.99f);
            stmt.clearParameters();
        }
        long totalTime = System.nanoTime() - start;

        stmt.executeQuery();

        connection.close();

        tm.commit();
        tm.shutdown();
    }

    @Test
    public void testPrepares() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource2.getConnection();

        for (int i = 0; i < 1000; i++) {
            PreparedStatement prepareStatement = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
            assertFalse(prepareStatement.isClosed());
            prepareStatement.close();
            assertTrue(prepareStatement.isClosed());
        }

        connection.close();

        tm.commit();
        tm.shutdown();
    }

    @Test
    public void testCachedPrepared() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource1.getConnection();

        PreparedStatement prepareStatement1 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        PreparedStatement prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");

        Assert.assertSame(prepareStatement1.unwrap(PreparedStatement.class), prepareStatement2.unwrap(PreparedStatement.class));

        prepareStatement2.close();

        prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Assert.assertSame(prepareStatement1.unwrap(PreparedStatement.class), prepareStatement2.unwrap(PreparedStatement.class));

        prepareStatement1.close();
        prepareStatement2.close();

        connection.close();
        tm.shutdown();
    }

    @Test
    public void testCachedStatementsCanBeReused() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();
        try {
            Connection connection = poolingDataSource1.getConnection();
            try {
                PreparedStatement prepareStatement1 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
                assertFalse(prepareStatement1.isClosed());

                prepareStatement1.close();
                assertTrue(prepareStatement1.isClosed());

                PreparedStatement prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
                assertFalse(prepareStatement2.isClosed());
            } finally {
                connection.close();
            }
        } finally {
            tm.shutdown();
        }
    }

    @Test
    public void testUnCachedPrepared() throws Exception {
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(60);
        tm.begin();

        Connection connection = poolingDataSource2.getConnection();

        PreparedStatement prepareStatement1 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        PreparedStatement prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");

        Assert.assertNotSame(prepareStatement1.unwrap(PreparedStatement.class), prepareStatement2.unwrap(PreparedStatement.class));

        prepareStatement2.close();

        prepareStatement2 = connection.prepareStatement("SELECT 1 FROM nothing WHERE a=? AND b=? AND c=? AND d=?");
        Assert.assertNotSame(prepareStatement1.unwrap(PreparedStatement.class), prepareStatement2.unwrap(PreparedStatement.class));

        prepareStatement1.close();
        prepareStatement2.close();

        connection.close();
        tm.shutdown();
    }
}
