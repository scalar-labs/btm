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
import java.util.concurrent.atomic.AtomicBoolean;

import static bitronix.tm.journal.nio.NioJournalFileRecord.calculateRequiredBytes;
import static bitronix.tm.journal.nio.NioJournalFileRecord.disposeAll;

/**
 * Is the single thread that writes to the journal.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalWritingThread extends Thread implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioJournalWritingThread.class);
    private static final boolean trace = log.isTraceEnabled();

    private final List<SequencedQueueEntry<NioJournalFileRecord>> pendingEntriesToWorkOn =
            new ArrayList<SequencedQueueEntry<NioJournalFileRecord>>(CONCURRENCY);

    private final AtomicBoolean closeRequested = new AtomicBoolean();

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

    /**
     * Constructs and starts a thread on the given journal that handles writes.
     *
     * @param trackedTransactions the shared map of dangling transactions.
     * @param journalFile         the journal to operate on.
     * @param forceSynchronizer   the synchronizer used allowing logging threads to wait on the force command.
     * @param incomingQueue       the queue instance to operate on.
     */
    public NioJournalWritingThread(NioTrackedTransactions trackedTransactions,
                                   NioJournalFile journalFile,
                                   NioForceSynchronizer forceSynchronizer,
                                   SequencedBlockingQueue<NioJournalFileRecord> incomingQueue) {
        super("Bitronix - Nio Transaction Journal - JournalWriter");
        this.trackedTransactions = trackedTransactions;
        this.journalFile = journalFile;
        this.forceSynchronizer = forceSynchronizer;
        this.incomingQueue = incomingQueue;
        start();
    }

    /**
     * Attempts to close the thread gracefully.
     */
    public synchronized void close() {
        closeRequested.set(true);

        try {
            for (int i = 0; i < 60 && isAlive(); i++) {
                boolean isWaiting = getState() != State.RUNNABLE,
                        forceInterrupt = i >= 59;

                if (forceInterrupt || isWaiting) {
                    boolean doInterrupt = forceInterrupt || (incomingQueue.isEmpty() && pendingEntriesToWorkOn.isEmpty());
                    if (isWaiting) {
                        if (log.isDebugEnabled())
                            log.debug("Interrupting journal writer that is currently in waiting state.");
                    } else {
                        if (doInterrupt) {
                            log.warn("Interrupting on journal writer that is still processing {} " +
                                    "entries after requesting its shutdown.", pendingEntriesToWorkOn.size());
                        } else {
                            log.error("Waiting on journal writer that has {} unwritten transactions in " +
                                    "the queue. This may compromise transactions during the next recovery run.",
                                    pendingEntriesToWorkOn.size());
                        }
                    }

                    // If we'd interrupt, the channel is no longer writable. We may only interrupt if the
                    // thread is currently waiting and does not have any pending jobs or if we

                    if (doInterrupt)
                        interrupt();
                }

                int maxWait = 10; // 1 second
                while (isAlive() && maxWait-- > 0)
                    wait(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        if (isAlive()) {
            final String msg = "Failed to close the nio log appender on journal " +
                    journalFile.getFile() + ". The thread is still alive.";
            log.error(msg);
            throw new IllegalStateException(msg);
        } else {
            log.info("Closed the nio log appender on journal {} after processing {} log entries.",
                    journalFile.getFile(), processedCount);
        }
    }

    private int collectWork(final List<NioJournalFileRecord> recordsToWorkOn,
                            final boolean block) throws InterruptedException {

        recordsToWorkOn.clear();

        if (block)
            return incomingQueue.takeAndDrainElementsTo(pendingEntriesToWorkOn, recordsToWorkOn);
        else
            return incomingQueue.drainElementsTo(pendingEntriesToWorkOn, recordsToWorkOn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        final List<NioJournalFileRecord> recordsToWorkOn = new ArrayList<NioJournalFileRecord>(CONCURRENCY);

        while (!isInterrupted() && !closeRequested.get()) {
            try {
                pendingEntriesToWorkOn.clear();

                // Note: Be careful with ordering the loop condition statements below.
                //       collectWork() must not be called if the loop is aborted by any other
                //       condition than collect work returning '0'.

                for (int iterationsBeforeForce = WRITE_ITERATIONS_BEFORE_FORCE;
                     iterationsBeforeForce > 0; iterationsBeforeForce--) {

                    try {
                        if (collectWork(recordsToWorkOn, iterationsBeforeForce == WRITE_ITERATIONS_BEFORE_FORCE) == 0)
                            break;

                        if (trace) {
                            log.trace("Attempting to write {} log entries into the journal file.",
                                    recordsToWorkOn.size());
                        }

                        // TODO: Handle the case that growing is not working and not enough remaining space
                        // TODO: is available. ClassicJournal blocks until TX are committed. This should do
                        // TODO: the same without causing a deadlock.

                        handleJournalRollover(recordsToWorkOn);

                        journalFile.write(recordsToWorkOn);
                        processedCount += recordsToWorkOn.size();
                    } catch (Exception e) {
                        log.error("Failed storing " + recordsToWorkOn.size() + " transaction log records.", e);
                        for (NioJournalFileRecord record : recordsToWorkOn) {
                            log.error("Failed storing transaction {}.",
                                    new NioJournalRecord(record.getPayload(), record.isValid()));
                        }
                    } finally {
                        disposeAll(recordsToWorkOn);
                    }
                }

                if (forceSynchronizer != null)
                    forceSynchronizer.processEnlisted(forceJournalFile, pendingEntriesToWorkOn);

            } catch (InterruptedException t) {
                if (recordsToWorkOn.isEmpty()) {
                    if (log.isDebugEnabled())
                        log.debug("Cleanly interrupted log appender.");
                } else {
                    log.warn("Interrupted log appender with {} entries still in queue.", recordsToWorkOn.size());
                    reportAllRemainingElementsAsFailed();
                }
                interrupt();
            } catch (Throwable t) { //NOSONAR: The log writer must not stop execution even when a fatal error occurred.
                reportAllRemainingElementsAsFailed();
                log.error("Fatal error when storing logs. Reporting all remaining elements as failures.", t);

                // Waiting for 1 second to avoid running in endless loops when every invocation causes a fatal error.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        synchronized (this) {
            notifyAll();
        }
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
                log.debug("Detected that the journal {} must be rolled over (requested {}bytes, " +
                        "remaining capacity {}bytes). Performing the rollover now.", new Object[]{
                        journalFile.getFile(), requiredBytes, remainingCapacity});
            }

            journalFile.rollover();

            dumpDanglingTransactionsToJournal();
            attemptToGrowJournalIfRequired();
        }
    }

    private void dumpDanglingTransactionsToJournal() throws IOException {
        trackedTransactions.purgeTransactionsExceedingLifetime();

        final List<NioJournalRecord> records = new ArrayList<NioJournalRecord>(
                trackedTransactions.getTracked().values());

        if (!records.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("Adding {} unfinished transactions to the journal after performing the rollover.",
                        records.size());
            }

            final List<NioJournalFileRecord> chunks = new ArrayList<NioJournalFileRecord>(CONCURRENCY);
            for (NioJournalRecord record : records) {
                final NioJournalFileRecord fileRecord = journalFile.createEmptyRecord();
                record.encodeTo(fileRecord.createEmptyPayload(record.getRecordLength()));
                chunks.add(fileRecord);

                if (chunks.size() == CONCURRENCY) {
                    journalFile.write(chunks);
                    processedCount += chunks.size();
                    disposeAll(chunks);
                    chunks.clear();
                }
            }

            if (!chunks.isEmpty()) {
                journalFile.write(chunks);
                processedCount += chunks.size();
                disposeAll(chunks);
            }

            if (log.isDebugEnabled()) {
                log.debug("Successfully wrote {} unfinished transactions to the journal " +
                        "after the rollover.", records.size());
            }
        }
    }

    private void attemptToGrowJournalIfRequired() throws IOException {
        long growOffsetSize = (long) (journalFile.getSize() * JOURNAL_GROW_OFFSET);
        if (journalFile.remainingCapacity() < growOffsetSize) {
            long newSize = (long) (journalFile.getSize() * JOURNAL_GROW_RATIO),
                    newSizeInMb = newSize / 1024 / 1024;
            log.warn("The configured journal size of {}mb is too small to keep all unfinished " +
                    "transactions. Increasing the size to {}mb",
                    journalFile.getSize() / 1024 / 1024, newSizeInMb);

            try {
                journalFile.growJournal(newSize);
                log.info("Successfully increased the journal size to {}mb (remaining capacity is {}mb)",
                        newSizeInMb, journalFile.remainingCapacity() / 1024 / 1024);
            } catch (IOException e) {
                log.warn("Failed increased the journal size to " + newSizeInMb + "mb (is the disk full?). " +
                        (journalFile.remainingCapacity() / 1024 / 1024) + "mb capacity is remaining in the " +
                        "pre-allocated block)", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournalWritingThread{" +
                ", pendingEntriesToWorkOn.size=" + pendingEntriesToWorkOn.size() +
                ", processedCount=" + processedCount +
                ", forceSynchronizer=" + forceSynchronizer +
                ", state=" + getState() +
                ", closeRequested=" + closeRequested.get() +
                '}';
    }
}
