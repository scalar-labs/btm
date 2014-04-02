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

import java.util.Arrays;

/**
 * <p>A constant UID byte array container optimized for use with hashed collections.</p>
 *
 * @author Ludovic Orban
 */
public final class Uid {

    private final byte[] array;
    private final int hashCodeValue;
    private final String toStringValue;

    public Uid(byte[] array) {
        this.array = new byte[array.length];
        System.arraycopy(array, 0, this.array, 0, array.length);
        this.hashCodeValue = arrayHashCode(array);
        this.toStringValue = arrayToString(array);
    }

    public byte[] getArray() {
        return array;
    }

    public byte[] extractServerId() {
        int serverIdLength = array.length - 4 - 8; // - sequence - timestamp
        if (serverIdLength < 1)
            return null;

        byte[] result = new byte[serverIdLength];
        System.arraycopy(array, 0, result, 0, serverIdLength);
        return result;
    }

    public long extractTimestamp() {
        return Encoder.bytesToLong(array, array.length - 4 - 8); // - sequence - timestamp
    }

    public int extractSequence() {
        return Encoder.bytesToInt(array, array.length - 4); // - sequence
    }

    public int length() {
    	return array.length;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Uid) {
            Uid otherUid = (Uid) obj;

            // optimizes performance a bit
            if (hashCodeValue != otherUid.hashCodeValue)
                return false;

            return Arrays.equals(array, otherUid.array);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashCodeValue;
    }

    @Override
    public String toString() {
        return toStringValue;
    }

    /**
     * Compute a UID byte array hashcode value.
     * @param uid the byte array used for hashcode computation.
     * @return a constant hash value for the specified uid.
     */
    private static int arrayHashCode(byte[] uid) {
        int hash = 0;
        // Common fast but good hash with wide dispersion
        for (int i = uid.length - 1; i > 0; i--) {
            // rotate left and xor
            // (very fast in assembler, a bit clumsy in Java)
            hash <<= 1;

            if (hash < 0) {
                hash |= 1;
            }

            hash ^= uid[i];
        }
        return hash;
    }

    /**
     * Decode a UID byte array into a (somewhat) human-readable hex string.
     * @param uid the uid to decode.
     * @return the resulting printable string.
     */
    private static String arrayToString(byte[] uid) {
        char[] hexChars = new char[uid.length * 2];
        int c = 0;
        int v;
        for (int i = 0; i < uid.length; i++) {
            v = uid[i] & 0xFF;
            hexChars[c++] = HEX[v >> 4];
            hexChars[c++] = HEX[v & 0xF];
        }
        return new String(hexChars);
    }

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
}

