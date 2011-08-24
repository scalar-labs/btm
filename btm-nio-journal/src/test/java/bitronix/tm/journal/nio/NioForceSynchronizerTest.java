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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the functionality of NioForceSynchronizer.
 *
 * @author juergen kellerer, 2011-05-29
 */
@Ignore
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
    volatile BlockingQueue<NioForceSynchronizer<Object>.ForceableElement> queue;

    final NioForceSynchronizer<Object> forceSynchronizer = new NioForceSynchronizer<Object>();

    @Before
    public void setUp() throws Exception {
        elements = new ArrayList<Object>();
        queue = new ArrayBlockingQueue<NioForceSynchronizer<Object>.ForceableElement>(10);
        for (int i = 0; i < 6; i++)
            elements.add(new Object());
    }

    @Test
    public void testCanEnlistAndUnWrapElements() throws Exception {
        for (Object element : elements)
            forceSynchronizer.enlistElement(element, queue);

        List<Object> enlistedElements = new ArrayList<Object>();
        NioForceSynchronizer.unwrap(queue, enlistedElements);

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
        for (int i = 0; i < 10000; i++) {
            setUp();

            boolean success = random.nextBoolean();
            for (Future<Boolean> future : (success ?
                    doTestWaitOnEnlistedWithSuccess() : doTestWaitOnEnlistedWithFailure()))
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
        final CountDownLatch enlistCountDown = new CountDownLatch(elements.size());

        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        for (final Object element : elements) {
            futures.add(service.submit(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    forceSynchronizer.enlistElement(element, queue);
                    enlistCountDown.countDown();
                    return forceSynchronizer.waitOnEnlisted();
                }
            }));
        }

        enlistCountDown.await();
        for (Future<?> future : futures) assertFalse(future.isDone());

        try {
            if (!forceSynchronizer.processEnlistedIfRequired(callable, queue))
                forceSynchronizer.processEnlisted(callable, queue);
        } catch (Exception e) {
            // ignore.
        }

        return futures;
    }
}
