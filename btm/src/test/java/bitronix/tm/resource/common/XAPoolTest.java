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
package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.utils.CryptoEngine;
import junit.framework.TestCase;

/**
 *
 * @author Ludovic Orban
 */
public class XAPoolTest extends TestCase {

    public void testBuildXAFactory() throws Exception {
        ResourceBean rb = new ResourceBean() {};

        rb.setMaxPoolSize(1);
        rb.setClassName(MockitoXADataSource.class.getName());
        rb.getDriverProperties().setProperty("userName", "java");
        rb.getDriverProperties().setProperty("password", "{DES}" + CryptoEngine.crypt("DES", "java"));

        XAPool<DummyResourceHolder, DummyStatefulHolder> xaPool = new XAPool<DummyResourceHolder, DummyStatefulHolder>(null, rb, null);
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