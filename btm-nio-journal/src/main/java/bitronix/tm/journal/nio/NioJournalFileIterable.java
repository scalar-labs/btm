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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import static bitronix.tm.journal.nio.NioJournalFileRecord.RECORD_CRC32_OFFSET;
import static bitronix.tm.journal.nio.NioJournalFileRecord.RECORD_HEADER_SIZE;
import static bitronix.tm.journal.nio.NioJournalFileRecord.RECORD_TRAILER_SIZE;

/**
 * Low level file iterator.
 * <p/>
 * Iterates the given journal, returning records based on the given delimiter.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalFileIterable implements Iterable<NioJournalFileRecord> {

    private static final Logger log = LoggerFactory.getLogger(NioJournalFileIterable.class);

    private UUID delimiter;
    private long journalSize;
    private boolean readInvalid;
    private FileChannel fileChannel;

    NioJournalFileIterable(UUID delimiter, FileChannel fileChannel, boolean readInvalid) throws IOException {
        this.delimiter = delimiter;
        this.fileChannel = fileChannel;
        this.readInvalid = readInvalid;
        this.journalSize = fileChannel.size();
    }

    public long findPositionAfterLastRecord() {
        RecordIterator recordIterator = new RecordIterator();
        while (recordIterator.hasNext())
            recordIterator.next();
        return recordIterator.getPositionAfterLastRecord();
    }

    public Iterator<NioJournalFileRecord> iterator() {
        return new RecordIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "JournalIterable{" +
                "delimiter=" + delimiter +
                ", journalSize=" + journalSize +
                ", fileChannel=" + fileChannel +
                '}';
    }

    private class RecordIterator implements Iterator<NioJournalFileRecord> {

        private int reverseCrc32Offset = RECORD_CRC32_OFFSET - RECORD_HEADER_SIZE;

        private long position, bufferPosition, positionAfterLastRecord, readEntries, brokenEntries;
        private ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);

        NioJournalFileRecord nextEntry;

        /**
         * Returns the exact byte position of the last returned record.
         *
         * @return the exact byte position of the last returned record.
         */
        public long getPositionAfterLastRecord() {
            return positionAfterLastRecord;
        }

        public boolean hasNext() {
            while (nextEntry == null && readNextEntry()) {
                nextEntry = new NioJournalFileRecord(delimiter, buffer);
                readEntries++;
                if (buffer.getInt(buffer.position() + reverseCrc32Offset) != nextEntry.calculateCrc32()) {
                    if (readInvalid) {
                        log.warn("CRC32 differs in payload of record for {}, marking the entry as invalid.", nextEntry);
                        nextEntry.markInvalid();
                    } else {
                        log.warn("CRC32 differs in payload of record {}, skipping the entry.", nextEntry);
                        nextEntry = null;
                    }
                    brokenEntries++;
                } else {
                    positionAfterLastRecord = bufferPosition + buffer.limit() + RECORD_TRAILER_SIZE;
                }
            }
            return nextEntry != null;
        }

        public NioJournalFileRecord next() {
            if (!hasNext())
                throw new NoSuchElementException("Has no more entries.");
            try {
                return nextEntry;
            } finally {
                nextEntry = null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private boolean readNextEntry() {
            // Set the buffer to the end of the previous entry.
            consumePreviousRecord();

            while (true) {
                buffer.limit(buffer.capacity());
                int recordLength = NioJournalFileRecord.findNextRecord(buffer, delimiter);
                NioJournalFileRecord.ReadStatus readStatus = NioJournalFileRecord.ReadStatus.decode(recordLength);

                switch (readStatus) {
                    case FoundPartialRecord:
                        if (buffer.position() == 0) {

                            int newSize = buffer.capacity() + 4 * 1024;
                            if (log.isDebugEnabled()) {
                                log.debug("Detected a buffer underflow. Creating a new buffer of size {}",
                                        newSize);
                            }

                            buffer = ByteBuffer.allocate(newSize).put(buffer);

                        } else if (buffer.hasRemaining()) {
                            if (log.isTraceEnabled()) {
                                log.trace("Found partial record at the end of the buffer, moving " +
                                        "partial content ({} bytes) to the beginning of the buffer.",
                                        buffer.remaining());
                            }
                            buffer.compact();
                        }
                        break;
                    case NoHeaderInBuffer:
                        buffer.clear();
                        break;
                }

                if (readStatus != NioJournalFileRecord.ReadStatus.ReadOk) {
                    try {
                        // Capture the position of the buffer within the file.
                        bufferPosition = position - buffer.position();
                        int readBytes = fileChannel.read(buffer, position);
                        if (readBytes == -1)
                            break;
                        position += readBytes;
                        buffer.flip();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // continue
                } else {
                    buffer.limit(buffer.position() + recordLength);
                    return true;
                }
            }
            return false;
        }

        private void consumePreviousRecord() {
            if (buffer.hasRemaining()) {
                final int limit = buffer.limit(), cap = buffer.capacity();
                buffer.limit(cap).position(Math.min(cap, limit + RECORD_TRAILER_SIZE));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "JournalIterabl$Iterator{" +
                    "position=" + position +
                    ", readEntries=" + readEntries +
                    ", brokenEntries=" + brokenEntries +
                    ", nextEntry=" + nextEntry +
                    ", buffer=" + NioJournalFileRecord.bufferToString(buffer) +
                    '}';
        }
    }
}
