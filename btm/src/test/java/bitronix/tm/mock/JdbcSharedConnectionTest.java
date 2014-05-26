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
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transaction;
import java.sql.Connection;
import java.util.ArrayList;

/**
 *
 * @author Ludovic Orban
 */
public class JdbcSharedConnectionTest extends AbstractMockJdbcTest {
    private final static Logger log = LoggerFactory.getLogger(NewJdbcProperUsageMockTest.class);

    public void testSharedConnectionMultithreaded() throws Exception {
        if (log.isDebugEnabled()) { log.debug("*** Starting testSharedConnectionMultithreaded: getting TM"); }
        final BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) { log.debug("*** before begin"); }
        tm.begin();
        if (log.isDebugEnabled()) { log.debug("*** after begin"); }

        final Transaction suspended = tm.suspend();

        final ArrayList<Connection> twoConnections = new ArrayList<Connection>();
        Thread thread1 = new Thread() {
            @Override
        	public void run() {
        		try {
					tm.resume(suspended);
			        if (log.isDebugEnabled()) { log.debug("*** getting connection from DS1"); }
			        Connection connection = poolingDataSource1.getConnection();
			        connection.createStatement();
			        twoConnections.add(connection);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
        	}
        };
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread() {
            @Override
        	public void run() {
        		try {
					tm.resume(suspended);
			        if (log.isDebugEnabled()) { log.debug("*** getting connection from DS1"); }
			        Connection connection = poolingDataSource1.getConnection();
			        connection.createStatement();
			        twoConnections.add(connection);
			        tm.commit();
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
        	}
        };
        thread2.start();
        thread2.join();

        PooledConnectionProxy handle1 = (PooledConnectionProxy) twoConnections.get(0);
        PooledConnectionProxy handle2 = (PooledConnectionProxy) twoConnections.get(1);
        assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

    }

    public void testUnSharedConnection() throws Exception {
        if (log.isDebugEnabled()) { log.debug("*** Starting testUnSharedConnection: getting TM"); }
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) { log.debug("*** before begin"); }
        tm.begin();
        if (log.isDebugEnabled()) { log.debug("*** after begin"); }

        if (log.isDebugEnabled()) { log.debug("*** getting connection from DS2"); }
        Connection connection1 = poolingDataSource2.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) { log.debug("*** getting second connection from DS2"); }
        Connection connection2 = poolingDataSource2.getConnection();

        PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
        PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
        assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

        connection1.close();
        connection2.close();

        tm.commit();
    }

    public void testSharedConnectionInLocalTransaction() throws Exception {

        if (log.isDebugEnabled()) { log.debug("*** Starting testSharedConnectionInLocalTransaction: getting connection from DS1"); }
        Connection connection1 = poolingDataSource1.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) { log.debug("*** getting second connection from DS1"); }
        Connection connection2 = poolingDataSource1.getConnection();

        PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
        PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
        assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

        connection1.close();
        connection2.close();
    }

    public void testUnSharedConnectionInLocalTransaction() throws Exception {

        if (log.isDebugEnabled()) { log.debug("*** Starting testUnSharedConnectionInLocalTransaction: getting connection from DS2"); }
        Connection connection1 = poolingDataSource2.getConnection();
        // createStatement causes enlistment
        connection1.createStatement();

        if (log.isDebugEnabled()) { log.debug("*** getting second connection from DS2"); }
        Connection connection2 = poolingDataSource2.getConnection();

        PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
        PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
        assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

        connection1.close();
        connection2.close();
    }

    public void testSharedConnectionInGlobal() throws Exception {
        if (log.isDebugEnabled()) { log.debug("*** testSharedConnectionInGlobal: Starting getting TM"); }
        BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
        tm.setTransactionTimeout(120);

        if (log.isDebugEnabled()) { log.debug("*** before begin"); }
        tm.begin();
        if (log.isDebugEnabled()) { log.debug("*** after begin"); }

        if (log.isDebugEnabled()) { log.debug("*** getting connection from DS1"); }
        Connection connection1 = poolingDataSource1.getConnection();

        if (log.isDebugEnabled()) { log.debug("*** getting second connection from DS1"); }
        Connection connection2 = poolingDataSource1.getConnection();

        PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
        PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
        assertSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

        connection1.close();
        connection2.close();

        tm.commit();
    }
}
