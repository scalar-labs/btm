/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2011, Bitronix Software.
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

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.Journal;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static javax.transaction.Status.STATUS_COMMITTING;
import static javax.transaction.Status.STATUS_ROLLING_BACK;

/**
 * Nio & 'java.util.concurrent' based implementation of a transaction journal.
 *
 * @author juergen kellerer, 2011-04-30
 * @see bitronix.tm.journal.Journal
 */
public class NioJournal implements Journal, NioJournalConstants {

    private final static Logger log = LoggerFactory.getLogger(NioJournal.class);

    /**
     * Returns the journal file used by this implementation.
     *
     * @return the journal file used by this implementation.
     */
    public static File getJournalFilePath() {
        Configuration config = TransactionManagerServices.getConfiguration();
        File part1File = new File(config.getLogPart1Filename());
        return new File(part1File.getParentFile(), "nio-" + part1File.getName());
    }

    // Session tracking
    final NioTrackedTransactions trackedTransactions = new NioTrackedTransactions();

    // Force related stuff
    final NioForceSynchronizer<NioJournalFileRecord> forceSynchronizer =
            new NioForceSynchronizer<NioJournalFileRecord>();

    // Worker
    volatile NioJournalWritingThread journalWritingThread;

    volatile File journalFilePath;
    volatile NioJournalFile journalFile;

    boolean skipForce = SKIP_FSYNC;

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(final int status, final Uid gtrid, Set uniqueNames) throws IOException {
        assertJournalIsOpen();

        if (gtrid == null)
            throw new IllegalArgumentException("GTRID cannot be set to null.");
        if (uniqueNames == null)
            uniqueNames = Collections.emptySet();

        @SuppressWarnings("unchecked")
        final NioJournalRecord record = new NioJournalRecord(status, gtrid, uniqueNames);
        trackedTransactions.track(status, gtrid, record);

        if (log.isTraceEnabled())
            log.trace("Attempting to log a new transaction log record {}.", record);

        try {
            final NioJournalFileRecord fileRecord = journalFile.createEmptyRecord();
            record.encodeTo(fileRecord.createEmptyPayload(record.getRecordLength()));
            forceSynchronizer.enlistElement(fileRecord, journalWritingThread.getIncomingQueue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }

    }

    private void assertJournalIsOpen() throws IOException {
        if (!isOpen())
            throw new IOException("The journal is not yet open or closed.");
    }

    public boolean isSkipForce() {
        return skipForce;
    }

    public void setSkipForce(boolean skipForce) {
        if (isOpen())
            throw new IllegalStateException("Cannot change skip force when the journal is already open.");
        this.skipForce = skipForce;
    }

    /**
     * Returns true if the journal is open.
     *
     * @return true if the journal is open.
     */
    public final boolean isOpen() {
        return journalFile != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void open() throws IOException {
        journalFilePath = getJournalFilePath();

         // HACK: Start
        long journalSize = TransactionManagerServices.getConfiguration().getMaxLogSizeInMb() *
                1024L * 1024L * 3L;
        // Default is 2, however 6mb seems to be the best trade-off between size and performance for this impl.
        // Configuration must be adjusted later to cover this correctly.
        // HACK: End

        if (log.isDebugEnabled()) {
            log.debug("Attempting to open the journal file {} with a min fixed size of {}mb",
                    journalFilePath, journalSize / 1024 / 1024);
        }

        if (log.isTraceEnabled())
            log.trace("Calling close in prior to open to ensure the journal wasn't opened before.");
        close();

        this.journalFile = new NioJournalFile(journalFilePath, journalSize);
        log.info("Successfully opened the journal file {}.", journalFilePath);

        if (log.isDebugEnabled())
            log.debug("Scanning for unfinished transactions within {}.", journalFilePath);

        for (NioJournalFileRecord fileRecord : journalFile.readAll(false)) {
            ByteBuffer buffer = fileRecord.getPayload();
            try {
                buffer.mark();
                NioJournalRecord record = new NioJournalRecord(buffer, fileRecord.isValid());
                if (!fileRecord.isValid()) {
                    log.error("Transaction log entry {} loaded from journal {} fails CRC32 check. " +
                            "Discarding the entry.", record, journalFilePath);
                } else
                    trackedTransactions.track(record);
            } catch (Exception e) {
                buffer.reset();
                String contentString = NioJournalFileRecord.bufferToString(buffer);
                log.error("Transaction log entry buffer with content <{}> loaded from journal {} cannot be " +
                        "decoded. Discarding the entry.", contentString, journalFilePath);
            }
        }

        log.info("Found {} unfinished transactions within the journal.", trackedTransactions.size());
        trackedTransactions.purgeTransactionsExceedingLifetime();

        journalWritingThread = new NioJournalWritingThread(trackedTransactions, journalFile,
                skipForce ? null : forceSynchronizer);
        log.info("Successfully started a new log appender on the journal file {}.", journalFilePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws IOException {
        closeLogAppender();

        if (journalFile != null) {
            if (log.isDebugEnabled())
                log.debug("Attempting to close the nio transaction journal.");
            journalFile.close();
            journalFile = null;
            log.info("Closed the nio transaction journal.");
        }

        trackedTransactions.clear();
    }

    private synchronized void closeLogAppender() throws IOException {
        if (journalWritingThread != null) {
            if (log.isDebugEnabled())
                log.debug("Attempting to close the nio log appender.");

            journalWritingThread.close();
            journalWritingThread = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            log.info("Shutting down the nio transaction journal on {}.", journalFilePath);
            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void force() throws IOException {
        if (skipForce)
            return;

        assertJournalIsOpen();

        if (!forceSynchronizer.waitOnEnlisted()) {
            throw new IOException("Forced failed on the latest entry logged within this thread, " +
                    "see log output for more details.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map collectDanglingRecords() throws IOException {
        assertJournalIsOpen();
        final Map<Uid, NioJournalRecord> tracked = trackedTransactions.getTracked();
        final Map<Uid, NioJournalRecord> dangling = new HashMap<Uid, NioJournalRecord>(tracked.size());

        for (Map.Entry<Uid, NioJournalRecord> entry : tracked.entrySet()) {
            if (entry.getValue().getStatus() == STATUS_COMMITTING ||
                    entry.getValue().getStatus() == STATUS_ROLLING_BACK) {
                dangling.put(entry.getKey(), entry.getValue());
            }
        }

        return dangling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator readRecords(boolean includeInvalid) throws IOException {
        assertJournalIsOpen();
        return journalFile.readAll(includeInvalid).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournal{" +
                "journalFilePath=" + journalFilePath +
                ", skipForce=" + skipForce +
                ", trackedTransactions=" + trackedTransactions +
                ", forceSynchronizer=" + forceSynchronizer +
                ", journalWritingThread=" + journalWritingThread +
                ", journalFile=" + journalFile +
                '}';
    }
}
