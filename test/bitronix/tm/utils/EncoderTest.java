package bitronix.tm.utils;

import junit.framework.TestCase;

public class EncoderTest extends TestCase {

    public void testLongEncodingDecoding() {
        byte[] longAsBytes;
        long result;

        longAsBytes = Encoder.longToBytes(Long.MAX_VALUE);
        result = Encoder.bytesToLong(longAsBytes);
        assertEquals(Long.MAX_VALUE, result);

        longAsBytes = Encoder.longToBytes(Long.MIN_VALUE);
        result = Encoder.bytesToLong(longAsBytes);
        assertEquals(Long.MIN_VALUE, result);

        longAsBytes = Encoder.longToBytes(-1L);
        result = Encoder.bytesToLong(longAsBytes);
        assertEquals(-1L, result);


        try {
            Encoder.bytesToLong(new byte[4]);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("a long can only be decoded from a 8 bytes array (got a 4 byte(s) array)", ex.getMessage());
        }
    }

}
