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

import static bitronix.tm.journal.nio.NioJournalFileRecord.FindResult;

/**
 * Low level file iterator.
 * <p/>
 * Iterates the given journal, returning records based on the given delimiter.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalFileIterable implements Iterable<NioJournalFileRecord> {

    static final int INITIAL_READ_BUFFER_SIZE = 64 * 1024;

    private static final Logger log = LoggerFactory.getLogger(NioJournalFileIterable.class);
    private static final boolean trace = log.isTraceEnabled();

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

    /**
     * Iterates all elements and returns the absolute position after the last record.
     *
     * @return the absolute position inside the file after the last record.
     */
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

        private long position, bufferOffsetInFile, positionAfterLastRecord, readEntries, brokenEntries;
        private ByteBuffer buffer = ByteBuffer.allocate(INITIAL_READ_BUFFER_SIZE);

        private NioJournalFileRecord nextEntry, disposableEntry;

        /**
         * Returns the exact byte position of the last returned record.
         *
         * @return the exact byte position of the last returned record.
         */
        public long getPositionAfterLastRecord() {
            return positionAfterLastRecord;
        }

        public boolean hasNext() {
            if (nextEntry == null && disposableEntry != null) {
                if (trace) { log.trace("Disposing previously returned journal file record " + disposableEntry + " to protect access in invalid state."); }
                disposableEntry.dispose(false);
                disposableEntry = null;
            }

            while (nextEntry == null && (nextEntry = readNextEntry()) != null) {
                readEntries++;
                if (!nextEntry.isValid()) {
                    if (readInvalid) {
                        log.warn("CRC32 differs in payload of record for " + nextEntry + ", returning this invalid entry.");
                    } else {
                        log.warn("CRC32 differs in payload of record " + nextEntry + ", skipping the entry.");
                        nextEntry = null;
                    }
                    brokenEntries++;
                } else {
                    positionAfterLastRecord = bufferOffsetInFile + buffer.position();
                }
            }

            return nextEntry != null;
        }

        public NioJournalFileRecord next() {
            if (!hasNext())
                throw new NoSuchElementException("There are no more entries inside the journal.");
            try {
                return nextEntry;
            } finally {
                disposableEntry = nextEntry;
                nextEntry = null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private NioJournalFileRecord readNextEntry() {
            while (true) {
                final FindResult findResult = NioJournalFileRecord.findNextRecord(delimiter, buffer);

                switch (findResult.getStatus()) {
                    case FoundPartialRecord:
                        if (buffer.position() == 0) {

                            int newSize = buffer.capacity() + 4 * 1024;
                            if (log.isDebugEnabled()) {
                                log.debug("Detected a buffer underflow on the attempt to read the journal file. " +
                                        "Creating a new buffer of size " + newSize + " to fit in the current journal record.");
                            }

                            buffer = ByteBuffer.allocate(newSize).put(buffer);

                        } else if (buffer.hasRemaining()) {
                            if (trace) {
                                log.trace("Found partial record at the end of the buffer, moving partial content " +
                                        "(" + buffer.remaining() + " bytes) to the beginning of the buffer.");
                            }
                            buffer.compact();
                        }
                        break;

                    case NoHeaderInBuffer:
                        buffer.clear();
                        break;
                }

                if (findResult.getStatus() != NioJournalFileRecord.ReadStatus.ReadOk) {
                    try {
                        // Capture the position of the buffer within the file.
                        bufferOffsetInFile = position - buffer.position();
                        int readBytes = fileChannel.read(buffer, position);
                        if (readBytes == -1)
                            break;
                        position += readBytes;
                        buffer.flip();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return findResult.getRecord();
                }
            }
            return null;
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
