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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.resource.MockJournal;
import bitronix.tm.mock.resource.jms.MockXAConnectionFactory;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Ludovic Orban
 */
public abstract class AbstractMockJmsTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(AbstractMockJmsTest.class);

    protected PoolingConnectionFactory poolingConnectionFactory1;
    protected PoolingConnectionFactory poolingConnectionFactory2;
    protected static final int POOL_SIZE = 5;
    protected static final String CONNECTION_FACTORY1_NAME = "pcf1";
    protected static final String CONNECTION_FACTORY2_NAME = "pcf2";

    @Override
    protected void setUp() throws Exception {
        poolingConnectionFactory1 = new PoolingConnectionFactory();
        poolingConnectionFactory1.setClassName(MockXAConnectionFactory.class.getName());
        poolingConnectionFactory1.setUniqueName(CONNECTION_FACTORY1_NAME);
        poolingConnectionFactory1.setAcquisitionTimeout(5);
        poolingConnectionFactory1.setMinPoolSize(POOL_SIZE);
        poolingConnectionFactory1.setMaxPoolSize(POOL_SIZE);
        poolingConnectionFactory1.init();

        poolingConnectionFactory2 = new PoolingConnectionFactory();
        poolingConnectionFactory2.setClassName(MockXAConnectionFactory.class.getName());
        poolingConnectionFactory2.setUniqueName(CONNECTION_FACTORY2_NAME);
        poolingConnectionFactory2.setAcquisitionTimeout(5);
        poolingConnectionFactory2.setMinPoolSize(POOL_SIZE);
        poolingConnectionFactory2.setMaxPoolSize(POOL_SIZE);
        poolingConnectionFactory2.init();

        // change disk journal into mock journal
        Field field = TransactionManagerServices.class.getDeclaredField("journalRef");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());

        TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(2);

        // start TM
        TransactionManagerServices.getTransactionManager();

        // clear event recorder list
        EventRecorder.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (log.isDebugEnabled()) { log.debug("*** tearDown rollback"); }
            TransactionManagerServices.getTransactionManager().rollback();
        } catch (Exception ex) {
            // ignore
        }
        poolingConnectionFactory1.close();
        poolingConnectionFactory2.close();

        TransactionManagerServices.getTransactionManager().shutdown();
    }
}
