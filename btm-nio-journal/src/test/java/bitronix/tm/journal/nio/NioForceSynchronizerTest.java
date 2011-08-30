/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Juergen Kellerer.
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

package bitronix.tm.journal.nio;

import bitronix.tm.journal.nio.util.SequencedBlockingQueue;
import bitronix.tm.journal.nio.util.SequencedQueueEntry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Tests the functionality of NioForceSynchronizer.
 *
 * @author juergen kellerer, 2011-05-29
 */
public class NioForceSynchronizerTest {

    static ExecutorService service;

    @BeforeClass
    public static void initService() {
        service = Executors.newFixedThreadPool(128);
    }

    @AfterClass
    public static void shutdownService() {
        service.shutdown();
    }

    volatile List<Object> elements;

    final SequencedBlockingQueue<Object> queue = new SequencedBlockingQueue<Object>();
    final NioForceSynchronizer forceSynchronizer = new NioForceSynchronizer(queue);

    @Before
    public void setUp() throws Exception {
        elements = new ArrayList<Object>();
        for (int i = 0; i < 6; i++)
            elements.add(new Object());
    }

    @Test
    public void testCanEnlistAndUnWrapElements() throws Exception {
        for (Object element : elements)
            queue.putElement(element);

        List<Object> enlistedElements = new ArrayList<Object>();
        queue.drainElementsTo(enlistedElements);

        assertArrayEquals(elements.toArray(), enlistedElements.toArray());
    }

    @Test
    public void testWaitOnEnlisted() throws Exception {
        List<Future<Boolean>> futures = doTestWaitOnEnlistedWithSuccess();
        for (Future<Boolean> future : futures)
            assertTrue(future.get());
    }

    @Test
    public void testWaitOnEnlistedReceivesFailures() throws Exception {
        List<Future<Boolean>> futures = doTestWaitOnEnlistedWithFailure();
        for (Future<Boolean> future : futures)
            assertFalse(future.get());
    }

    @Test
    public void testWaitOnEnlistedFailuresIntersectSuccess() throws Exception {
        final Random random = new Random();
        Map<Future<Boolean>, Boolean> expectedResults = new HashMap<Future<Boolean>, Boolean>();
        int successCount = 1000, errorCount = 1000;
        while (successCount > 0 || errorCount > 0) {
            setUp();

            final boolean success = random.nextBoolean();
            if (success) successCount--;
            else errorCount--;

            final List<Future<Boolean>> futures = success ?
                    doTestWaitOnEnlistedWithSuccess() :
                    doTestWaitOnEnlistedWithFailure();

            for (Future<Boolean> future : futures)
                expectedResults.put(future, success);
        }

        for (Map.Entry<Future<Boolean>, Boolean> entry : expectedResults.entrySet())
            assertEquals(entry.getValue(), entry.getKey().get());
    }

    private List<Future<Boolean>> doTestWaitOnEnlistedWithSuccess() throws Exception {
        return doTestWaitOnEnlisted(new Callable<Object>() {
            public Object call() throws Exception {
                return null;
            }
        });
    }

    private List<Future<Boolean>> doTestWaitOnEnlistedWithFailure() throws Exception {
        return doTestWaitOnEnlisted(new Callable<Object>() {
            public Object call() throws Exception {
                throw new Exception();
            }
        });
    }

    private List<Future<Boolean>> doTestWaitOnEnlisted(Callable<Object> callable) throws Exception {
        final List<Object> objects = elements;
        final CountDownLatch enlistCountDown = new CountDownLatch(objects.size());

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        for (final Object element : objects) {
            futures.add(service.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    queue.putElement(element);
                    enlistCountDown.countDown();
                    return forceSynchronizer.waitOnEnlisted();
                }
            }));
        }

        enlistCountDown.await();
        for (Future<?> future : futures) assertFalse(future.isDone());

        try {
            ArrayList<SequencedQueueEntry<Object>> entries = new ArrayList<SequencedQueueEntry<Object>>();
            queue.takeAndDrainElementsTo(entries, new ArrayList<Object>());
            if (!forceSynchronizer.processEnlistedIfRequired(callable, entries))
                forceSynchronizer.processEnlisted(callable, entries);
        } catch (Exception e) {
            // ignore.
        }

        return futures;
    }
}
