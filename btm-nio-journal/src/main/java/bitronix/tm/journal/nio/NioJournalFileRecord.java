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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Low level file record.
 * <p/>
 * Implements methods for finding, reading and writing a single record.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalFileRecord {

    private static final Logger log = LoggerFactory.getLogger(NioJournalFileRecord.class);

    private static final byte[] RECORD_DELIMITER_PREFIX = "\r\nLR[".getBytes(NioJournalRecord.NAME_CHARSET);
    private static final byte[] RECORD_DELIMITER_SUFFIX = "][".getBytes(NioJournalRecord.NAME_CHARSET);
    private static final byte[] RECORD_DELIMITER_TRAILER = "]-".getBytes(NioJournalRecord.NAME_CHARSET);

    public static final int RECORD_LENGTH_OFFSET = RECORD_DELIMITER_PREFIX.length + 16;
    public static final int RECORD_CRC32_OFFSET = RECORD_LENGTH_OFFSET + 4;

    public static final int RECORD_HEADER_SIZE =
            RECORD_DELIMITER_PREFIX.length +
                    16 + 4 + 4 +
                    RECORD_DELIMITER_SUFFIX.length;

    public static final int RECORD_TRAILER_SIZE = RECORD_DELIMITER_TRAILER.length + 16;

    private static final boolean trace = log.isTraceEnabled();

    private UUID delimiter;
    private ByteBuffer payload, recordBuffer;
    private boolean valid = true;

    /**
     * Utility methods that converts the buffer to a string.
     *
     * @param buffer the buffer to convert. (note: use "duplicate" if the buffer should not get consumed)
     * @return the string representation of the buffer using 'ISO-8859-1' charset.
     */
    public static String bufferToString(ByteBuffer buffer) {
        if (buffer == null)
            return "<no-buffer (null)>";

        buffer = buffer.duplicate();
        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        return new String(content, Charset.forName("ISO-8859-1"));
    }

    /**
     * Reads all records contained in the given file channel.
     *
     * @param delimiter      the delimiter used to identify records.
     * @param channel        the channel to read from.
     * @param includeInvalid include those records that do not pass CRC checks.
     * @return a new iterable that returns a repeatable iteration over records.
     * @throws IOException in case of the IO operation fails initially.
     */
    public static Iterable<NioJournalFileRecord> readRecords(UUID delimiter, FileChannel channel,
                                                             boolean includeInvalid) throws IOException {
        return new NioJournalFileIterable(delimiter, channel, includeInvalid);
    }

    /**
     * Returns the number of bytes required to write the given records.
     *
     * @param records the records to use for the calculation.
     * @return the number of bytes required to write the given records.
     */
    public static int calculateRequiredBytes(Collection<NioJournalFileRecord> records) {
        int requiredBytes = 0;
        for (NioJournalFileRecord source : records)
            requiredBytes += source.getRecordSize();
        return requiredBytes;
    }

    /**
     * Disposes all records.
     *
     * @param records the records to dispose.
     */
    public static void disposeAll(Collection<NioJournalFileRecord> records) {
        int idx = 0;
        final ByteBuffer[] buffers = new ByteBuffer[records.size()];
        for (NioJournalFileRecord record : records) {
            buffers[idx++] = record.recordBuffer;
            record.dispose(false);
        }
        NioBufferPool.getInstance().recycleBuffers(Arrays.asList(buffers));
    }

    /**
     * Creates an empty record for the given delimiter.
     *
     * @param delimiter the delimiter to create the record for.
     */
    public NioJournalFileRecord(UUID delimiter) {
        this.delimiter = delimiter;
    }

    NioJournalFileRecord(UUID delimiter, ByteBuffer payload) {
        this(delimiter);
        this.payload = payload;
    }

    /**
     * Dispose all held resources and recycle any contained buffers.
     */
    public void dispose() {
        dispose(true);
    }

    /**
     * Dispose all held resources and recycle any contained buffers.
     *
     * @param recycle specified whether the kept buffer is recycled or not.
     */
    void dispose(final boolean recycle) {
        if (recycle)
            NioBufferPool.getInstance().recycleBuffer(recordBuffer);
        recordBuffer = null;
        payload = null;
    }

    /**
     * Creates an empty payload buffer of the given size and transaction status.
     *
     * @param payloadSize the size of the payload to create.
     * @return the created buffer.
     */
    public ByteBuffer createEmptyPayload(int payloadSize) {
        recordBuffer = NioBufferPool.getInstance().poll(payloadSize + RECORD_HEADER_SIZE + RECORD_TRAILER_SIZE);

        writeRecordHeaderFor(payloadSize, delimiter, recordBuffer);
        payload = (ByteBuffer) recordBuffer.slice().limit(payloadSize);
        writeRecordTrailerFor(delimiter, (ByteBuffer) recordBuffer.position(recordBuffer.position() + payloadSize));

        recordBuffer.flip();

        return getPayload();
    }

    /**
     * Writes this record to the given target buffer.
     *
     * @param targetDelimiter the target delimiter used to delimit records.
     * @param target          the target to write to.
     */
    public void writeRecord(UUID targetDelimiter, ByteBuffer target) {
        if (!targetDelimiter.equals(delimiter)) {
            if (trace) {
                log.trace("Correcting delimiter from {} to {}, the target changed in the meantime.",
                        delimiter, targetDelimiter);
            }
            delimiter = targetDelimiter;
            recordBuffer = null;
        }

        if (recordBuffer == null || payload == null) {
            if (payload != null) {
                final ByteBuffer pl = payload; // must be assigned to a local var.
                createEmptyPayload(pl.remaining()).put(pl);
            } else
                throw new IllegalStateException("The payload was not yet written. Cannot write this record.");
        }

        // Calculate CRC32
        recordBuffer.putInt(RECORD_CRC32_OFFSET, calculateCrc32());

        recordBuffer.mark();
        try {
            target.put(recordBuffer);
        } finally {
            recordBuffer.reset();
        }
    }

    int calculateCrc32() {
        final CRC32 crc = new CRC32();
        crc.update(payload.array(), payload.arrayOffset() + payload.position(), payload.remaining());
        return (int) crc.getValue();
    }

    public int getRecordSize() {
        return recordBuffer != null ? recordBuffer.limit() :
                payload.limit() + RECORD_HEADER_SIZE + RECORD_TRAILER_SIZE;
    }

    public ByteBuffer getPayload() {
        return payload.duplicate();
    }

    public UUID getDelimiter() {
        return delimiter;
    }

    public boolean isValid() {
        return valid;
    }

    void markInvalid() {
        valid = false;
    }

    @Override
    public String toString() {
        return "NioJournalFileRecord{" +
                ", delimiter=" + delimiter +
                ", payload=" + bufferToString(payload) +
                '}';
    }

    static void writeUUID(UUID source, ByteBuffer target) {
        target.putLong(source.getMostSignificantBits());
        target.putLong(source.getLeastSignificantBits());
    }

    static UUID readUUID(ByteBuffer buffer) {
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static void writeRecordHeaderFor(int payloadSize, UUID delimiter, ByteBuffer target) {
        target.put(RECORD_DELIMITER_PREFIX);
        writeUUID(delimiter, target);
        target.putInt(payloadSize); // record length
        target.putInt(0); // reserved for CRC32 (comes later)
        target.put(RECORD_DELIMITER_SUFFIX);
    }

    private static void writeRecordTrailerFor(UUID delimiter, ByteBuffer target) {
        target.put(RECORD_DELIMITER_TRAILER);
        writeUUID(delimiter, target);
    }

    private static int readRecordHeader(ByteBuffer source, UUID delimiter) {
        source.mark();
        boolean willBePartial = source.remaining() < RECORD_HEADER_SIZE;
        try {
            int similarBytes = bufferContainsSequence(source, RECORD_DELIMITER_PREFIX);

            if (willBePartial || (similarBytes < 0 && !source.hasRemaining())) {
                if (trace) log.trace("Read a partial header, reporting -5.");
                return ReadStatus.foundPartialRecord.encode();
            }

            if (similarBytes < 1)
                return ReadStatus.noHeaderAtCurrentPosition.encode();

            final UUID uuid = readUUID(source);
            if (!delimiter.equals(uuid)) {
                if (trace) {
                    log.trace("Found an record header of delimiter {}, while expecting {}, skipping it.",
                            uuid, delimiter);
                }
                return ReadStatus.foundHeaderWithDifferentDelimiter.encode();
            }

            int recordLength = source.getInt();
            int crc32 = source.getInt(); // currently un-used.

            if (bufferContainsSequence(source, RECORD_DELIMITER_SUFFIX) <= 0)
                return ReadStatus.noHeaderAtCurrentPosition.encode();

            if (recordLength + RECORD_TRAILER_SIZE > source.remaining()) {
                if (trace) {
                    log.trace("Found partial record, the length {} exceeds the remaining bytes {}.",
                            recordLength, source.remaining());
                }
                return ReadStatus.foundPartialRecord.encode();
            }

            // Marking the beginning of the payload.
            source.mark();

            source.position(source.position() + recordLength);
            if (bufferContainsSequence(source, RECORD_DELIMITER_TRAILER) <= 0 ||
                    !delimiter.equals(readUUID(source))) {
                if (log.isDebugEnabled())
                    log.debug("Found an invalid record trailer for delimiter {}. Will skip this entry.", delimiter);
                return ReadStatus.noHeaderAtCurrentPosition.encode();
            }

            return recordLength;
        } finally {
            source.reset();
        }
    }

    static int findNextRecord(ByteBuffer source, UUID delimiter) {
        final byte hook = RECORD_DELIMITER_PREFIX[0];

        if (source.hasRemaining()) {
            do {
                source.mark();
                if (source.get() == hook) {
                    source.reset();

                    final int recordLength = readRecordHeader((ByteBuffer) source.reset(), delimiter);
                    final ReadStatus readStatus = ReadStatus.decode(recordLength);

                    switch (readStatus) {
                        case readOk:
                            return recordLength;
                        case foundPartialRecord:
                            return ReadStatus.foundPartialRecord.encode();
                        case foundHeaderWithDifferentDelimiter:
                            if (source.remaining() > RECORD_HEADER_SIZE) {
                                if (trace) {
                                    log.trace("Found other header entry, try to use length as hint to " +
                                            "iterate it quickly.");
                                }

                                int entryLength = source.getInt(source.position() + RECORD_LENGTH_OFFSET);
                                source.position(source.position() + RECORD_HEADER_SIZE);

                                if (entryLength + RECORD_TRAILER_SIZE <= source.remaining() &&
                                        source.get(source.position() + entryLength) == RECORD_DELIMITER_TRAILER[0]) {
                                    if (trace) log.trace("Quickly iterating other log entry.");
                                    source.position(source.position() + entryLength + RECORD_TRAILER_SIZE);
                                }

                                break;
                            }
                        default:
                            source.get(); // consuming the byte (that was reset before).
                    }
                }
            } while (source.hasRemaining());
        }

        return ReadStatus.noHeaderInBuffer.encode();
    }

    private static int bufferContainsSequence(final ByteBuffer source, final byte[] sequence) {
        final int maxCount = source.remaining();
        int count = 0;

        for (byte b : sequence) {
            if (maxCount == count || source.get() != b)
                return -count;
            count++;
        }

        return count;
    }

    static enum ReadStatus {
        /**
         * Header was successfully read, the record is fully contained in the buffer and it is valid.
         */
        readOk,
        /**
         * There's no header at the current buffer position.
         */
        noHeaderAtCurrentPosition,
        /**
         * There's no header in the whole buffer.
         */
        noHeaderInBuffer,
        /**
         * There's a header but it doesn't belong to the current delimiter.
         */
        foundHeaderWithDifferentDelimiter,
        /**
         * There's a valid header but the record is not complete.
         */
        foundPartialRecord,;

        static ReadStatus decode(int recordLength) {
            if (recordLength >= 0)
                return readOk;
            return values()[-recordLength];
        }

        static int encode(ReadStatus status) {
            return -status.ordinal();
        }

        int encode() {
            return encode(this);
        }
    }

}
