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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private RandomAccessFile randomeAccessFile;
    private final FileChannel fc;
    private final FileLock lock;
    private final TransactionLogHeader header;
	private long maxFileLength;
	private Set<Long> outstandingHeaderWrites;
	private long position;

    /**
     * Create an appender that will write to specified file up to the specified maximum length.
     * @param file the underlying File used to write to disk.
     * @param maxFileLength size of the file on disk that can never be bypassed.
     * @throws IOException if an I/O error occurs.
     */
    public TransactionLogAppender(File file, long maxFileLength) throws IOException {
        this.file = file;
        this.randomeAccessFile = new RandomAccessFile(file, "rw");
        this.fc = randomeAccessFile.getChannel();
        this.header = new TransactionLogHeader(fc, maxFileLength);
        this.maxFileLength = maxFileLength;
        this.lock = fc.tryLock(0, TransactionLogHeader.TIMESTAMP_HEADER, false);
        if (this.lock == null)
            throw new IOException("transaction log file " + file.getName() + " is locked. Is another instance already running?");

        this.outstandingHeaderWrites = new HashSet<Long>();
        this.position = header.getPosition();
    }

    /**
     * Get the current file position and advance the position by recordSize if
     * the maximum file length won't be exceeded.
     * @param recordSize the record size
     * @return the position the next write should be written at, or -1 if the
     *   maximum file length would be exceeded
     * @throws IOException if an I/O error occurs
     */
    protected long getPositionAndAdvance(int recordSize) throws IOException {
    	if (position + recordSize > maxFileLength) {
    		return -1;
    	}

    	long newPosition = position;
    	position += recordSize;
    	synchronized (outstandingHeaderWrites) {
    		outstandingHeaderWrites.add(newPosition);			
		}

    	return newPosition;
    }

    /**
     * Write a {@link TransactionLogRecord} to disk.
     * @param tlog the record to write to disk.
     * @return true if there was room in the log file and the log was written, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeLog(TransactionLogRecord tlog) throws IOException {
    	int recordSize = tlog.calculateTotalRecordSize();
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

        if (log.isDebugEnabled()) log.debug("between " + tlog.getWritePosition() + " and " + tlog.getWritePosition() + tlog.calculateTotalRecordSize() + ", writing " + tlog);

        final long writePosition = tlog.getWritePosition();
        while (buf.hasRemaining()) {
        	fc.write(buf, writePosition + buf.position());
        }

        synchronized (outstandingHeaderWrites) {
        	outstandingHeaderWrites.remove(writePosition);
        	if (outstandingHeaderWrites.isEmpty()) {
        		header.setPosition(position);
        	}
		}
    }

    /**
     * Return a {@link TransactionLogHeader} that allows reading and controlling the log file's header.
     * @return this log file's TransactionLogHeader
     * @throws IOException if an I/O error occurs
     */
    void rewind() throws IOException {
        header.rewind();
        position = header.getPosition();
    }

    /**
     * Get the log file header timestamp.
     * @return the log file header timestamp
     */
    long getTimestamp() {
    	return header.getTimestamp();
    }

    /**
     * Set the log file header timestamp
     * @param timestamp the timestamp to set in the log
     * @throws IOException if an I/O error occurs
     */
    void setTimestamp(long timestamp) throws IOException {
    	header.setTimestamp(timestamp);
    }

    /**
     * Get STATE_HEADER.
     * @return the STATE_HEADER value.
     */
    public byte getState() {
        return header.getState();
    }

    /**
     * Set STATE_HEADER.
     * @param state the STATE_HEADER value.
     * @throws IOException if an I/O error occurs.
     */
    public void setState(byte state) throws IOException {
    	header.setState(state);
    }

    /**
     * Get the current file position.
     * @return the file position
     */
    public long getPosition() {
    	// return header.getPosition();
    	return position;
    }


    /**
     * Close the appender and the underlying file.
     * @throws IOException if an I/O error occurs.
     */
    protected void close() throws IOException {
        header.setState(TransactionLogHeader.CLEAN_LOG_STATE);
        fc.force(false);
        if (lock != null)
        	lock.release();
        fc.close();
        randomeAccessFile.close();
    }

    /**
     * Creates a cursor on this journal file allowing iteration of its records.
     * This opens a new read-only file descriptor independent of the write-only one
     * still used for writing transaction logs.
     * @return a TransactionLogCursor.
     * @throws IOException if an I/O error occurs.
     */
    protected TransactionLogCursor getCursor() throws IOException {
        return new TransactionLogCursor(file);
    }

    /**
     * Force flushing the logs to disk
     * @throws IOException if an I/O error occurs.
     */
    protected void force() throws IOException {
        if (log.isDebugEnabled()) log.debug("forcing log writing");
        fc.force(false);
        if (log.isDebugEnabled()) log.debug("done forcing log");
    }

    public String toString() {
        return "a TransactionLogAppender on " + file.getName();
    }
}
