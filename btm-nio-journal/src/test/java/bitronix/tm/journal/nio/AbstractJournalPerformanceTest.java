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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.journal.NullJournal;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.Status;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Common peformance & load test to be used with all journal implementations.
 *
 * @author juergen kellerer, 2011-04-30
 */
public abstract class AbstractJournalPerformanceTest extends AbstractJournalTest {

    private static Set<String> resources = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "pooled-datasource",
            "jms-connection-pool",
            "xa-aware-cache"
    )));

    private static List<Set<String>> resourceNameSets = new ArrayList<Set<String>>();

    static {
        for (String resource : resources)
            resourceNameSets.add(Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(resource))));
    }

    protected int getLogCallsPerEmitter() {
        return 1000;
    }

    @Before
    public void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(true);
    }

    @Test
    public void testLogPerformance() throws Exception {
        journal.open();

        int concurrency = 128;
        UidGenerator.generateUid();
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);

        // Warming threads.
        List<Future<Object>> futures = executorService.invokeAll(Collections.nCopies(concurrency * 64, new Callable<Object>() {
            public Object call() throws Exception {
                return UidGenerator.generateUid();
            }
        }));
        for (Future<Object> future : futures) { future.get(); }

        // Testing now
        try {
            List<Callable<Integer>> tests = new ArrayList<Callable<Integer>>();
            TransactionEmitter concurrentDanglingEmitter = new TransactionEmitter(true);
            tests.add(concurrentDanglingEmitter);
            for (int i = 1; i < concurrency; i++)
                tests.add(new TransactionEmitter(false));

            long time = System.currentTimeMillis(), logCalls = 0;

            // Create dangling ids in the main emitter (at the beginning to see whether entries may get lost).
            TransactionEmitter firstDanglingEmitter = new TransactionEmitter(true);
            logCalls += firstDanglingEmitter.call();
            List<Uid> danglingUids = firstDanglingEmitter.getGeneratedUids();

            // Wait on the the emitters.
            for (Future<Integer> future : executorService.invokeAll(tests))
                logCalls += future.get();

            journal.force();
            danglingUids.addAll(concurrentDanglingEmitter.getGeneratedUids());

            double seconds = ((double) System.currentTimeMillis() - time) / 1000;
            System.out.printf("%s (threads=%d, fsync=%s): %d transactions, took %.2f seconds (%.2f tx/s)%n",
                    getClass().getSimpleName(), concurrency, TransactionManagerServices.getConfiguration().isForcedWriteEnabled(),
                    logCalls, seconds, logCalls / seconds);

            handleDanglingRecords(danglingUids);
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testLogPerformanceWithoutFsync() throws Exception {
        TransactionManagerServices.getConfiguration().setForcedWriteEnabled(false);
        testLogPerformance();
    }

    private void handleDanglingRecords(List<Uid> danglingUids) throws IOException {
        journal.close();
        journal.open();

        Map map = journal.collectDanglingRecords();
        int sizeBefore = map.size();

        for (Uid uid : danglingUids) {
            JournalRecord removed = (JournalRecord) map.remove(uid);
            if (removed == null)
                continue;
            // cleanup
            journal.log(Status.STATUS_COMMITTED, uid, removed.getUniqueNames());
        }

        int matchedUids = sizeBefore - map.size();

        System.out.printf("Created %d dangling records, the journal contained %d of them.%n",
                danglingUids.size(), matchedUids);

        journal.close();
        journal.open();

        if (!(journal instanceof NullJournal)) {
            assertEquals("Not all dangling IDs were matched.", danglingUids.size(), matchedUids);
            assertEquals("Not all dangling IDs were cleaned up.",
                    sizeBefore - matchedUids, journal.collectDanglingRecords().size());
        }
    }

    private class TransactionEmitter implements Callable<Integer> {

        boolean createDangling;
        List<Uid> generatedUids;

        private TransactionEmitter(boolean createDangling) {
            this.createDangling = createDangling;
        }

        public List<Uid> getGeneratedUids() {
            return generatedUids;
        }

        public Integer call() throws Exception {
            final int logCalls = getLogCallsPerEmitter();
            generatedUids = new ArrayList<Uid>(logCalls);

            for (int i = 0; i < logCalls; i++) {
                Uid uid = UidGenerator.generateUid();

                journal.log(Status.STATUS_ACTIVE, uid, resources);

                journal.log(Status.STATUS_PREPARING, uid, resources);
                journal.log(Status.STATUS_PREPARED, uid, resources);

                journal.log(Status.STATUS_COMMITTING, uid, resources);
                journal.force();

                if (!createDangling) {
                    for (Set<String> nameSet : resourceNameSets)
                        journal.log(Status.STATUS_COMMITTED, uid, nameSet);
                }

                generatedUids.add(uid);
            }
            return logCalls;
        }
    }
}
