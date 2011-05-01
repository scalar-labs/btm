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

import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.utils.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Implements {@code TransactionLogRecord} for the NioJournal.
 *
 * @author juergen kellerer, 2011-04-30
 */
class NioLogRecord extends TransactionLogRecord {

    private static final Logger log = LoggerFactory.getLogger(NioLogRecord.class);
    private static final int CRC32_POSITION = 4 + 4 + 4 + 8 + 4;

    // must be a charset of constant size, using 8 bit encoding
    public static final Charset NAME_CHARSET = Charset.forName("ISO-8859-1");
    public static final ThreadLocal<CharsetEncoder> NAME_ENCODERS = new ThreadLocal<CharsetEncoder>() {
        @Override
        protected CharsetEncoder initialValue() {
            return NAME_CHARSET.newEncoder();
        }
    };

    static Set namesFromBuffer(ByteBuffer buffer) {
        final int count = buffer.getInt();
        final Set<String> names = new HashSet<String>(count);

        for (int i = 0, len; i < count; i++) {
            len = buffer.getShort();
            names.add(NAME_CHARSET.decode((ByteBuffer) buffer.slice().limit(len)).toString());
            buffer.position(buffer.position() + len);
        }

        return names;
    }

    static void namesToBuffer(Set uniqueNames, ByteBuffer buffer) {
        final CharsetEncoder charsetEncoder = NAME_ENCODERS.get();

        buffer.putInt(uniqueNames.size());
        for (Object un : uniqueNames) {
            String name = (String) un;
            final int length = name.length();
            if (length > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Cannot encode name " + un +
                        " as its length exceeds the limit of " + Short.MAX_VALUE);
            }
            buffer.putShort((short) length);
            charsetEncoder.encode(CharBuffer.wrap(name), buffer, true);
        }
    }

    static Uid uidFromBuffer(ByteBuffer buffer) {
        byte[] arr = new byte[buffer.get() & 0xff];
        buffer.get(arr);
        return new Uid(arr);
    }

    static void uidToBuffer(Uid uid, ByteBuffer buffer) {
        byte[] array = uid.getArray();
        if (array.length > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Cannot encode GTRID " + uid +
                    " as its length exceeds the limit of " + Byte.MAX_VALUE);
        }

        buffer.put((byte) array.length);
        buffer.put(array);
    }

    static int readStatus(ByteBuffer buffer) {
        return buffer.getInt(buffer.position());
    }

    private boolean eagerCrc32Calculation;

    public NioLogRecord(int status, Uid gtrid, Set uniqueNames) {
        super(status, gtrid, uniqueNames);
    }

    public NioLogRecord(ByteBuffer buffer) {
        super(buffer.getInt(), // status
                buffer.getInt(), // recordLength
                buffer.getInt(), // headerLength
                buffer.getLong(), // time
                buffer.getInt(), // sequenceNumber
                buffer.getInt(), // crc32
                uidFromBuffer(buffer), // gtrid
                namesFromBuffer(buffer), // uniqueNames
                buffer.getInt()); // endRecord
        eagerCrc32Calculation = true;
    }

    // used to test only.
    NioLogRecord(int status, int recordLength, int headerLength, long time,
                 int sequenceNumber, int crc32, Uid gtrid, Set uniqueNames, int endRecord) {
        super(status, recordLength, headerLength, time, sequenceNumber, crc32, gtrid, uniqueNames, endRecord);
        eagerCrc32Calculation = true;
    }

    /**
     * Encodes this instance to the given or a newly allocated buffer and returns the flipped buffer instance.
     * <p/>
     * Note: If the given buffer can be used for writing, all contents will be replaced.
     *
     * @param buffer the buffer to attempt to use for writing.
     * @return the flipped buffer containing the content.
     */
    protected void encodeTo(ByteBuffer buffer) {
        buffer.putInt(getStatus());
        buffer.putInt(getRecordLength());
        buffer.putInt(getHeaderLength());
        buffer.putLong(getTime());
        buffer.putInt((int) getSequenceNumber());

        if (buffer.position() != CRC32_POSITION) {
            throw new IllegalStateException("There a bug in the implementation " +
                    "CRC32 position is at an unexpected index.");
        }
        buffer.putInt(0); // CRC32_POSITION

        uidToBuffer(getGtrid(), buffer);
        namesToBuffer(getUniqueNames(), buffer);

        buffer.putInt(getEndRecord()).flip();

        // Calculate CRC32
        final CRC32 crc = new CRC32();
        crc.update(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        buffer.putInt(CRC32_POSITION, (int) crc.getValue());
    }

    public int getEffectiveRecordLength() {
        return 4 + 4 + getRecordLength(); // getRecordLength() misses: status + record length
    }

    public boolean hasEnoughCapacity(ByteBuffer buffer) {
        return buffer != null && buffer.capacity() >= getEffectiveRecordLength();
    }


    @Override
    public int calculateCrc32() {
        if (!eagerCrc32Calculation)
            return 0;

        final NioBufferPool bufferPool = NioBufferPool.getInstance();
        final ByteBuffer sharedBuffer = bufferPool.poll(getEffectiveRecordLength());
        try {
            encodeTo(sharedBuffer);
            return sharedBuffer.getInt(CRC32_POSITION);
        } finally {
            bufferPool.recycleBuffer(sharedBuffer);
        }
    }

    @Override
    public boolean isCrc32Correct() {
        eagerCrc32Calculation = true;
        return super.isCrc32Correct();
    }

    @Override
    public int getCrc32() {
        if (!eagerCrc32Calculation) {
            eagerCrc32Calculation = true;
            try {
                Method refresh = getClass().getSuperclass().getDeclaredMethod("refresh");
                refresh.setAccessible(true);
                refresh.invoke(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.getCrc32();
    }
}
