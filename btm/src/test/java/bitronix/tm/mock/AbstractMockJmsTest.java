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
 * @author lorban
 */
public abstract class AbstractMockJmsTest extends TestCase {

    private final static Logger log = LoggerFactory.getLogger(AbstractMockJmsTest.class);

    protected PoolingConnectionFactory poolingConnectionFactory1;
    protected PoolingConnectionFactory poolingConnectionFactory2;
    protected static final int POOL_SIZE = 5;
    protected static final String CONNECTION_FACTORY1_NAME = "pcf1";
    protected static final String CONNECTION_FACTORY2_NAME = "pcf2";

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
        AtomicReference<Journal> journalRef = (AtomicReference<Journal>) field.get(TransactionManagerServices.class);
        journalRef.set(new MockJournal());

        // start TM
        TransactionManagerServices.getTransactionManager();

        // clear event recorder list
        EventRecorder.clear();
    }
    protected void tearDown() throws Exception {
        try {
            if (log.isDebugEnabled()) log.debug("*** tearDown rollback");
            TransactionManagerServices.getTransactionManager().rollback();
        } catch (Exception ex) {
            // ignore
        }
        poolingConnectionFactory1.close();
        poolingConnectionFactory2.close();
    }

}
