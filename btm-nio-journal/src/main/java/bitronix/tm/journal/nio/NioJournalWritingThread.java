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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static bitronix.tm.journal.nio.NioJournalFileRecord.calculateRequiredBytes;
import static bitronix.tm.journal.nio.NioJournalFileRecord.disposeAll;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Is the single thread that writes to the journal.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalWritingThread extends Thread implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioJournalWritingThread.class);
    private static final boolean trace = log.isTraceEnabled();

    /**
     * Constructs and starts a thread on the given journal that handles writes.
     * <p/>
     * This method does not return until the thread was started and is ready to listed on the given queue.
     * Threads that were created with this method are guaranteed to process all elements that were contained in the given
     * queue just before {@link #shutdown()} is called.
     *
     * @param transactions  the shared map of dangling transactions.
     * @param journal       the journal to operate on.
     * @param synchronizer  the synchronizer used allowing logging threads to wait on the force command.
     * @param incomingQueue the queue instance to operate on.
     * @return returns a started journal writing thread in running or waiting state.
     * @throws InterruptedException In case of the calling thread was interrupted before the journal writer switched to running mode.
     */
    public static NioJournalWritingThread newRunningInstance(NioTrackedTransactions transactions, NioJournalFile journal, NioForceSynchronizer synchronizer,
                                                             SequencedBlockingQueue<NioJournalFileRecord> incomingQueue) throws InterruptedException {
        final NioJournalWritingThread thread = new NioJournalWritingThread(transactions, journal, synchronizer, incomingQueue);
        synchronized (thread) {
            try {
                while (!thread.running)
                    thread.wait();
            } catch (InterruptedException e) {
                log.info("The attempt to open a journal writer on file " + journal.getFile() + " was interrupted before the writer was ready. " +
                        "Closing the uninitialized writer now.");
                // we need to shutdown the thread as we only the caller was interrupted so far.
                thread.shutdown();
                throw e;
            }
        }
        return thread;
    }

    private final List<SequencedQueueEntry<NioJournalFileRecord>> pendingEntriesToWorkOn =
            new ArrayList<SequencedQueueEntry<NioJournalFileRecord>>(CONCURRENCY);

    private boolean running;
    private volatile boolean closeRequested;

    private final NioForceSynchronizer forceSynchronizer;
    private final SequencedBlockingQueue<NioJournalFileRecord> incomingQueue;

    private final NioJournalFile journalFile;
    private final NioTrackedTransactions trackedTransactions;

    private long processedCount;

    private final Callable throwException = new Callable() {
        public Object call() throws Exception {
            throw new Exception();
        }
    };

    private final Callable forceJournalFile = new Callable() {
        public Object call() throws Exception {
            journalFile.force();
            return null;
        }
    };

    private NioJournalWritingThread(NioTrackedTransactions trackedTransactions, NioJournalFile journalFile,
                                    NioForceSynchronizer forceSynchronizer, SequencedBlockingQueue<NioJournalFileRecord> incomingQueue) {
        super("Bitronix - Nio Transaction Journal - JournalWriter");
        this.trackedTransactions = trackedTransactions;
        this.journalFile = journalFile;
        this.forceSynchronizer = forceSynchronizer;
        this.incomingQueue = incomingQueue;
        start();
    }

    /**
     * Attempts to shutdown the thread gracefully.
     */
    public synchronized void shutdown() {
        closeRequested = true;

        if (!running)
            return;

        try {
            for (int i = 0; i < 60 && running; i++) {
                final boolean isWaiting = getState() != State.RUNNABLE, forceInterrupt = i >= 59;

                if (forceInterrupt || isWaiting) {
                    final int entries = pendingEntriesToWorkOn.size();
                    final boolean doInterrupt = forceInterrupt || (incomingQueue.isEmpty() && entries == 0);

                    if (isWaiting) {
                        if (log.isDebugEnabled()) {
                            if (doInterrupt)
                                log.debug("Interrupting journal writer that is currently in waiting state.");
                            else
                                log.debug("Waiting on journal writer that has " + entries + " unwritten transactions in the queue.");
                        }
                    } else if (entries > 0) {
                        log.error("Interrupting journal writer that is still processing " + entries + " entries after requesting its shutdown. " +
                                "This may compromise transactions during the next recovery run.");
                    }

                    // If we'd call interrupt(), the channel is no longer writable (InterruptedIO...).
                    // We may only interrupt if the thread is currently waiting and does not have any pending jobs

                    if (doInterrupt)
                        interrupt();
                }

                int maxWait = 10; // 1 second
                while (running && maxWait-- > 0)
                    wait(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (running) {
            final String msg = "Failed to shutdown the nio log appender on journal " + journalFile.getFile() + ". The thread is still alive.";
            log.error(msg);
            throw new IllegalStateException(msg);
        } else {
            log.info("Closed the nio log appender on journal " + journalFile.getFile() + " after processing " + processedCount + " log entries.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            final List<NioJournalFileRecord> recordsToWorkOn = new ArrayList<NioJournalFileRecord>(CONCURRENCY);
            while (!isInterrupted() && !closeRequested) {

                // We are in running mode if we process at least those records that were already queued.
                synchronized (this) {
                    running = true;
                    notifyAll();

                    // TODO: This is not yet bullet proof.. and requires another rework as race conditions are still possible.

                }

                try {
                    pendingEntriesToWorkOn.clear();

                    for (int iterationsBeforeForce = WRITE_ITERATIONS_BEFORE_FORCE; iterationsBeforeForce > 0; iterationsBeforeForce--) {
                        boolean wasInterrupted = interrupted();
                        try {
                            recordsToWorkOn.clear();
                            final boolean blockForRecords = !wasInterrupted && !closeRequested && iterationsBeforeForce == WRITE_ITERATIONS_BEFORE_FORCE;
                            if (collectWork(recordsToWorkOn, blockForRecords) == 0)
                                break;

                            if (trace) { log.trace("Attempting to write " + recordsToWorkOn.size() + " log entries into the journal file."); }

                            // TODO: Handle the case that growing is not working and not enough remaining space
                            // TODO: is available. ClassicJournal blocks until TX are committed. This should do
                            // TODO: the same without causing a deadlock.

                            // Attempt to clear interrupt before starting to write.
                            if (!wasInterrupted) { wasInterrupted = interrupted(); }

                            handleJournalRollover(recordsToWorkOn);

                            journalFile.write(recordsToWorkOn);
                            processedCount += recordsToWorkOn.size();
                        } catch (InterruptedException e) {
                            throw e; // NOSONAR - Must be handled in upper block to ensure we handle interruptions at one location only.
                        } catch (Exception e) {
                            log.error("Failed storing " + recordsToWorkOn.size() + " transaction log records.", e);
                            for (NioJournalFileRecord record : recordsToWorkOn)
                                log.error("Failed storing transaction " + new NioJournalRecord(record.getPayload(), record.isValid()) + ".");
                        } finally {
                            try {
                                disposeAll(recordsToWorkOn);
                            } finally {
                                if (wasInterrupted) { interrupt(); }
                            }
                        }

                        if (wasInterrupted)
                            throw new InterruptedException();
                    }

                    tryForceAndReportAllRemainingElementsAsSuccess();

                } catch (InterruptedException t) {
                    if (recordsToWorkOn.isEmpty()) {
                        if (log.isDebugEnabled()) { log.debug("Cleanly interrupted log appender."); }
                    } else {
                        log.warn("Interrupted log appender with " + recordsToWorkOn.size() + " entries still in queue.");
                        reportAllRemainingElementsAsFailed();
                    }
                    interrupt();
                } catch (Throwable t) { //NOSONAR: The log writer must not stop execution even when a fatal error occurred.
                    log.error("Fatal error when storing logs. Reporting " + pendingEntriesToWorkOn.size() + " remaining elements as failures.", t);

                    // Waiting for 1 second to avoid running in endless loops when every invocation causes a fatal error.
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                } finally {
                    reportAllRemainingElementsAsFailed();
                }
            }
        } finally {
            synchronized (this) {
                running = false;
                notifyAll();
            }
        }
    }

    private int collectWork(List<NioJournalFileRecord> recordsToWorkOn, boolean blockForRecords) throws InterruptedException {
        int collectCount = 0;

        if (blockForRecords) {
            final long time = System.nanoTime();

            long remainingWriteDelay = Long.MAX_VALUE;
            do {
                if (remainingWriteDelay == Long.MAX_VALUE)
                    collectCount += incomingQueue.takeAndDrainElementsTo(pendingEntriesToWorkOn, recordsToWorkOn);
                else
                    collectCount += incomingQueue.pollAndDrainElementsTo(pendingEntriesToWorkOn, recordsToWorkOn, 5, MILLISECONDS);

                remainingWriteDelay = Math.max(0, WRITE_DELAY - NANOSECONDS.toMillis(System.nanoTime() - time));

            } while (remainingWriteDelay > 0 && collectCount < CONCURRENCY &&
                    !isInterrupted() && !closeRequested &&
                    (forceSynchronizer == null || forceSynchronizer.getNumberOfWaitingThreads() == 0));
        } else {
            // try to collect more entries that queued up during the time that the last write occurred.
            collectCount = incomingQueue.drainElementsTo(pendingEntriesToWorkOn, recordsToWorkOn);
        }

        return collectCount;
    }

    private void tryForceAndReportAllRemainingElementsAsSuccess() throws Exception {
        if (forceSynchronizer != null)
            forceSynchronizer.processEnlistedIfRequired(forceJournalFile, pendingEntriesToWorkOn);
        pendingEntriesToWorkOn.clear();
    }

    private void reportAllRemainingElementsAsFailed() {
        try {
            if (forceSynchronizer != null)
                forceSynchronizer.processEnlisted(throwException, pendingEntriesToWorkOn);
        } catch (Exception e) {
            // ignore.
        } finally {
            pendingEntriesToWorkOn.clear();
        }
    }

    private void handleJournalRollover(List<NioJournalFileRecord> buffersToWorkOn) throws IOException {
        final int requiredBytes = calculateRequiredBytes(buffersToWorkOn);
        final long remainingCapacity = journalFile.remainingCapacity();

        if (requiredBytes > remainingCapacity) {
            if (log.isDebugEnabled()) {
                log.debug("Detected that the journal " + journalFile.getFile() + " must be rolled over (requested " + requiredBytes + " bytes, " +
                        "remaining capacity " + remainingCapacity + " bytes). Performing the rollover now.");
            }

            journalFile.rollover();

            dumpUnfinishedTransactionsToJournal();
            attemptToGrowJournalIfRequired(requiredBytes);
        }
    }

    private void dumpUnfinishedTransactionsToJournal() throws IOException {
        trackedTransactions.purgeTransactionsExceedingLifetime();

        final List<NioJournalRecord> records = new ArrayList<NioJournalRecord>(trackedTransactions.getTracked().values());

        if (!records.isEmpty()) {
            final boolean debug = log.isDebugEnabled();
            if (debug) { log.debug("Transferring " + records.size() + " unfinished transactions to the head of the journal after performing the rollover."); }


            final List<NioJournalFileRecord> chunks = new ArrayList<NioJournalFileRecord>(CONCURRENCY);
            for (NioJournalRecord record : records) {
                final NioJournalFileRecord fileRecord = journalFile.createEmptyRecord();
                record.encodeTo(fileRecord.createEmptyPayload(record.getRecordLength()), true);
                chunks.add(fileRecord);

                if (chunks.size() == CONCURRENCY)
                    writeUnfinishedTransactionsChunks(chunks);
            }

            if (!chunks.isEmpty())
                writeUnfinishedTransactionsChunks(chunks);

            if (debug) { log.debug("Successfully wrote " + records.size() + " unfinished transactions to the journal after the rollover."); }
        }
    }

    private void writeUnfinishedTransactionsChunks(List<NioJournalFileRecord> chunks) throws IOException {
        attemptToGrowJournalIfRequired(calculateRequiredBytes(chunks));

        journalFile.write(chunks);
        processedCount += chunks.size();
        disposeAll(chunks);
        chunks.clear();
    }

    private void attemptToGrowJournalIfRequired(final long minRequiredCapacity) throws IOException {
        final long journalFileSize = journalFile.getSize();
        final long remainingCapacityGrowOffset = Math.max(minRequiredCapacity, journalFileSize * (long) JOURNAL_GROW_OFFSET);
        final long initialRemainingCapacity = journalFile.remainingCapacity();
        final boolean growRequired = initialRemainingCapacity < remainingCapacityGrowOffset;

        if (growRequired) {
            boolean success = false;

            if (JOURNAL_GROW_RATIO > 1D) {
                long newSize = journalFileSize, usedSize = journalFileSize - initialRemainingCapacity;
                do {
                    newSize *= JOURNAL_GROW_RATIO;
                } while ((newSize - usedSize) < remainingCapacityGrowOffset);

                final long newSizeInMb = newSize / 1024 / 1024;

                log.warn("The configured journal size of " + (journalFileSize / 1024 / 1024) + "mb is too small to keep all unfinished " +
                        "transactions. Increasing the size of " + journalFile.getFile() + " to " + newSizeInMb + "mb");

                try {
                    journalFile.growJournal(newSize);
                    log.info("Successfully increased the journal size to " + newSizeInMb + "mb (remaining capacity is " +
                            (journalFile.remainingCapacity() / 1024 / 1024) + "mb)");
                    success = true;
                } catch (IOException e) {
                    log.warn("Failed increased the journal size to " + newSizeInMb + "mb. " +
                            (journalFile.remainingCapacity() / 1024 / 1024) + "mb capacity is remaining in the pre-allocated block)", e);
                }
            }

            if (!success && journalFile.remainingCapacity() < minRequiredCapacity) {
                log.error("Growing the journal " + journalFile.getFile() + " is required to service the next write request of " +
                        minRequiredCapacity + " bytes, however the remaining journal capacity is " + journalFile.remainingCapacity() + ". " +
                        "Increase the initial journal size if the ability to grow the journal was disabled or check the remaining capacity " +
                        "of the storage device.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournalWritingThread{" +
                "pendingEntriesToWorkOn.size=" + pendingEntriesToWorkOn.size() +
                ", processedCount=" + processedCount +
                ", forceSynchronizer=" + forceSynchronizer +
                ", state=" + getState() +
                ", closeRequested=" + closeRequested +
                '}';
    }
}
