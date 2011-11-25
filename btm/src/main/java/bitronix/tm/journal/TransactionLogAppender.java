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
package bitronix.tm.journal;

import bitronix.tm.TransactionManagerServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Set;

/**
 * Used to write {@link TransactionLogRecord} objects to a log file.
 *
 * @author lorban
 */
public class TransactionLogAppender {

    private final static Logger log = LoggerFactory.getLogger(TransactionLogAppender.class);

    /**
     * int-encoded "xntB" ASCII string.
     * This will be useful after swapping log files since we will potentially overwrite old logs not necessarily of the
     * same size. Very useful when debugging and eventually restoring broken log files.
     */
    public static final int END_RECORD = 0x786e7442;

    private final File file;
    private final FileChannel fc;
    private final FileLock lock;
    private final TransactionLogHeader header;
    private final long maxFileLength;

    private static volatile DiskForceBatcherThread diskForceBatcherThread;

    /**
     * Create an appender that will write to specified file up to the specified maximum length.
     * All disk access are synchronized arround the RandomAccessFile object, including header calls.
     * @param file the underlying File used to write to disk.
     * @param maxFileLength size of the file on disk that can never be bypassed.
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogAppender(File file, long maxFileLength) throws IOException {
        this.maxFileLength = maxFileLength;
        this.file = file;
        this.fc = new RandomAccessFile(file, "rw").getChannel();
        this.header = new TransactionLogHeader(fc, maxFileLength);
        this.lock = fc.tryLock(0, TransactionLogHeader.TIMESTAMP_HEADER, false);
        if (this.lock == null)
            throw new IOException("transaction log file " + file.getName() + " is locked. Is another instance already running?");

        spawnBatcherThread();
    }

    /**
     * Return a {@link TransactionLogHeader} that allows reading and controlling the log file's header.
     * @return this log file's TransactionLogHeader
     */
    public TransactionLogHeader getHeader() {
        return header;
    }

    /**
     * Write a {@link TransactionLogRecord} to disk.
     * @param tlog the record to write to disk.
     * @return true if there was room in the log file and the log was written, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    public boolean writeLog(TransactionLogRecord tlog) throws IOException {
        synchronized (fc) {
            int recordSize = tlog.calculateTotalRecordSize();
            long futureFilePosition = getHeader().getPosition() + recordSize;
            if (futureFilePosition >= maxFileLength) { // see TransactionLogHeader.setPosition() as it double-checks this
                if (log.isDebugEnabled()) log.debug("log file is full (size would be: " + futureFilePosition + ", max allowed: " + maxFileLength + ")");
                return false;
            }
            if (log.isDebugEnabled()) log.debug("between " + getHeader().getPosition() + " and " + futureFilePosition + ", writing " + tlog);

            ByteBuffer buf = ByteBuffer.allocate(recordSize);
            buf.putInt(tlog.getStatus());
            buf.putInt(tlog.getRecordLength());
            buf.putInt(tlog.getHeaderLength());
            buf.putLong(tlog.getTime());
            buf.putInt(tlog.getSequenceNumber());
            buf.putInt(tlog.getCrc32());
            buf.put((byte) tlog.getGtrid().getArray().length);
            buf.put(tlog.getGtrid().getArray());
            Set<String> uniqueNames = tlog.getUniqueNames();
            buf.putInt(uniqueNames.size());
            for (String uniqueName : uniqueNames) {
                buf.putShort((short) uniqueName.length());
                buf.put(uniqueName.getBytes());
            }
            buf.putInt(tlog.getEndRecord());

            buf.flip();
            while (buf.hasRemaining()) {
                this.fc.write(buf);
            }
            getHeader().goAhead(tlog.calculateTotalRecordSize());
            if (log.isDebugEnabled()) log.debug("disk journal appender now at position " + getHeader().getPosition());

            return true;
        }
    }

    /**
     * Close the appender and the underlying file.
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        synchronized (fc) {
            shutdownBatcherThread();

            getHeader().setState(TransactionLogHeader.CLEAN_LOG_STATE);
            fc.force(false);
            lock.release();
            fc.close();
        }
    }

    /**
     * Creates a cursor on this journal file allowing iteration of its records.
     * This opens a new read-only file descriptor independent of the write-only one
     * still used for writing transaction logs.
     * @return a TransactionLogCursor.
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogCursor getCursor() throws IOException {
        return new TransactionLogCursor(file);
    }

    /**
     * Force flushing the logs to disk
     * @throws IOException if an I/O error occurs.
     */
    public void force() throws IOException {
        if (!TransactionManagerServices.getConfiguration().isForcedWriteEnabled()) {
            if (log.isDebugEnabled()) log.debug("disk forces have been disabled");
            return;
        }

        if (!TransactionManagerServices.getConfiguration().isForceBatchingEnabled()) {
            if (log.isDebugEnabled()) log.debug("not batching disk force");
            doForce();
        }
        else {
            diskForceBatcherThread.enqueue(this);
        }
    }

    public String toString() {
        return "a TransactionLogAppender on " + file.getName();
    }


    protected void doForce() throws IOException {
        synchronized (fc) {
            if (log.isDebugEnabled()) log.debug("forcing log writing");
            fc.force(false);
            if (log.isDebugEnabled()) log.debug("done forcing log");
        }
    }

    private void spawnBatcherThread() {
        synchronized (TransactionLogAppender.class) {
            if (diskForceBatcherThread != null)
                return;

            if (log.isDebugEnabled()) log.debug("spawning disk force batcher thread");
            diskForceBatcherThread = DiskForceBatcherThread.getInstance();

            if (!TransactionManagerServices.getConfiguration().isForcedWriteEnabled()) {
                log.warn("transaction journal disk syncs have been disabled, transaction logs integrity is not guaranteed !");
                return;
            }
            if (!TransactionManagerServices.getConfiguration().isForceBatchingEnabled()) {
                log.warn("transaction journal disk syncs batching has been disabled, this will seriously impact performance !");
                return;
            }
        }
    }

    private void shutdownBatcherThread() {
        synchronized (TransactionLogAppender.class) {
            if (diskForceBatcherThread == null)
                return;

            if (log.isDebugEnabled()) log.debug("requesting disk force batcher thread to shutdown");
            diskForceBatcherThread.setAlive(false);
            diskForceBatcherThread.interrupt();
            do {
                try {
                    if (log.isDebugEnabled()) log.debug("waiting for disk force batcher thread to die");
                    diskForceBatcherThread.join();
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
            while (diskForceBatcherThread.isInterrupted());
            if (log.isDebugEnabled()) log.debug("disk force batcher thread has shutdown");

            diskForceBatcherThread = null;
        } // synchronized
    }

}
