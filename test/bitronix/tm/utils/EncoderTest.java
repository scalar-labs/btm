package bitronix.tm.utils;

import junit.framework.TestCase;

public class EncoderTest extends TestCase {

    public void testLongEncodingDecoding() {
        byte[] longAsBytes;
        long result;

        longAsBytes = Encoder.longToBytes(Long.MAX_VALUE);
        result = Encoder.bytesToLong(longAsBytes, 0);
        assertEquals(Long.MAX_VALUE, result);

        longAsBytes = Encoder.longToBytes(Long.MIN_VALUE);
        result = Encoder.bytesToLong(longAsBytes, 0);
        assertEquals(Long.MIN_VALUE, result);

        longAsBytes = Encoder.longToBytes(-1L);
        result = Encoder.bytesToLong(longAsBytes, 0);
        assertEquals(-1L, result);

        long timestamp = System.currentTimeMillis();
        longAsBytes = Encoder.longToBytes(timestamp);
        result = Encoder.bytesToLong(longAsBytes, 0);
        assertEquals(timestamp, result);


        try {
            Encoder.bytesToLong(new byte[4], 0);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("a long can only be decoded from 8 bytes of an array (got a 4 byte(s) array, must start at position 0)", ex.getMessage());
        }
    }

}
