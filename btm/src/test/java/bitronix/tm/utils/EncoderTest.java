/*
 * Bitronix Transaction Manager
 *
 * Copyright (c) 2010, Bitronix Software.
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
package bitronix.tm.utils;

import junit.framework.TestCase;

/**
 *
 * @author lorban
 */
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

        byte[] intAsBytes = Encoder.intToBytes(-1);
        int resultAsInt = Encoder.bytesToInt(intAsBytes, 0);
        assertEquals(-1, resultAsInt);


        try {
            Encoder.bytesToLong(new byte[4], 0);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("a long can only be decoded from 8 bytes of an array (got a 4 byte(s) array, must start at position 0)", ex.getMessage());
        }
    }

}
