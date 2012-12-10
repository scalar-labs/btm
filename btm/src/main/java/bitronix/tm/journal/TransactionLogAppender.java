/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.journal;

import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used to write {@link TransactionLogRecord} objects to a log file.
 *
 * @author Ludovic Orban
 * @author Brett Wooldridge
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
    private final RandomAccessFile randomeAccessFile;
    private final FileChannel fc;
    private final FileLock lock;
    private final TransactionLogHeader header;
	private final long maxFileLength;
	private final AtomicInteger outstandingWrites;
	private final HashMap<Uid, Set<String>> danglingRecords;
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

        this.outstandingWrites = new AtomicInteger();

        this.danglingRecords = new HashMap<Uid, Set<String>>();

        this.position = header.getPosition();
    }

    /**
     * Get the current file position and advance the position by recordSize if
     * the maximum file length won't be exceeded.  Callers to this method are
     * synchronized within the DiskJournal and it is guaranteed there will 
     * never be two callers to this method.  This creates a Java memory barrier
     * that guarantees that 'position' will never be viewed inconsistently
     * between threads.
     *
     * @param tlog the TransactionLogRecord
     * @return true if the log should rollover, false otherwise
     * @throws IOException if an I/O error occurs
     */
    protected boolean setPositionAndAdvance(TransactionLogRecord tlog) throws IOException {
        int tlogSize = tlog.calculateTotalRecordSize();
    	if (position + tlogSize > maxFileLength) {
    		return true;
    	}

    	long writePosition = position;
    	position += tlogSize;
    	tlog.setWritePosition(writePosition);

    	outstandingWrites.incrementAndGet();
    	return false;
    }

    /**
     * Write a {@link TransactionLogRecord} to disk.
     * @param tlog the record to write to disk.
     * @throws IOException if an I/O error occurs.
     */
    protected void writeLog(TransactionLogRecord tlog) throws IOException {
        try {
            int status = tlog.getStatus();
            Uid gtrid = tlog.getGtrid();

        	int recordSize = tlog.calculateTotalRecordSize();
            ByteBuffer buf = ByteBuffer.allocate(recordSize);
            buf.putInt(tlog.getStatus());
            buf.putInt(tlog.getRecordLength());
            buf.putInt(tlog.getHeaderLength());
            buf.putLong(tlog.getTime());
            buf.putInt(tlog.getSequenceNumber());
            buf.putInt(tlog.getCrc32());
            buf.put((byte) gtrid.getArray().length);
            buf.put(gtrid.getArray());
            Set<String> uniqueNames = tlog.getUniqueNames();
            buf.putInt(uniqueNames.size());
            for (String uniqueName : uniqueNames) {
                buf.putShort((short) uniqueName.length());
                buf.put(uniqueName.getBytes());
            }
            buf.putInt(tlog.getEndRecord());
            buf.flip();

            if (log.isDebugEnabled()) { log.debug("between " + tlog.getWritePosition() + " and " + tlog.getWritePosition() + tlog.calculateTotalRecordSize() + ", writing " + tlog); }
    
            final long writePosition = tlog.getWritePosition();
            while (buf.hasRemaining()) {
            	fc.write(buf, writePosition + buf.position());
            }

            trackOutstanding(status, gtrid, uniqueNames);
        }
        finally {
        	if (outstandingWrites.decrementAndGet() == 0) {
        		header.setPosition(position);
        	}
        }
    }

    protected List<TransactionLogRecord> getDanglingLogs() {
    	synchronized (danglingRecords) {
	        List<Uid> sortedUids = new ArrayList<Uid>(danglingRecords.keySet());
	        Collections.sort(sortedUids, new Comparator<Uid>() {
	            public int compare(Uid uid1, Uid uid2) {
	                return Integer.valueOf(uid1.extractSequence()).compareTo(uid2.extractSequence()); 
	            }
	        });
	
	        List<TransactionLogRecord> outstandingLogs = new ArrayList<TransactionLogRecord>(danglingRecords.size());
	        for (Uid uid : sortedUids) {
	            Set<String> uniqueNames = danglingRecords.get(uid);
	            outstandingLogs.add(new TransactionLogRecord(Status.STATUS_COMMITTING, uid, uniqueNames));
	        }
	
	        return outstandingLogs;
    	}
    }

    protected void clearDanglingLogs() {
    	synchronized (danglingRecords) {
    		danglingRecords.clear();
    	}
    }

    /**
     * This method tracks outstanding (uncommitted) resources by gtrid.  Access
     * to the danglingRecords map, and the TreeSets contained within are guarded
     * by a synchronization lock on danglingRecords itself.
     *
     * @param status the transaction log record status
     * @param gtrid the transaction id
     * @param uniqueNames the set of uniquely named resources
     */
    private void trackOutstanding(int status, Uid gtrid, Set<String> uniqueNames) {
    	if (uniqueNames.isEmpty()) {
    		return;
    	}

        switch (status) {
            case Status.STATUS_COMMITTING: {
            	synchronized (danglingRecords) {
            		Set<String> outstanding = danglingRecords.get(gtrid);
            		if (outstanding == null) {
            			outstanding = new TreeSet<String>(uniqueNames);
            			danglingRecords.put(gtrid, outstanding);
            		}
            		outstanding.addAll(uniqueNames);
            	}
                break;
            }
            case Status.STATUS_ROLLEDBACK:
            case Status.STATUS_COMMITTED:
            case Status.STATUS_UNKNOWN: {
            	synchronized (danglingRecords) {
	                Set<String> outstanding = danglingRecords.get(gtrid);
	                if (outstanding != null && outstanding.removeAll(uniqueNames) && outstanding.isEmpty()) {
	                    danglingRecords.remove(gtrid);
	                }
            	}
                break;
            }
        }
    }

    /**
     * Return a {@link TransactionLogHeader} that allows reading and controlling the log file's header.
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
        if (log.isDebugEnabled()) { log.debug("forcing log writing"); }
        fc.force(false);
        if (log.isDebugEnabled()) { log.debug("done forcing log"); }
    }

    public String toString() {
        return "a TransactionLogAppender on " + file.getName();
    }
}
