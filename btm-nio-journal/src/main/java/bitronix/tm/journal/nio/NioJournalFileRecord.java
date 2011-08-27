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
class NioJournalFileRecord implements NioJournalConstants {

    private static final Logger log = LoggerFactory.getLogger(NioJournalFileRecord.class);

    private static final byte[] RECORD_DELIMITER_PREFIX = "\r\nLR[".getBytes(NioJournalRecord.NAME_CHARSET);
    private static final byte[] RECORD_DELIMITER_SUFFIX = "][".getBytes(NioJournalRecord.NAME_CHARSET);
    private static final byte[] RECORD_DELIMITER_TRAILER = "]-".getBytes(NioJournalRecord.NAME_CHARSET);

    /**
     * Defines the offset of the 4 byte int value from the beginning of the record that stores the total length of the record itself.
     */
    public static final int RECORD_LENGTH_OFFSET = RECORD_DELIMITER_PREFIX.length + 16;
    /**
     * Defines the offset of the 4 byte int value from the beginning of the record that stores the CRC32 checksum of the record's payload.
     */
    public static final int RECORD_CRC32_OFFSET = RECORD_LENGTH_OFFSET + 4;
    /**
     * Defines the number of bytes consumed by the raw-header of the file-record (this does not include any additional header inside the payload).
     */
    public static final int RECORD_HEADER_SIZE =
            RECORD_DELIMITER_PREFIX.length +
                    16 + /* opening delimiter (uuid) */
                    4 + /* length */
                    4 + /* crc32 */
                    RECORD_DELIMITER_SUFFIX.length;
    /**
     * Defines the number of bytes consumed by the raw-trailer of the file-record.
     */
    public static final int RECORD_TRAILER_SIZE =
            16 + /* closing delimiter (uuid) */
                    RECORD_DELIMITER_TRAILER.length;

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
        return new String(content, NAME_CHARSET);
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
    public static Iterable<NioJournalFileRecord> readRecords(UUID delimiter, FileChannel channel, boolean includeInvalid) throws IOException {
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
        this.payload = payload == null ? null : payload.duplicate();
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
            if (log.isDebugEnabled())
                log.debug("Correcting delimiter from " + delimiter + " to " + targetDelimiter + ", the target changed in the meantime.");
            delimiter = targetDelimiter;
            recordBuffer = null;
        }

