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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    final NioDanglingTransactions danglingTransactions = new NioDanglingTransactions();

    // Force related stuff
    final NioForceSynchronizer<NioJournalFileRecord> forceSynchronizer =
            new NioForceSynchronizer<NioJournalFileRecord>();

    // Worker
    NioJournalWritingThread journalWritingThread;

    File journalFilePath;
    NioJournalFile journalFile;
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

        final NioLogRecord record = new NioLogRecord(status, gtrid, uniqueNames);
        danglingTransactions.track(status, gtrid, record);

        if (log.isTraceEnabled())
            log.trace("Attempting to log a new transaction log record {}.", record);

        try {
            final NioJournalFileRecord fileRecord = journalFile.createEmptyRecord();
            record.encodeTo(fileRecord.createEmptyPayload(record.getStatus(), record.getEffectiveRecordLength()));
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
        // Weak check for open, avoiding synchronization here.
        // Gains performance under the assumption that the journal is opened
        // first before log calls are performed from multiple threads.
        return journalFile != null; //NOSONAR
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void open() throws IOException {
        journalFilePath = getJournalFilePath();
        long journalSize = TransactionManagerServices.getConfiguration().getMaxLogSizeInMb() *
                1024 * 1024 * 3; // doubling the value as we use only 1 file.

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

        for (NioJournalFileRecord fileRecord : journalFile.readAll()) {
            ByteBuffer buffer = fileRecord.getPayload();
            try {
                buffer.mark();
                NioLogRecord record = new NioLogRecord(buffer);
                if (!record.isCrc32Correct()) {
                    log.error("Transaction log entry {} loaded from journal {} fails CRC32 check. " +
                            "Discarding the entry.", record, journalFilePath);
                } else
                    danglingTransactions.track(record);
            } catch (Exception e) {
                buffer.reset();
                String contentString = NioJournalFileRecord.bufferToString(buffer);
                log.error("Transaction log entry buffer with content <{}> loaded from journal {} cannot be " +
                        "decoded. Discarding the entry.", contentString, journalFilePath);
            }
        }

        log.info("Found {} dangling transactions within the journal.", danglingTransactions.size());
        danglingTransactions.purgeTransactionsExceedingLifetime();

        journalWritingThread = new NioJournalWritingThread(danglingTransactions, journalFile,
                skipForce ? null : forceSynchronizer);
        log.info("Successfully started a new log appender on the journal file {}.", journalFilePath);

        if (log.isDebugEnabled())
            log.debug("Attempting to collect dangling transactions from the journal.");
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

        danglingTransactions.clear();
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
        return new HashMap<Uid, NioLogRecord>(danglingTransactions.getTracked());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournal{" +
                "journalFilePath=" + journalFilePath +
                ", skipForce=" + skipForce +
                ", danglingTransactions=" + danglingTransactions +
                ", forceSynchronizer=" + forceSynchronizer +
                ", journalWritingThread=" + journalWritingThread +
                ", journalFile=" + journalFile +
                '}';
    }
}
