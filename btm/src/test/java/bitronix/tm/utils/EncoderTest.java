/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
