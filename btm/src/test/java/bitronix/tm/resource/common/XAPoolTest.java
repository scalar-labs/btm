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
package bitronix.tm.resource.common;

import junit.framework.TestCase;
import bitronix.tm.*;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.utils.CryptoEngine;

/**
 *
 * @author lorban
 */
public class XAPoolTest extends TestCase {

    public void testBuildXAFactory() throws Exception {
        ResourceBean rb = new ResourceBean() {};

        rb.setMaxPoolSize(1);
        rb.setClassName(MockitoXADataSource.class.getName());
        rb.getDriverProperties().setProperty("userName", "java");
        rb.getDriverProperties().setProperty("password", "{DES}" + CryptoEngine.crypt("DES", "java"));

        XAPool xaPool = new XAPool(null, rb);
        assertEquals(0, xaPool.totalPoolSize());
        assertEquals(0, xaPool.inPoolSize());

        MockitoXADataSource xads = (MockitoXADataSource) xaPool.getXAFactory();
        assertEquals("java", xads.getUserName());
        assertEquals("java", xads.getPassword());
    }

    public void testNoRestartOfTaskSchedulerDuringClose() throws Exception {
        PoolingDataSource pds = new PoolingDataSource();
        pds.setClassName(MockitoXADataSource.class.getName());
        pds.setMaxPoolSize(1);
        pds.setUniqueName("mock");
        pds.init();

        BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
        btm.shutdown();

        pds.close();

        assertFalse(TransactionManagerServices.isTaskSchedulerRunning());
    }

}