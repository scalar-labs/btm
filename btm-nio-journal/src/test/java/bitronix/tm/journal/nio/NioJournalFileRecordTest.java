package bitronix.tm.journal.nio;

import org.junit.Before;
import org.junit.Test;

import javax.transaction.Status;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

import static bitronix.tm.journal.nio.NioJournalConstants.NAME_CHARSET;
import static bitronix.tm.journal.nio.NioJournalFileRecord.*;
import static bitronix.tm.journal.nio.NioJournalFileRecord.ReadStatus.*;
import static bitronix.tm.utils.UidGenerator.generateUid;
import static java.lang.System.arraycopy;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Implements functionality tests on the class NioJournalFileRecord.
 *
 * @author juergen kellerer, 2011-08-28
 */
public class NioJournalFileRecordTest {

    static final UUID expectedDelimiter = UUID.randomUUID(), otherDelimiter = UUID.randomUUID();
    static Random random = new Random();

    NioJournalRecord payload = new NioJournalRecord(Status.STATUS_ACTIVE, generateUid(), new HashSet<String>(asList("a name")));
    byte[] payloadBytes;

    NioJournalFileRecord fileRecord;

    @Before
    public void setUp() throws Exception {
        payloadBytes = new byte[payload.getRecordLength()];
        payload.encodeTo(wrap(payloadBytes), false);
        fileRecord = new NioJournalFileRecord(expectedDelimiter);
    }

    private byte[] recordToBytes(NioJournalFileRecord fileRecord) {
        byte[] buffer = new byte[fileRecord.getRecordSize()];
        ByteBuffer wrap = wrap(buffer);
        fileRecord.writeRecord(expectedDelimiter, wrap);
        assertFalse("getRecordSize() returned an invalid size", wrap.hasRemaining());
        return buffer;
    }

    private byte[] recordToBytes() throws Exception {
        testCanCreateEmptyPayloadAndWriteToIt();
        return recordToBytes(fileRecord);
    }

    private void createNoise(byte[] chunk) {
        random.nextBytes(chunk);

        // Remove values that could randomly lead to the detection of partial records.
        for (int c = chunk.length - 1; c >= 0; c--)
            if (chunk[c] == '[')
                chunk[c] = ' ';
        Arrays.fill(chunk, chunk.length - 4, chunk.length, (byte) ' ');
    }

    @Test
    public void testCanCreateEmptyPayloadAndWriteToIt() throws Exception {
        fileRecord.createEmptyPayload(payloadBytes.length).put(payloadBytes);
        assertEquals(wrap(payloadBytes), fileRecord.getPayload());
    }

    @Test
    public void testCanWriteAndReadRecord() throws Exception {
        fileRecord = NioJournalFileRecord.readRecord(expectedDelimiter, wrap(recordToBytes()));

        assertTrue("is not valid", fileRecord.isValid());
        assertEquals(expectedDelimiter, fileRecord.getDelimiter());
        assertEquals(wrap(payloadBytes), fileRecord.getPayload());
    }

