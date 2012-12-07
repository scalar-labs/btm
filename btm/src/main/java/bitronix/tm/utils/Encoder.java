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

/**
 * Number to byte array and byte array to number encoder.
 *
 * @author lorban
 */
public class Encoder {

    public static byte[] longToBytes(long aLong) {
        byte[] array = new byte[8];

        array[7] = (byte) (aLong & 0xff);
        array[6] = (byte) ((aLong >> 8) & 0xff);
        array[5] = (byte) ((aLong >> 16) & 0xff);
        array[4] = (byte) ((aLong >> 24) & 0xff);
        array[3] = (byte) ((aLong >> 32) & 0xff);
        array[2] = (byte) ((aLong >> 40) & 0xff);
        array[1] = (byte) ((aLong >> 48) & 0xff);
        array[0] = (byte) ((aLong >> 56) & 0xff);

        return array;
    }

    public static byte[] intToBytes(int anInt) {
        byte[] array = new byte[4];

        array[3] = (byte) (anInt & 0xff);
        array[2] = (byte) ((anInt >> 8) & 0xff);
        array[1] = (byte) ((anInt >> 16) & 0xff);
        array[0] = (byte) ((anInt >> 24) & 0xff);

        return array;
    }

    public static byte[] shortToBytes(short aShort) {
        byte[] array = new byte[2];

        array[1] = (byte) (aShort & 0xff);
        array[0] = (byte) ((aShort >> 8) & 0xff);

        return array;
    }

    public static long bytesToLong(byte[] bytes, int pos) {
        if (bytes.length + pos < 8)
            throw new IllegalArgumentException("a long can only be decoded from 8 bytes of an array (got a " + bytes.length + " byte(s) array, must start at position " + pos + ")");

        long result = 0;

        for(int i=0; i < 8 ;i++) {
           result <<= 8;
           result ^= (long) bytes[i + pos] & 0xFF;
        }

        return result;
    }

    public static int bytesToInt(byte[] bytes, int pos) {
        if (bytes.length + pos < 4)
            throw new IllegalArgumentException("an integer can only be decoded from 4 bytes of an array (got a " + bytes.length + " byte(s) array, must start at position " + pos + ")");

        int result = 0;

        for(int i=0; i < 4 ;i++) {
           result <<= 8;
           result ^= (int) bytes[i + pos] & 0xFF;
        }

        return result;
    }
}
