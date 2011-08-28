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

import bitronix.tm.journal.JournalRecord;
import bitronix.tm.utils.Uid;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableSet;

/**
 * Implements {@code TransactionLogRecord} for the NioJournal.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioJournalRecord implements JournalRecord, NioJournalConstants {

    // Starts from 0 for every new runtime session.
    private static final AtomicLong JOURNAL_RECORD_SEQUENCE = new AtomicLong();

    /**
     * Provides thread safe access to name encoders.
     * (Similar to the way it is done inside the JRE as constructing the encoder is relatively expensive)
     */
    public static final ThreadLocal<CharsetEncoder> NAME_ENCODERS = new ThreadLocal<CharsetEncoder>() {
        @Override
        protected CharsetEncoder initialValue() {
            return NAME_CHARSET.newEncoder();
        }
    };

    private static final byte[][] txStatusStrings;

    static {
        txStatusStrings = new byte[TRANSACTION_STATUS_STRINGS.size()][];
        for (int i = 0; i < txStatusStrings.length; i++) {
            String statusString = TRANSACTION_STATUS_STRINGS.get(i);
            txStatusStrings[i] = statusString.getBytes(NAME_CHARSET);

            if (txStatusStrings[i].length != 6)
                throw new IllegalStateException("The TX status string '" + statusString + "' encodes to a length != 6. Please fix this.");
        }
    }

    public static final int STATIC_RECORD_LENGTH =
            /* status-string        */ txStatusStrings[0].length +
            /* status               */ 4 +
            /* recordLength         */ 4 +
            /* time                 */ 8 +
            /* sequenceNumber       */ 8 +
            /* rolled over flag     */ 1 +
            /* gtrid (static)       */ 1 +
            /* uniqueNames (static) */ 2;

    private static byte[] statusToBytes(int status) {
        if (status > 0 && status < txStatusStrings.length)
            return txStatusStrings[status];
        return txStatusStrings[txStatusStrings.length - 1];
    }

    private static ByteBuffer skipStatusString(ByteBuffer buffer) {
        return (ByteBuffer) buffer.position(buffer.position() + txStatusStrings[0].length);
    }

    private static int calculateRecordLength(Uid gtrid, Set<String> names) {
        int length = STATIC_RECORD_LENGTH + gtrid.getArray().length;
        for (String name : names)
            length += 2 + name.length();
        return length;
    }

    private static Set<String> namesFromBuffer(ByteBuffer buffer) {
        final int count = buffer.getShort();
        final Set<String> names = new HashSet<String>(count);

        for (int i = 0, len; i < count; i++) {
            // Note: Decoding may be implemented without max. speed optimization as it is only used when
            //       reading the journal file (happens only once)
            len = buffer.getShort();
            String name = NAME_CHARSET.decode((ByteBuffer) buffer.slice().limit(len)).toString();

            // Note: Unique names should be only a couple, but many log records may be created when reading a file.
            //       internalizing strings can significantly reduce the memory footprint.
            names.add(name.intern());

            buffer.position(buffer.position() + len);
        }

        return names;
    }

    private static void namesToBuffer(Set<String> uniqueNames, ByteBuffer buffer) {
        final CharsetEncoder charsetEncoder = NAME_ENCODERS.get();

        assertIsInRange(uniqueNames, uniqueNames.size(), Short.MAX_VALUE);
        buffer.putShort((short) uniqueNames.size());

        for (Object un : uniqueNames) {
            String name = (String) un;
            final int length = name.length();

            assertIsInRange(un, length, Short.MAX_VALUE);
            buffer.putShort((short) length);

            charsetEncoder.encode(CharBuffer.wrap(name), buffer, true);
        }
    }

    private static Uid uidFromBuffer(ByteBuffer buffer) {
        byte[] arr = new byte[buffer.get() & 0xff];
        buffer.get(arr);
        return new Uid(arr);
    }

    private static void uidToBuffer(Uid uid, ByteBuffer buffer) {
        byte[] array = uid.getArray();

        assertIsInRange(uid, array.length, Byte.MAX_VALUE);
        buffer.put((byte) array.length);
        buffer.put(array);
    }

    private static void assertIsInRange(Object element, int length, int limit) {
        if (length > limit) {
            throw new IllegalArgumentException("Cannot encode " + element +
                    " as its size exceeds the limit of " + limit);
        }
    }


    private final int status;
    private final Uid gtrid;
    private final Set<String> uniqueNames;
    private final long time, sequenceNumber;
    private final int recordLength;
    private final boolean valid, rolledOverFlag;

    NioJournalRecord(int status, int recordLength, long time, long sequenceNumber, boolean rolledOverFlag,
                     Uid gtrid, Set<String> uniqueNames, boolean valid) {
        // Note: This constructor should not be used outside of unit tests or this class.
        this.status = status;
        this.gtrid = gtrid;
        this.uniqueNames = uniqueNames;
        this.time = time;
        this.sequenceNumber = sequenceNumber;
        this.recordLength = recordLength;
        this.valid = valid;
        this.rolledOverFlag = rolledOverFlag;
    }

    /**
     * Constructs a new record of the given values.
     *
     * @param status      the TX status,  see {@link javax.transaction.Status}.
     * @param gtrid       the global transaction id.
     * @param uniqueNames the unique names identifying the resources participating in the transaction.
     */
    public NioJournalRecord(int status, Uid gtrid, Set<String> uniqueNames) {
        this(status, calculateRecordLength(gtrid, uniqueNames), System.currentTimeMillis(),
                JOURNAL_RECORD_SEQUENCE.incrementAndGet(), false, gtrid, unmodifiableSet(new HashSet<String>(uniqueNames)), true);
    }

    /**
     * Constructs a new record by de-serializing the state from the given byte buffer.
     * <p/>
     * Note: When valid is set to false, the buffer must still contain decodeable data.
     * Data that fails decoding will cause runtime exceptions.
     *
     * @param buffer the buffer containing the serialized record state.
     * @param valid  specifies whether the record should be marked valid.
     */
    public NioJournalRecord(ByteBuffer buffer, boolean valid) {
        this(
                skipStatusString(buffer).getInt(), // status
                buffer.getInt(), // recordLength
                buffer.getLong(), // time
                buffer.getLong(), // sequenceNumber
                buffer.get() == 1, // rolledOver flag
                uidFromBuffer(buffer), // gtrid
                unmodifiableSet(namesFromBuffer(buffer)), // uniqueNames
                valid
        );
    }

    /**
     * Encodes this instance to the given buffer.
     *
     * @param buffer the buffer to use for writing.
     */
    protected void encodeTo(ByteBuffer buffer, boolean rolledOver) {
        buffer.put(statusToBytes(getStatus()));
        buffer.putInt(getStatus());
        buffer.putInt(getRecordLength());
        buffer.putLong(getTime());
        buffer.putLong(getSequenceNumber());
        buffer.put((byte) (rolledOver ? 1 : 0));
        uidToBuffer(getGtrid(), buffer);
        namesToBuffer(getUniqueNames(), buffer);
    }

    /**
     * Returns a copy of this record with unique names being reduced by the given record.
     *
     * @param comittedOrRolledbackRecord a record in committed or rolledback state used to reduce the unique names of this record.
     * @return a copy of this record with a reduced set of names. If no reduction happened 'this' is returned.
     */
    protected NioJournalRecord createNameReducedCopy(NioJournalRecord comittedOrRolledbackRecord) {
        if (!FINAL_STATUS.contains(comittedOrRolledbackRecord.status)) {
            throw new IllegalArgumentException("The given record " + comittedOrRolledbackRecord + " is not in a state that allows " +
                    "its usage as resource name reduction record.");
        }

        final HashSet<String> reducedNames = new HashSet<String>(uniqueNames);
        if (reducedNames.removeAll(comittedOrRolledbackRecord.getUniqueNames()))
            return new NioJournalRecord(status, recordLength, time, sequenceNumber, rolledOverFlag, gtrid, unmodifiableSet(reducedNames), valid);
        else
            return this;
    }

    /**
     * Returns true if the given buffer has enough capacity to take this record.
     *
     * @param buffer the buffer to test.
     * @return true if the capacity is sufficient for encoding.
     */
    public boolean hasEnoughCapacity(ByteBuffer buffer) {
        return buffer != null && buffer.capacity() >= getRecordLength();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    public Uid getGtrid() {
        return gtrid;
    }


    /**
     * {@inheritDoc}
     */
    public Set<String> getUniqueNames() {
        return uniqueNames;
    }

    /**
     * Returns the number of unique names.
     *
     * @return the number of unique names.
     */
    protected int getUniqueNamesCount() {
        return uniqueNames.size();
    }

    /**
     * Returns true if the unique names of this record are all contained in 'other'.
     *
     * @param other the other record to check.
     * @return true if the unique names of this record are all contained in 'other'.
     */
    protected boolean isUniqueNamesContainedInRecord(NioJournalRecord other) {
        return uniqueNames.isEmpty() || other.uniqueNames.containsAll(uniqueNames);
    }

    /**
     * {@inheritDoc}
     */
    public long getTime() {
        return time;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public boolean isRolledOverFlag() {
        return rolledOverFlag;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ?> getRecordProperties() {
        Map<String, Object> props = new LinkedHashMap<String, Object>(4);
        props.put("recordLength", recordLength);
        props.put("sequenceNumber", sequenceNumber);
        return props;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NioJournalRecord{" +
                "status=" + TRANSACTION_LONG_STATUS_STRINGS.get(Math.min(TRANSACTION_LONG_STATUS_STRINGS.size() - 1, status)) +
                ", gtrid=" + gtrid +
                ", uniqueNames=" + uniqueNames +
                ", time=" + new Date(time) +
                ", sequenceNumber=" + sequenceNumber +
                ", recordLength=" + recordLength +
                ", rolledOverFlag=" + rolledOverFlag +
                ", valid=" + valid +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NioJournalRecord)) return false;

        NioJournalRecord that = (NioJournalRecord) o;

        if (recordLength != that.getRecordLength()) return false;
        if (sequenceNumber != that.getSequenceNumber()) return false;
        if (status != that.getStatus()) return false;
        if (time != that.getTime()) return false;
        if (valid != that.isValid()) return false;
        if (gtrid != null ? !gtrid.equals(that.getGtrid()) : that.getGtrid() != null) return false;
        if (uniqueNames != null ? !uniqueNames.equals(that.getUniqueNames()) : that.getUniqueNames() != null)
            return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = status;
        result = 31 * result + (gtrid != null ? gtrid.hashCode() : 0);
        result = 31 * result + (uniqueNames != null ? uniqueNames.hashCode() : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
        result = 31 * result + recordLength;
        result = 31 * result + (valid ? 1 : 0);
        return result;
    }
}
