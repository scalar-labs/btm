/*
 * Copyright (C) 2006-2014 Bitronix Software (http://www.bitronix.be)
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
package bitronix.tm.resource.jdbc.proxy;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 *
 * @author rankincj
 */
public class PreparedStatementJavaProxyTest {

    private JdbcPooledConnection connection;
    private PreparedStatement stmt;

    @Before
    public void setup() {
        connection = mock(JdbcPooledConnection.class);
        stmt = mock(PreparedStatement.class);
    }

    @Test
    public void testCachedStatementCanBeUnwrapped() throws SQLException {
        CacheKey key = new CacheKey("SELECT * FROM DUAL");
        PreparedStatement proxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(connection, stmt, key);
        assertTrue(proxy.isWrapperFor(PreparedStatement.class));
        assertSame(proxy.unwrap(PreparedStatement.class), stmt);
    }

    @Test
    public void testCachedStatementPretendsToClose() throws SQLException {
        CacheKey key = new CacheKey("SELECT * FROM DUAL");
        PreparedStatement proxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(connection, stmt, key);

        proxy.close();

        assertTrue(proxy.isClosed());
        verify(stmt, never()).close();
        verify(stmt).clearParameters();
        verify(stmt).clearWarnings();
        verify(stmt).clearBatch();
        verify(connection).putCachedStatement(key, stmt);
    }

    @Test
    public void testUncachedStatementCanBeUnwrapped() throws SQLException {
        PreparedStatement proxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(connection, stmt, null);
        assertTrue(proxy.isWrapperFor(PreparedStatement.class));
        assertSame(proxy.unwrap(PreparedStatement.class), stmt);
    }

    @Test
    public void testUncachedStatementReallyCloses() throws SQLException {
        PreparedStatement proxy = JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(connection, stmt, null);

        proxy.close();

        assertTrue(proxy.isClosed());
        verify(stmt).close();
    }

}