        if (recordBuffer == null || payload == null) {
            if (payload != null) {
                // Must be assigned to a local var as "createEmptyPayload(..)" re-initialized the field as a sub-region of the record buffer.
                final ByteBuffer pl = payload.duplicate();
                // Creating the record buffer and write the payload into the reserved region.
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

    /**
     * Returns the total size of the serialized record in bytes (including header, trailer and payload).
     *
     * @return the total size of the serialized record in bytes (including header, trailer and payload).
     */
    public int getRecordSize() {
        return recordBuffer != null ? recordBuffer.limit() : payload.limit() + RECORD_HEADER_SIZE + RECORD_TRAILER_SIZE;
    }

    /**
     * Returns a writable, fixed size buffer containing the payload.
     *
     * @return a writable, fixed size buffer containing the payload.
     */
    public ByteBuffer getPayload() {
        return payload.duplicate();
    }

    /**
     * Returns the delimiter used to separate log records belonging to the same list.
     *
     * @return the delimiter used to separate log records belonging to the same list.
     */
    public UUID getDelimiter() {
        return delimiter;
    }

    /**
     * Returns true if this record can be considered valid.
     *
     * @return true if this record can be considered valid.
     */
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
        final boolean willBePartial = source.remaining() < RECORD_HEADER_SIZE;
        try {
            int similarBytes = bufferContainsSequence(source, RECORD_DELIMITER_PREFIX);

            if (willBePartial || (similarBytes < 0 && !source.hasRemaining())) {
                if (trace) { log.trace("Read a partial header, reporting ReadStatus.FoundPartialRecord."); }
                return ReadStatus.FoundPartialRecord.encode();
            }

            if (similarBytes < 1)
                return ReadStatus.NoHeaderAtCurrentPosition.encode();

            final UUID uuid = readUUID(source);
            if (!delimiter.equals(uuid)) {
                if (trace) { log.trace("Found a record header of delimiter " + uuid + ", while expecting " + delimiter + ", skipping it."); }
                return ReadStatus.FoundHeaderWithDifferentDelimiter.encode();
            }

            int recordLength = source.getInt();

            // jump over crc32
            source.getInt(); // checksum is not needed here, we'll come back and evaluate it later.

            if (bufferContainsSequence(source, RECORD_DELIMITER_SUFFIX) <= 0)
                return ReadStatus.NoHeaderAtCurrentPosition.encode();

            if (recordLength + RECORD_TRAILER_SIZE > source.remaining()) {
                if (trace) { log.trace("Found partial record, the length " + recordLength + " exceeds the remaining bytes " + source.remaining() + "."); }
                return ReadStatus.FoundPartialRecord.encode();
            }

            // Marking the beginning of the payload.
            source.mark();

            source.position(source.position() + recordLength);
            if (bufferContainsSequence(source, RECORD_DELIMITER_TRAILER) <= 0 || !delimiter.equals(readUUID(source))) {
                if (log.isDebugEnabled()) {
                    log.debug("Found an invalid record trailer for delimiter " + delimiter + ". Will skip the entry " + bufferToString(source) + ".");
                }
                return ReadStatus.NoHeaderAtCurrentPosition.encode();
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
                        case ReadOk:
                            return recordLength;
                        case FoundPartialRecord:
                            return ReadStatus.FoundPartialRecord.encode();
                        case FoundHeaderWithDifferentDelimiter:
                            if (source.remaining() > RECORD_HEADER_SIZE) {
                                if (trace) { log.trace("Found other header entry, try to use its length as hint to iterate it quickly."); }

                                int entryLength = source.getInt(source.position() + RECORD_LENGTH_OFFSET);
                                source.position(source.position() + RECORD_HEADER_SIZE);

                                if (entryLength + RECORD_TRAILER_SIZE <= source.remaining() &&
                                        source.get(source.position() + entryLength) == RECORD_DELIMITER_TRAILER[0]) {
                                    if (trace) { log.trace("Quickly iterating other log entry."); }
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

        return ReadStatus.NoHeaderInBuffer.encode();
    }

    /**
     * Verifies whether the given sequence is contained inside the buffer and advances the buffer's position by the matched characters.
     *
     * @param source   the buffer to search in.
     * @param sequence the sequence of bytes to compare.
     * @return a positive number of matched bytes if the whole sequence was matched. A negative number of matched bytes if only a subset matched.
     */
    private static int bufferContainsSequence(final ByteBuffer source, final byte[] sequence) {
        final int maxCount = source.remaining();
        int count = 0;

        for (byte b : sequence) {
            if (maxCount == count || source.get() != b) {
                if (maxCount != count)
                    source.position(source.position() - 1); // revert the last consumed byte.
                return -count;
            }
            count++;
        }

        return count;
    }

    /**
     * Enumerates the possible states when reading a record starting from an arbitrary position inside the file.
     */
    static enum ReadStatus {
        /**
         * Header was successfully read, the record is fully contained in the buffer and it is valid.
         */
        ReadOk,
        /**
         * There's no header at the current buffer position.
         */
        NoHeaderAtCurrentPosition,
        /**
         * There's no header in the whole buffer.
         */
        NoHeaderInBuffer,
        /**
         * There's a header but it doesn't belong to the current delimiter.
         */
        FoundHeaderWithDifferentDelimiter,
        /**
         * There's a valid header but the record is not complete.
         */
        FoundPartialRecord,;

        static ReadStatus decode(int recordLength) {
            if (recordLength >= 0)
                return ReadOk;
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