    @Test
    public void testCanReadInvalidEntry() throws Exception {
        byte[] recordBytes = recordToBytes();
        int namePosition = new String(recordBytes, NAME_CHARSET.name()).indexOf("a name");

        ByteBuffer buffer = wrap(recordBytes);
        buffer.put(namePosition, (byte) '#');
        fileRecord = NioJournalFileRecord.readRecord(expectedDelimiter, buffer);

        assertFalse("is valid", fileRecord.isValid());
        assertEquals("# name", new NioJournalRecord(fileRecord.getPayload(), false).getUniqueNames().iterator().next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotExceedRecordMaxSize() throws Exception {
        fileRecord.createEmptyPayload(JOURNAL_MAX_RECORD_SIZE + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotCreateNegativeSizedPayload() throws Exception {
        fileRecord.createEmptyPayload(-1);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotWriteEmptyRecord() throws Exception {
        fileRecord.writeRecord(expectedDelimiter, allocate(fileRecord.getRecordSize()));
    }

    @Test
    public void testCannotWriteDisposedRecord() throws Exception {
        testCanCreateEmptyPayloadAndWriteToIt();
        fileRecord.writeRecord(expectedDelimiter, allocate(fileRecord.getRecordSize()));
        fileRecord.dispose();
        try {
            fileRecord.writeRecord(expectedDelimiter, allocate(fileRecord.getRecordSize()));
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testSkipsOtherValidRecords() throws Exception {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Test
    public void testDoesNotSkipOtherInvalidRecords() throws Exception {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Test
    public void testFindsRecordAtArbitraryPositionInBuffer() throws Exception {
        final byte[] chunk = new byte[8 * 1024];
        final byte[] recordBytes = recordToBytes();
        final ByteBuffer byteBuffer = wrap(chunk);

        final int rightMargin = chunk.length - recordBytes.length;

        for (int i = 0; i <= rightMargin; i++) {
            createNoise(chunk);

            arraycopy(recordBytes, 0, chunk, i, recordBytes.length);
            byteBuffer.clear();

            NioJournalFileRecord.FindResult result = findNextRecord(expectedDelimiter, byteBuffer);

            assertEquals(ReadOk, result.getStatus());
            assertEquals(recordBytes.length, result.getRecord().getRecordSize());
            assertEquals("didn't find correct record position.", i, byteBuffer.position() - recordBytes.length);
            assertEquals(chunk.length - i - recordBytes.length, byteBuffer.remaining());
        }
    }

    @Test
    public void testFindsForwardLookingPartialRecords() throws Exception {
        final byte[] chunk = new byte[8 * 1024];
        final byte[] recordBytes = recordToBytes();
        final ByteBuffer byteBuffer = wrap(chunk);

        for (int i = (chunk.length - recordBytes.length) + 1; i < chunk.length; i++) {
            createNoise(chunk);

            arraycopy(recordBytes, 0, chunk, i, chunk.length - i);
            byteBuffer.clear();

            NioJournalFileRecord.FindResult result = findNextRecord(expectedDelimiter, byteBuffer);

            assertEquals(FoundPartialRecord, result.getStatus());
            assertEquals("didn't find correct partial record position.", i, byteBuffer.position());
        }
    }

    @Test
    public void testFindsBrokenRecordsIfHeaderAndTrailerAreIntact() throws Exception {
        final byte[] chunk = new byte[4 * 1024];
        final byte[] recordBytes = recordToBytes();
        final ByteBuffer byteBuffer = wrap(chunk);

        byte corruptionByte = '#';
        String recordString = new String(recordBytes, NAME_CHARSET.name());
        while (recordString.indexOf((char) corruptionByte) != -1)
            corruptionByte = (byte) random.nextInt(256);

        for (int repetitions = 10; repetitions > 0; repetitions--)
            for (int i = 1000; i < 2000; i++) {
                createNoise(chunk);
                //Arrays.fill(chunk, (byte) 0x0);

                arraycopy(recordBytes, 0, chunk, i, recordBytes.length);
                byteBuffer.clear();

                int corruption = 1500;
                byteBuffer.put(corruption, corruptionByte);
                FindResult findResult = findNextRecord(expectedDelimiter, byteBuffer);

                boolean recordIsOutsideOfCorruption = i > corruption || i + recordBytes.length <= corruption;
                if (recordIsOutsideOfCorruption) {
                    assertTrue("didn't find valid record at pos " + i + " (" + findResult + ")",
                            findResult.getRecord() != null && findResult.getRecord().isValid());
                } else {
                    boolean corruptionInsidePayload = corruption >= i + RECORD_HEADER_SIZE && corruption < i + recordBytes.length - RECORD_TRAILER_SIZE;
                    boolean corruptionInsideCRC32 = corruption >= i + RECORD_CRC32_OFFSET && corruption < i + RECORD_CRC32_OFFSET + 4;
                    boolean corruptionInsideLengthField = corruption >= i + RECORD_CRC32_OFFSET - 4 && corruption < i + RECORD_CRC32_OFFSET;

                    if (corruptionInsidePayload || corruptionInsideCRC32) {
                        assertFalse("didn't find corrupted record at pos " + i + " (" + findResult + ")",
                                findResult.getRecord() == null || findResult.getRecord().isValid());
                    } else if (corruptionInsideLengthField) {
                        assertTrue("pos " + i + " (" + findResult + ")", asList(NoHeaderInBuffer, FoundPartialRecord).contains(findResult.getStatus()));
                    } else {
                        assertEquals("pos " + i + " (" + findResult + ")", NoHeaderInBuffer, findResult.getStatus());
                    }
                }
            }
    }
}
