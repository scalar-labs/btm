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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static bitronix.tm.journal.nio.NioJournalFileRecord.readRecords;

/**
 * Low level file handling implementation.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalFile implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioJournalFile.class);

    private static final byte[] JOURNAL_HEADER_PREFIX = "\r\nNio TX Journal [Version 1.0] - ".getBytes(NAME_CHARSET);
    private static final byte[] JOURNAL_HEADER_SUFFIX = "\r\n".getBytes(NAME_CHARSET);

    static final int FIXED_HEADER_SIZE = 512;

    private volatile UUID previousDelimiter = UUID.randomUUID();
    private volatile UUID delimiter = UUID.randomUUID();

    ByteBuffer writeBuffer;

    private final File file;
    private final RandomAccessFile raf;

    private FileLock lock;
    private FileChannel fileChannel;
    private long startPosition;

    private final AtomicLong journalSize = new AtomicLong();
    private final AtomicLong lastModified = new AtomicLong(), lastForced = new AtomicLong();

    public NioJournalFile(File file, long initialJournalSize) throws IOException {
        this.file = file;
        raf = new RandomAccessFile(file, "rw");

        fileChannel = raf.getChannel();
        lock = fileChannel.lock();
        final boolean createHeader = raf.length() == 0;

        try {
            readJournalHeader();
        } catch (IOException e) {
            log.error("Failed reading journal header, refusing to open the file " + file + ".", e);
            close();
            throw e;
        }

        // We can increase but not shrink the journal.
        this.journalSize.set(Math.max(initialJournalSize, raf.length()));
        growJournal(this.journalSize.get());

        if (createHeader)
            writeJournalHeader();
        else {
            NioJournalFileIterable it = (NioJournalFileIterable) readRecords(delimiter, fileChannel, false);
            long position = it.findPositionAfterLastRecord();
            fileChannel.position(Math.max(FIXED_HEADER_SIZE, position));
        }
    }

    public File getFile() {
        return file;
    }

    public synchronized long getSize() {
        return journalSize.get();
    }

    public long getPosition() throws IOException {
        return fileChannel.position();
    }

    /**
     * Closes the journal.
     *
     * @throws IOException in case of the operation failed.
     */
    public synchronized void close() throws IOException {
        try {
            if (fileChannel != null) {
                force();
                try {
                    lock.release();
                } finally {
                    fileChannel.close();
                }
            }
        } finally {
            fileChannel = null;
            lock = null;
        }
    }

    public synchronized void growJournal(long newSize) throws IOException {
        if (newSize >= journalSize.get()) {
            raf.setLength(newSize);
            journalSize.set(newSize);
        }
    }

    public synchronized Iterable<NioJournalFileRecord> readAll(boolean includeInvalid) throws IOException {
        final Iterable<NioJournalFileRecord> first = readRecords(previousDelimiter, fileChannel, includeInvalid);
        final Iterable<NioJournalFileRecord> second = readRecords(delimiter, fileChannel, includeInvalid);

        return new Iterable<NioJournalFileRecord>() {
            @Override
            public Iterator<NioJournalFileRecord> iterator() {
                return new NioCompositeIterator(Arrays.asList(first, second));
            }
        };
    }

    private void asserHeaderPartEquals(ByteBuffer buffer, byte[] value) throws IOException {
        byte[] prefix = new byte[value.length];
        buffer.get(prefix);
        if (!Arrays.equals(prefix, value)) {

            // If we'd had multiple version, legacy handling would go in here.

            throw new IOException("Failed opening journal file " + raf + ", expected a file header of <" +
                    NioJournalFileRecord.bufferToString(ByteBuffer.wrap(value)) + "> but was <" +
                    NioJournalFileRecord.bufferToString(ByteBuffer.wrap(prefix)) + ">");
        }
    }

    private void readJournalHeader() throws IOException {
        if (fileChannel.size() == 0)
            return; // new file.

        ByteBuffer buffer = getWriteBuffer(PRE_ALLOCATED_BUFFER_SIZE);
        fileChannel.read(buffer, 0);
        buffer.flip();

        try {
            asserHeaderPartEquals(buffer, JOURNAL_HEADER_PREFIX);
            previousDelimiter = NioJournalFileRecord.readUUID(buffer);
            delimiter = NioJournalFileRecord.readUUID(buffer);
            asserHeaderPartEquals(buffer, JOURNAL_HEADER_SUFFIX);
            startPosition = buffer.position();
        } catch (IOException e) {
            previousDelimiter = UUID.randomUUID();
            delimiter = UUID.randomUUID();
            throw e;
        }
    }

    private void writeJournalHeader() throws IOException {
        if (fileChannel.position() != 0)
            throw new IllegalStateException("File channel is not positioned at the header location.");

        ByteBuffer buffer = getWriteBuffer(PRE_ALLOCATED_BUFFER_SIZE);
        buffer.put(JOURNAL_HEADER_PREFIX);
        NioJournalFileRecord.writeUUID(previousDelimiter, buffer);
        NioJournalFileRecord.writeUUID(delimiter, buffer);
        buffer.put(JOURNAL_HEADER_SUFFIX);
        fileChannel.write((ByteBuffer) buffer.flip());

        // Set position to data area
        fileChannel.position(FIXED_HEADER_SIZE);
    }

    /**
     * Rollover to the beginning of the journal file.
     *
     * @throws IOException in case of the operation failed.
     */
    public synchronized void rollover() throws IOException {
        eraseRemainingBytesInJournal();

        fileChannel.position(0);
        previousDelimiter = delimiter;
        delimiter = UUID.randomUUID();
        writeJournalHeader();
    }

    private void eraseRemainingBytesInJournal() throws IOException {
        final int blockSize = 4 * 1024;
        final ByteBuffer buffer = getWriteBuffer(blockSize);
        while (buffer.hasRemaining())
            buffer.put((byte) ' ');

        do {
            buffer.flip().limit((int) Math.min(remainingCapacity(), blockSize));
        } while (fileChannel.write(buffer) != 0);
    }

    /**
     * Creates an empty record that may be used to write it to the journal.
     *
     * @return an empty record that may be used to write if to the journal.
     */
    public final NioJournalFileRecord createEmptyRecord() {
        return new NioJournalFileRecord(delimiter);
    }

    /**
     * Writes the given records to this journal.
     *
     * @param records the records to write.
     * @return the number of written bytes.
     * @throws IOException in case of the operation failed.
     */
    public synchronized long write(List<NioJournalFileRecord> records) throws IOException {
        try {
            final int requiredBytes = NioJournalFileRecord.calculateRequiredBytes(records);
            final long remainingBytes = remainingCapacity();
            if (requiredBytes > remainingBytes) {
                throw new IOException("Journal faces a rollover (remaining capacity: " + remainingBytes +
                        ", required: " + requiredBytes + "). Manually trigger this before writing new content.");
            }

            // the implementation of gathering and scattering byte channels is not very fast.
            // using an intermediate buffer improves speed by factor 4 to 5 (direct buffer is ~25% improvement on top).

            final UUID targetDelimiter = delimiter;
            final ByteBuffer writeBuffer = getWriteBuffer(requiredBytes);
            for (NioJournalFileRecord record : records)
                record.writeRecord(targetDelimiter, writeBuffer);

            writeBuffer.flip();

            return fileChannel.write(writeBuffer);
        } finally {
            lastModified.set(System.currentTimeMillis());
        }
    }

    private ByteBuffer getWriteBuffer(int requiredBytes) {
        ByteBuffer buffer = writeBuffer;
        if (buffer == null || buffer.capacity() < requiredBytes)
            writeBuffer = buffer = ByteBuffer.allocateDirect(requiredBytes);

        buffer.clear().limit(requiredBytes);
        return buffer;
    }

    /**
     * Returns the remaining capacity in this journal until the rollover happens.
     *
     * @return the remaining capacity in this journal until the rollover happens.
     * @throws IOException in case of the operation failed.
     */
    public long remainingCapacity() throws IOException {
        return Math.max(0, journalSize.get() - fileChannel.position());
    }

    /**
     * Forces the journal to disk (fsync).
     *
     * @throws IOException in case of the operation failed.
     */
    public void force() throws IOException {
        if (lastForced.get() != lastModified.get()) {
            if (log.isTraceEnabled())
                log.trace("Forcing the file channel {}", fileChannel);

            fileChannel.force(false);
            lastForced.set(lastModified.get());
        } else {
            if (log.isTraceEnabled())
                log.trace("Force not required on file channel {}", fileChannel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournalFile{" +
                "previousDelimiter=" + previousDelimiter +
                ", delimiter=" + delimiter +
                ", lastModified=" + lastModified +
                ", lastForced=" + lastForced +
                ", file=" + file +
                ", journalSize=" + journalSize +
                ", startPosition=" + startPosition +
                '}';
    }
}
